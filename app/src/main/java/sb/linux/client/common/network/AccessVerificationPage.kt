package sb.linux.client.common.network

/** 只有已经渲染的源站内容才能确认访问验证通过，空白页与错误页均不放行。 */
object AccessVerificationPage {
    const val STATE_SCRIPT = """
        (function(){
            if(document.readyState!=='complete' || !document.body) return 'waiting';
            if(document.querySelector('ul.post-list > li.post-item a.post-title, .post-topic-title, .post-content, .user-card, .forum-main > .main-panel, [data-home-keyword-filter-open], input[type=password], a[href="/topic/new"], a[href="/index.php?sort=comment"], a[href="/index.php?sort=post"]')) return 'ready';
            var title=document.title||'';
            var body=document.body.innerText||'';
            if(document.querySelector('#challenge-form,.ui-uam-box,.cf-browser-verification,#cf-challenge-running,.challenge-container,[class*="turnstile"],iframe[src*="challenges.cloudflare.com"]') ||
                /Checking your Browser|Just a moment|Attention Required|Verifying your request/i.test(title) ||
                /SECURITY_VERIFICATION|X-FL-UA-Step/.test(body)) return 'challenge';
            return 'waiting';
        })()
    """
}
