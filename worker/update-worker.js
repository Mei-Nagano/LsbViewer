// LinuxSB 检查更新代理（Cloudflare Worker）
//
// 部署（推荐，零配置）：
//   1. Cloudflare Dashboard → Workers & Pages → Create → Create Worker
//   2. 名字随意（如 lsb-update）→ Deploy 后进入 Edit code
//   3. 用本文件内容整体替换默认代码 → Deploy
//   4. 部署后把 Settings → Domains & Routes 里的 https://lsb-update.xxx.workers.dev
//      地址填入 App 的 UpdateChecker.WORKER_URL
//
// 作用：代理 GitHub Releases API，规避官方 API 60 次/小时/IP 的速率限制
// 与国内无法直连 api.github.com 的问题。带 10 分钟共享缓存（Cache API），
// 所有 App 用户命中同一份缓存，上游实际调用量极小。

const REPO = "Mei-Nagano/LsbViewer";
const UPSTREAM = `https://api.github.com/repos/${REPO}/releases/latest`;
const CACHE_SECONDS = 600; // 10 分钟

// 部署后必配（Settings → Variables and Secrets → Add，Type 选 Secret）：
//   GH_TOKEN = GitHub PAT（Fine-grained，Public repositories 只读即可，无需勾选任何权限）
// 不配会因 Cloudflare 共享出口 IP 被 GitHub 403（匿名限额 60 次/小时/IP 被全球用户共用）
// 配置后限额 5000 次/小时/token，配合缓存绰绰有余。
// 该 Token 只存在 Worker Secret 里，不会进入 App 或仓库。

const RESP_HEADERS = {
  "Content-Type": "application/json; charset=utf-8",
  "Access-Control-Allow-Origin": "*",
  "Cache-Control": `public, max-age=${CACHE_SECONDS}, s-maxage=${CACHE_SECONDS}`,
};

export default {
  async fetch(request, env, ctx) {
    // 命中缓存直接返回（所有用户共享，这是规避速率限制的关键）
    const cached = await caches.default.match(request);
    if (cached) return cached;

    let status = 502;
    let data = { error: "upstream_unavailable" };
    try {
      const headers = {
        "User-Agent": "LsbViewer-update-worker",
        "Accept": "application/vnd.github+json",
      };
      if (env.GH_TOKEN) headers["Authorization"] = `Bearer ${env.GH_TOKEN}`;
      const resp = await fetch(UPSTREAM, { headers });
      status = resp.status;
      if (resp.ok) {
        // 只透传 App 需要的字段，响应体更小
        const o = await resp.json();
        data = {
          tag_name: o.tag_name || "",
          name: o.name || "",
          html_url: o.html_url || "",
          body: o.body || "",
          published_at: o.published_at || "",
          assets: (o.assets || []).map((a) => ({
            name: a.name,
            size: a.size,
            browser_download_url: a.browser_download_url,
          })),
        };
      } else {
        // 404 = 仓库还没有 Release；透传状态码，App 端按"无更新"处理
        data = { error: `upstream_${resp.status}` };
      }
    } catch (e) {
      status = 502;
      data = { error: "upstream_fetch_failed" };
    }

    const out = new Response(JSON.stringify(data), { status, headers: RESP_HEADERS });
    // 只缓存成功结果，错误（如暂无 Release）不缓存，避免错误被钉住 10 分钟
    if (status === 200) ctx.waitUntil(caches.default.put(request, out.clone()));
    return out;
  },
};
