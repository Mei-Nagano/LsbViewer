package sb.linux.client.ui.patterns.web

/** Keep the original challenge DOM and scripts; only hide unrelated visible siblings. */
internal object VerificationRegionScript {
    const val ISOLATE = """
        (function(){
            var node=document.querySelector('#challenge-form,.ui-uam-box,.cf-browser-verification,#cf-challenge-running');
            if(!node) return;
            while(node && node.parentElement && node!==document.body){
                Array.from(node.parentElement.children).forEach(function(sibling){
                    if(sibling!==node && !/^(SCRIPT|STYLE|LINK)$/.test(sibling.tagName)){
                        sibling.style.setProperty('display','none','important');
                    }
                });
                node=node.parentElement;
            }
        })();
    """
}
