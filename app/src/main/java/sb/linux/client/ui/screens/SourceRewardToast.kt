package sb.linux.client.ui.screens

/** 源站脚本已核对用户和奖励后，将其弹窗改为应用提示。 */
internal const val SOURCE_REWARD_TOAST_PREFIX = "lsb-post-reward:"
internal val sourceRewardToastScript = """
    (()=>{
      if(window.__lsbRewardToastInstalled)return;
      window.__lsbRewardToastInstalled=true;
      function consume(){
        const popup=document.querySelector('.post-random-rewards-popup');
        if(!popup||popup.hidden)return;
        const message=popup.querySelector('[data-post-random-rewards-message]')?.textContent?.trim();
        if(!message)return;
        popup.hidden=true;
        document.cookie='post_random_rewards_popup=; Max-Age=0; Path=/; SameSite=Lax';
        console.info('lsb-post-reward:'+JSON.stringify(message));
      }
      new MutationObserver(consume).observe(document.documentElement,
        {subtree:true,childList:true,attributes:true,attributeFilter:['hidden']});
      document.addEventListener('bbs1:reply-saved',()=>queueMicrotask(consume));
      consume();
    })();
""".trimIndent()
