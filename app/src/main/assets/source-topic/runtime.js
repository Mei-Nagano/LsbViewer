/* 宿主交互脚本。源站内容不持有 Android 接口，操作仅通过拦截的导航回到原生。 */
(() => {
    const list = document.getElementById('lsb-posts');
    let options = {}, query = '', scheduled = false;
    const saved = new Map();
    let popover = null, popoverBtn = null, popoverUrl = '', suppressClickUntil = 0, pressTimer = null, pressStart = null;
    const closePopover = () => {
        if (popover) { popover.remove(); popover = null; popoverBtn = null; popoverUrl = ''; }
    };
    const showStickerPopover = (wrap, img) => {
        if (!options.canCollectSticker || !isCollectEligible(img)) return;
        closePopover();
        const rawUrl = imageSource(img);
        const alt = (img.alt || img.title || '').trim();
        const rect = wrap.getBoundingClientRect();
        const box = document.createElement('div');
        box.className = 'lsb-sticker-popover';
        const btn = document.createElement('button');
        btn.type = 'button';
        btn.textContent = '加入我的表情';
        btn.addEventListener('click', e => {
            e.preventDefault();
            e.stopPropagation();
            if (btn.disabled) return;
            btn.disabled = true;
            btn.textContent = '正在添加…';
            location.href = 'lsb-action://collect-sticker/?url=' + encodeURIComponent(rawUrl) + '&alt=' + encodeURIComponent(alt);
        });
        box.append(btn);
        document.body.append(box);
        const bw = box.offsetWidth || 120, bh = box.offsetHeight || 36;
        const vw = document.documentElement.clientWidth || window.innerWidth;
        const docTop = rect.top + window.scrollY;
        const docBottom = rect.bottom + window.scrollY;
        const centerX = Math.max(bw / 2 + 8, Math.min(vw - bw / 2 - 8, rect.left + rect.width / 2));
        const aboveTop = docTop - bh - 8;
        const top = aboveTop >= 8 ? aboveTop : docBottom + 8;
        box.style.left = centerX + 'px';
        box.style.top = Math.max(8, top) + 'px';
        popover = box;
        popoverBtn = btn;
        popoverUrl = rawUrl;
    };
    const clearPress = () => { if (pressTimer) { clearTimeout(pressTimer); pressTimer = null; } pressStart = null; };
    document.addEventListener('touchstart', e => {
        const wrap = e.target.closest('.lsb-image.is-sticker');
        if (!wrap || e.touches.length !== 1) {
            if (!e.target.closest('.lsb-sticker-popover')) closePopover();
            clearPress();
            return;
        }
        if (!options.canCollectSticker) return;
        const img = wrap.querySelector('img');
        if (!img || !isCollectEligible(img)) return;
        const t = e.touches[0];
        pressStart = {x: t.clientX, y: t.clientY};
        clearTimeout(pressTimer);
        pressTimer = setTimeout(() => {
            pressTimer = null;
            suppressClickUntil = Date.now() + 700;
            showStickerPopover(wrap, img);
        }, 420);
    }, {passive: true});
    document.addEventListener('touchmove', e => {
        if (!pressStart || !pressTimer || e.touches.length !== 1) return;
        const t = e.touches[0];
        if (Math.hypot(t.clientX - pressStart.x, t.clientY - pressStart.y) > 10) clearPress();
    }, {passive: true});
    document.addEventListener('touchend', clearPress, {passive: true});
    document.addEventListener('touchcancel', clearPress, {passive: true});
    document.addEventListener('contextmenu', e => {
        const wrap = e.target.closest('.lsb-image.is-sticker');
        if (!wrap || !options.canCollectSticker) return;
        const img = wrap.querySelector('img');
        if (!img || !isCollectEligible(img)) return;
        e.preventDefault();
        clearPress();
        suppressClickUntil = Date.now() + 700;
        showStickerPopover(wrap, img);
    });
    window.addEventListener('scroll', closePopover, {passive: true});
    const notify = () => {
        if (scheduled) return;
        scheduled = true;
        requestAnimationFrame(() => { scheduled = false; console.debug('lsb-content-layout'); });
    };
    const resize = new ResizeObserver(notify);
    resize.observe(list);
    const isStickerImg = img => /\/app\/upload\/sticker_pack\//i.test(img.getAttribute('src') || img.src || '');
    const isRasterUrl = source => !!source && !/(?:^data:image\/svg\+xml|\.svg(?:$|[?#]))/i.test(source)
        && /\.(?:avif|bmp|gif|jpe?g|png|webp)(?:$|[?#])/i.test(source);
    const imageSource = img => {
        const linked = img.closest('a[href]')?.href || '';
        return isRasterUrl(linked) ? linked : (img.currentSrc || img.src || '');
    };
    const isCollectEligible = img => {
        if (img.closest('.post-avatar,.avatar-profile-link') || img.matches('.avatar-img,.emoji,.emoticon')) return false;
        const src = imageSource(img);
        return !/\/(?:avatar|avatar_upload)(?:[\/_-]|$)/i.test(src) && isRasterUrl(src);
    };
    const prep = root => {
        root.querySelectorAll('[data-nb-editor-math]').forEach(el => {
            if (!window.katex) return;
            const source = el.textContent;
            try { katex.render(source, el, {displayMode:el.dataset.nbEditorMath === 'display', throwOnError:false, trust:false, maxExpand:1000, maxSize:20}); }
            catch (_) { el.textContent = source; }
        });
        root.querySelectorAll('pre code').forEach(el => { if (window.hljs) hljs.highlightElement(el); });
        root.querySelectorAll('[data-body] img').forEach(img => {
            const sticker = isStickerImg(img);
            const wrap = document.createElement('span');
            wrap.className = sticker ? 'lsb-image is-sticker is-loading' : 'lsb-image is-loading';
            img.replaceWith(wrap); wrap.append(img);
            if (!sticker) img.classList.add('image-lightbox-clickable');
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
        },
        stickerCollectResult(url, ok) {
            if (!popover || !popoverBtn || popoverUrl !== url) return;
            if (ok) {
                popoverBtn.textContent = '已加入我的表情';
                popoverBtn.disabled = true;
                setTimeout(closePopover, 800);
            } else {
                popoverBtn.textContent = '加入我的表情';
                popoverBtn.disabled = false;
            }
        }
    };
    document.addEventListener('click',event=>{
        if (event.target.closest('.lsb-sticker-popover')) return;
        if (Date.now() < suppressClickUntil) { event.preventDefault(); return; }
        if (popover) closePopover();
        const image=event.target.closest('.lsb-image');
        if(image){
            event.preventDefault();const img=image.querySelector('img');
            if(image.classList.contains('is-error')){
                image.className=image.classList.contains('is-sticker')?'lsb-image is-sticker is-loading':'lsb-image is-loading';
                const src=img.src;img.removeAttribute('src');img.src=src;return;
            }
            const rawUrl=imageSource(img);
            const alt=(img.alt || img.title || '').trim();
            const eligible=isCollectEligible(img)?'1':'0';
            location.href='lsb-action://image/?url='+encodeURIComponent(rawUrl)+'&alt='+encodeURIComponent(alt)+'&eligible='+eligible;return;
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
