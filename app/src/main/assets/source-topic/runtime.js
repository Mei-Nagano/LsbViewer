/* 宿主交互脚本。源站内容不持有 Android 接口，操作仅通过拦截的导航回到原生。 */
(() => {
    const list = document.getElementById('lsb-posts');
    let options = {}, query = '', scheduled = false;
    const saved = new Map();
    const notify = () => {
        if (scheduled) return;
        scheduled = true;
        requestAnimationFrame(() => { scheduled = false; console.debug('lsb-content-layout'); });
    };
    const resize = new ResizeObserver(notify);
    resize.observe(list);
    const prep = root => {
        root.querySelectorAll('[data-nb-editor-math]').forEach(el => {
            if (!window.katex) return;
            const source = el.textContent;
            try { katex.render(source, el, {displayMode:el.dataset.nbEditorMath === 'display', throwOnError:false, trust:false, maxExpand:1000, maxSize:20}); }
            catch (_) { el.textContent = source; }
        });
        root.querySelectorAll('pre code').forEach(el => { if (window.hljs) hljs.highlightElement(el); });
        root.querySelectorAll('[data-body] img').forEach(img => {
            const wrap = document.createElement('span');
            wrap.className = 'lsb-image is-loading'; img.replaceWith(wrap); wrap.append(img);
            img.classList.add('image-lightbox-clickable');
            img.loading = 'eager';
            const done = () => {wrap.classList.remove('is-loading', 'is-error'); notify();};
            const fail = () => {wrap.classList.remove('is-loading'); wrap.classList.add('is-error'); notify();};
            img.addEventListener('load',done); img.addEventListener('error',fail);
            if (img.complete) (img.naturalWidth ? done : fail)();
        });
        root.querySelectorAll('img').forEach(img => img.addEventListener('load',notify));
        root.querySelectorAll('details').forEach(el => el.addEventListener('toggle',notify));
    };
    const highlight = () => {
        list.querySelectorAll('mark.lsb-search-hit').forEach(el => el.replaceWith(document.createTextNode(el.textContent)));
        list.normalize();
        if (!query.trim()) return;
        list.querySelectorAll('[data-body]').forEach(body => {
            const walker = document.createTreeWalker(body, NodeFilter.SHOW_TEXT);
            const nodes = []; while(walker.nextNode()) nodes.push(walker.currentNode);
            nodes.forEach(node => {
                if (node.parentElement.closest('script,style,.katex,mark')) return;
                const text = node.textContent, lower = text.toLowerCase(), needle = query.trim().toLowerCase();
                let at = 0, found = lower.indexOf(needle), fragment = document.createDocumentFragment();
                if(found < 0) return;
                const details = node.parentElement.closest('details'); if(details) details.open = true;
                while(found >= 0) {
                    fragment.append(document.createTextNode(text.slice(at,found)));
                    const mark = document.createElement('mark');mark.className='lsb-search-hit';mark.textContent=text.slice(found,found+needle.length);fragment.append(mark);
                    at=found+needle.length;found=lower.indexOf(needle,at);
                }
                fragment.append(document.createTextNode(text.slice(at)));node.replaceWith(fragment);
            });
        });
    };
    window.lsbContent = {
        update(next) {
            options = next;
            const rows = new Map(Array.from(list.querySelectorAll(':scope > [data-post-id]'),el=>[el.dataset.postId,el]));
            const desired = [];
            const slot = document.getElementById('lsb-native-slot') || document.createElement('li');slot.id='lsb-native-slot';slot.style.height=next.slotHeight+'px';
            const header = document.getElementById('lsb-comments') || document.createElement('li');header.className='lsb-comments-header';header.id='lsb-comments';header.textContent='全部回复 '+next.count;
            let commentsAdded = false;
            next.rows.forEach(row => {
                if(row.id !== next.mainId && next.comments && !commentsAdded){desired.push(slot,header);commentsAdded=true;}
                let node = rows.get(row.id);
                if(!node || saved.get(row.id)!==row.html){
                    const template=document.createElement('template');template.innerHTML=row.html;const replacement=template.content.firstElementChild;prep(replacement);
                    if(node)node.replaceWith(replacement);node=replacement;saved.set(row.id,row.html);
                }
                desired.push(node);
            });
            if(!commentsAdded){desired.push(slot);if(next.comments)desired.push(header);}
            const keep=new Set(desired);Array.from(list.children).forEach(node=>{if(!keep.has(node))node.remove();});
            desired.forEach((node,index)=>{if(list.children[index]!==node)list.insertBefore(node,list.children[index] || null);});
            for(const id of saved.keys())if(!next.rows.some(row=>row.id===id))saved.delete(id);
            query=next.query || '';highlight();notify();
        },
        metrics() {
            const top = el => el ? el.getBoundingClientRect().top + window.scrollY : 0;
            return {width:document.documentElement.clientWidth,height:Math.ceil(list.getBoundingClientRect().height),query,
                slot:top(document.getElementById('lsb-native-slot')),comments:top(document.getElementById('lsb-comments')),
                posts:Array.from(list.querySelectorAll(':scope > [data-post-id]'),el=>{
                    const body=el.querySelector('[data-body]'),match=el.querySelector('.lsb-search-hit');
                    const signature=body?.querySelector(':scope > .post-signature-content');
                    const bodyHeight=body?.getBoundingClientRect().height || 0;
                    return {id:el.dataset.postId,top:top(el),height:el.getBoundingClientRect().height,body:top(body),bodyHeight,
                        readingHeight:signature?Math.max(0,top(signature)-top(body)):bodyHeight,match:match?top(match):null};
                }),
                headings:Array.from(list.querySelector(`[data-post-id="${options.mainId || '0'}"] [data-body]`)?.querySelectorAll('h1,h2,h3,h4,h5,h6') || [],top)};
        },
        highlightPost(id) {
            list.querySelectorAll('.lsb-floor-highlight').forEach(el=>el.classList.remove('lsb-floor-highlight'));
            const el=list.querySelector(`[data-post-id="${id}"]`);if(el){el.classList.add('lsb-floor-highlight');setTimeout(()=>el.classList.remove('lsb-floor-highlight'),1800);}
        }
    };
    document.addEventListener('click',event=>{
        const image=event.target.closest('.lsb-image');
        if(image){
            event.preventDefault();const img=image.querySelector('img');
            if(image.classList.contains('is-error')){image.className='lsb-image is-loading';const src=img.src;img.removeAttribute('src');img.src=src;return;}
            location.href='lsb-action://image/?url='+encodeURIComponent(img.currentSrc || img.src);return;
        }
        const link=event.target.closest('a[href]');
        if(link){
            event.preventDefault();
            if(link.getAttribute('href').startsWith('#')){
                const target=document.getElementById(decodeURIComponent(link.hash.slice(1)));
                if(target)location.href='lsb-action://anchor/?top='+topOf(target);
            }else location.href=link.href;
        }
    });
    const topOf = el => el.getBoundingClientRect().top + window.scrollY;
    window.addEventListener('load',notify);document.fonts.ready.then(notify);
})();
