package sb.linux.client.data

import sb.linux.client.common.error.LsbException

object ChallengePagePolicy {
        const val UAM_CHALLENGE_JS =
            "(function(){try{var t=(document.title||'');var b=(document.body?document.body.innerText:'');" +
                "var el=!!(document.querySelector('#challenge-form')||document.querySelector('.ui-uam-box')||" +
                "document.querySelector('.cf-browser-verification')||document.querySelector('#cf-challenge-running')||" +
                "document.querySelector('.challenge-container')||document.querySelector('[class*=\"turnstile\"]')||" +
                "document.querySelector('iframe[src*=\"challenges.cloudflare.com\"]'));" +
                "return (t.indexOf('Checking your Browser')>=0||t.indexOf('Just a moment')>=0||" +
                "t.indexOf('Attention Required')>=0||t.indexOf('Verifying your request')>=0||" +
                "b.indexOf('SECURITY_VERIFICATION')>=0||b.indexOf('X-FL-UA-Step')>=0||el);" +
                "}catch(e){return false;}})()"

        /**
         * 挑战页体积上限：验证页是纯脚本页，通常只有几 KB；正常论坛页远大于此。
         * 人可读文案类弱特征只在小于该体积的响应里才采信。
         */
        private const val CHALLENGE_MAX_HTML = 24 * 1024

        /** 取 <title> 文本（供弱特征判定用），无标题返回空串 */
        private fun titleOf(html: String): String =
            Regex("<title[^>]*>([\\s\\S]{0,300}?)</title>", RegexOption.IGNORE_CASE)
                .find(html)?.groupValues?.get(1)?.trim().orEmpty()

        /**
         * 分级匹配挑战页特征。
         *
         * strong = 机器标识（脚本变量 / 请求头 / Cookie 名 / 固定 URL 路径），正文散文
         * 里不会出现，可以直接全文匹配。
         * weak = 人可读文案，只在 <title> 里或极小的响应里才作为判据 —— 早先对整页
         * 全文 contains("turnstile") / ("Protected By") 之类，会把「讨论 Cloudflare
         * 的帖子」或搜这些词的搜索结果页误判成挑战页，弹出无意义的验证框，最后报
         * 「访问验证恢复失败」而看不到内容。
         */
        private fun hasChallengeMarker(html: String, strong: List<String>, weak: List<String>): Boolean {
            if (strong.any { html.contains(it, ignoreCase = true) }) return true
            if (weak.isEmpty()) return false
            if (weak.any { titleOf(html).contains(it, ignoreCase = true) }) return true
            return html.length < CHALLENGE_MAX_HTML && weak.any { html.contains(it, ignoreCase = true) }
        }

        private val UAM_STRONG = listOf(
            "X-FL-UA-Step",
            "fl_ua_p",
            "SECURITY_VERIFICATION",
            "UAM_CHECKING",
        )
        private val UAM_WEAK = listOf(
            "Checking your Browser",
            "Verifying your request",
            "Protected By",
            "Peekabo",
        )

        private val CF_STRONG = listOf(
            "challenge-platform",
            "challenges.cloudflare.com",
            "cf-chl-",
            "cf-browser-verification",
            "__cf_chl",
            "cf-turnstile",
            "cdn-cgi/challenge",
            "CF-Chl-Client",
        )
        private val CF_WEAK = listOf(
            "turnstile",
            "Just a moment",
            "Attention Required",
        )

        /** 源站 Peekabo UAM 验证页特征（与页面 JS 一致） */
        internal fun isUamChallenge(html: String): Boolean =
            hasChallengeMarker(html, UAM_STRONG, UAM_WEAK)

        /** Cloudflare 人机验证 / JS 挑战特征（常以 403/503 返回） */
        private fun isCloudflareChallenge(html: String): Boolean =
            hasChallengeMarker(html, CF_STRONG, CF_WEAK)

        internal fun isChallenge(html: String): Boolean = isUamChallenge(html) || isCloudflareChallenge(html)

        /** SVG 字节嗅探：<svg 开头，或 <?xml 声明且前 1KB 内含 <svg（魔数校验用，与渲染库无关） */
        private fun isSvgBytes(b: ByteArray): Boolean {
            val head = String(b, 0, minOf(1024, b.size), Charsets.UTF_8).trimStart()
            return head.startsWith("<svg") || (head.startsWith("<?xml") && head.contains("<svg"))
        }

        /** 已知二进制图片魔数：JPEG / PNG / GIF / WebP */
        private fun isBinaryImageMagic(b: ByteArray): Boolean {
            fun u(i: Int) = b[i].toInt() and 0xFF
            return (b.size >= 3 && u(0) == 0xFF && u(1) == 0xD8 && u(2) == 0xFF) ||
                (b.size >= 4 && u(0) == 0x89 && u(1) == 0x50 && u(2) == 0x4E && u(3) == 0x47) ||
                (b.size >= 4 && u(0) == 0x47 && u(1) == 0x49 && u(2) == 0x46 && u(3) == 0x38) ||
                (b.size >= 12 && u(0) == 0x52 && u(1) == 0x49 && u(2) == 0x46 && u(3) == 0x46 &&
                    u(8) == 0x57 && u(9) == 0x45 && u(10) == 0x42 && u(11) == 0x50)
        }

        /** 有效图片字节：以魔数/内容为准，不信任 Content-Type（验证盾可能返回伪装成图片类型的 HTML） */
        fun isValidImageBytes(b: ByteArray): Boolean =
            b.isNotEmpty() && (isBinaryImageMagic(b) || (b[0] == '<'.code.toByte() && isSvgBytes(b)))

        /**
         * 空白或疑似被源站/Cloudflare 套盾的响应，也需要触发验证页展示。
         * 关键：CF 盾通常返回 403/503，不能只按 code==200 判断。
         */
        internal fun needsVerification(code: Int, html: String): Boolean =
            html.isBlank() || isChallenge(html) ||
                (code == 403 || code == 503 || code == 429) && html.isNotBlank()

        internal fun uamNonce(html: String): Int =
            Regex("""var nonce\s*=\s*(\d+)""").find(html)?.groupValues?.get(1)?.toIntOrNull()
                ?: throw LsbException("验证页缺少 nonce")

        /** 复刻源站 UAM JS：对 cookie 中字母数字字符按位置加权求和 */
        internal fun uamSum(cookie: String, nonce: Int): Long {
            var sum = 12345L
            for (i in cookie.indices) {
                val ch = cookie[i]
                if (ch.isLetterOrDigit()) sum += ch.code.toLong() * (nonce + i)
            }
            return sum
        }
}
