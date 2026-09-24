(() => {
  const widget = document.querySelector('.chat-widget');
  if (!widget) return;
  const el = id => document.getElementById(id);
  const panel=el('chatPanel'), launcher=el('chatLauncher'), log=el('chatMessages'), input=el('chatInput');
  let mode='list', peer=null, revision=0, unread=0, polling=false, sending=false, reading=false, loadingOlder=false;
  let timer, searchTimer, readTimer, messages=new Map(), drafts=new Map(), retries=new Map(), hasMore=false;
  const text=(tag,cls,value)=>{const node=document.createElement(tag);if(cls)node.className=cls;node.textContent=value;return node;};
  const time=value=>value ? value.replace('T',' ').slice(5,16) : '';
  const available=()=>!panel.hidden&&!document.hidden&&document.hasFocus()&&!document.querySelector('dialog[open]');
  const bottom=()=>log.scrollHeight-log.scrollTop-log.clientHeight<45;
  function error(message){el('chatError').textContent=message;el('chatError').hidden=!message;}
  async function api(path, data) {
    const options={credentials:'same-origin',redirect:'error',cache:'no-store',headers:{Accept:'application/json'}};
    if(data){options.method='POST';const body=new URLSearchParams();for(const [key,value] of Object.entries(data)){if(Array.isArray(value))value.forEach(v=>body.append(key,String(v)));else body.set(key,String(value));}body.set(widget.dataset.csrfName,widget.dataset.csrfToken);options.body=body;}
    let response;try{response=await fetch('/api/chat'+path,options);}catch{throw Error('연결을 확인하고 다시 시도해주세요. 로그인이 만료되었다면 새로고침해주세요.');}
    if(!response.ok){let detail;try{detail=await response.json();}catch{}throw Error(detail?.message||(response.status===403?'로그인 상태를 확인하고 새로고침해주세요.':'메시지를 불러오지 못했습니다. 잠시 후 다시 시도해주세요.'));}
    if(response.status===204)return null;
    if(!response.headers.get('content-type')?.includes('application/json'))throw Error('로그인이 만료되었습니다. 새로고침해주세요.');
    return response.json();
  }
  function badge(count){unread=count;el('chatUnread').textContent=count>99?'99+':String(count);el('chatUnread').hidden=count===0;launcher.setAttribute('aria-label',`${panel.hidden?'채팅 열기':'채팅 닫기'}${count?`, 안 읽은 메시지 ${count}개`:''}`);}
  function saveDraft(){if(peer)drafts.set(peer.id,input.value);}
  function view(next){mode=next;revision++;error('');el('chatListView').hidden=next!=='list';el('chatPeopleView').hidden=next!=='people';el('chatThreadView').hidden=next!=='thread';el('chatBack').hidden=next==='list';el('chatPanelTitle').textContent=next==='people'?'새 대화':next==='thread'?peer.name:'쪽지 · 채팅';el('chatSubtitle').textContent=next==='thread'?`${peer.department} · ${peer.position}`:'동료와 나누는 대화';}
  function row(person,preview,stamp,count){
    const button=text('button','chat-person','');button.type='button';button.dataset.peerId=person.id;
    const avatar=text('span','chat-avatar',person.name.slice(0,1));
    if(person.profileImageUrl){const img=document.createElement('img');img.src=person.profileImageUrl;img.alt='';img.loading='lazy';avatar.append(img);}
    button.append(avatar);
    const info=text('span','chat-person-info','');info.append(text('strong','',person.name),text('small','',`${person.department} · ${person.position}${person.available?'':' · 비활성'}`));if(preview!==undefined)info.append(text('span','chat-preview',preview));button.append(info);
    const meta=text('span','chat-person-meta','');if(stamp)meta.append(text('time','',time(stamp)));if(count)meta.append(text('span','chat-row-unread',count>99?'99+':String(count)));button.append(meta);button.addEventListener('click',()=>openThread(person));return button;
  }
  function list(container,rows,empty){container.replaceChildren();if(!rows.length)container.append(text('p','chat-empty-note',empty));else rows.forEach(r=>container.append(r));}
  async function inbox(){const data=await api('/inbox');badge(data.unread);if(!panel.hidden&&mode==='list'){
    const container=el('chatThreads'),focused=document.activeElement?.dataset.peerId,top=container.scrollTop;
    list(container,data.threads.map(t=>row(t.person,t.preview,t.sentAt,t.unread)),'아직 대화가 없습니다. 새 대화로 동료에게 메시지를 보내보세요.');container.scrollTop=top;if(focused)container.querySelector(`[data-peer-id="${focused}"]`)?.focus({preventScroll:true});
  }}
  async function people(){const version=revision,q=el('chatSearch').value;try{const data=await api('/people?query='+encodeURIComponent(q));if(version!==revision||mode!=='people'||q!==el('chatSearch').value)return;list(el('chatPeople'),data.map(p=>row(p)),'검색 조건에 맞는 직원이 없습니다.');}catch(e){if(version===revision)error(e.message);}}
  function render(older=false,forceBottom=false){
    const height=log.scrollHeight,top=log.scrollTop,atBottom=bottom();log.replaceChildren();
    [...messages.values()].sort((a,b)=>a.id-b.id).forEach(m=>{const item=text('article','chat-message'+(m.mine?' mine':''),'');item.dataset.messageId=m.id;item.append(text('p','chat-bubble',m.content));const meta=text('div','chat-message-meta','');meta.append(text('time','',time(m.sentAt)));if(m.mine)meta.append(text('span','',m.readAt?'읽음':'안 읽음'));item.append(meta);log.append(item);});
    if(!messages.size)log.append(text('p','chat-empty-note','첫 메시지를 보내 대화를 시작하세요.'));
    if(older)log.scrollTop=top+(log.scrollHeight-height);else if(forceBottom||atBottom)log.scrollTop=log.scrollHeight;else log.scrollTop=top;
    el('chatOlder').hidden=!hasMore;el('chatLatest').hidden=bottom();scheduleRead();
  }
  async function history(older=false,initial=false){
    if(!peer)return;const version=revision,id=peer.id;
    const before=older&&messages.size?Math.min(...messages.keys()):null;
    const data=await api(`/threads/${id}${before?'?before='+before:''}`);
    if(version!==revision||mode!=='thread')return;
    peer=data.person;input.disabled=!peer.available;el('chatSend').disabled=!peer.available||sending;
    el('chatComposeHint').textContent=peer.available?'Enter 전송 · Shift+Enter 줄바꿈':'비활성 계정에는 메시지를 보낼 수 없습니다.';
    let changed=initial;for(const m of data.messages){const prior=messages.get(m.id);if(!prior||prior.readAt!==m.readAt)changed=true;messages.set(m.id,m);}
    if(older||initial)hasMore=data.hasMore;
    if(changed||older)render(older,initial);else scheduleRead();
  }
  async function openThread(person){saveDraft();peer=person;messages=new Map();hasMore=false;view('thread');input.value=drafts.get(peer.id)||'';input.disabled=true;el('chatSend').disabled=true;el('chatOlder').hidden=true;el('chatLatest').hidden=true;log.replaceChildren(text('p','chat-empty-note','대화를 불러오는 중입니다…'));const version=revision;try{await history(false,true);if(version===revision&&available()&&innerWidth>650)input.focus();}catch(e){if(version===revision)error(e.message);}}
  function scheduleRead(){clearTimeout(readTimer);readTimer=setTimeout(readVisible,250);}
  async function readVisible(){
    if(reading||mode!=='thread'||!available())return;const box=log.getBoundingClientRect();
    const ids=[...log.querySelectorAll('[data-message-id]')].filter(node=>{const m=messages.get(Number(node.dataset.messageId)),r=node.getBoundingClientRect();return m&&!m.mine&&!m.readAt&&r.bottom>box.top&&r.top<box.bottom;}).map(n=>Number(n.dataset.messageId));
    if(!ids.length)return;const version=revision,id=peer.id;reading=true;
    try{await api(`/threads/${id}/read`,{ids});if(version===revision)ids.forEach(key=>{const m=messages.get(key);if(m)m.readAt=new Date().toISOString();});await inbox();}catch(e){if(version===revision&&!panel.hidden)error(e.message);}finally{reading=false;}
  }
  async function poll(){
    clearTimeout(timer);if(polling)return;if(document.hidden){timer=setTimeout(poll,5000);return;}polling=true;
    try{await inbox();if(!panel.hidden&&mode==='thread')await history();}catch(e){if(!panel.hidden)error(e.message);}finally{polling=false;timer=setTimeout(poll,panel.hidden?10000:4000);}
  }
  function setOpen(open,focus=true){saveDraft();panel.hidden=!open;launcher.setAttribute('aria-expanded',String(open));badge(unread);if(open){el('chatClose').focus();poll();scheduleRead();}else if(focus)launcher.focus();}
  launcher.addEventListener('click',()=>setOpen(panel.hidden));el('chatClose').addEventListener('click',()=>setOpen(false));
  el('chatBack').addEventListener('click',()=>{saveDraft();view('list');peer=null;poll();el('chatNew').focus();});
  el('chatNew').addEventListener('click',()=>{saveDraft();view('people');peer=null;el('chatSearch').value='';people();el('chatSearch').focus();});
  el('chatSearch').addEventListener('input',()=>{clearTimeout(searchTimer);searchTimer=setTimeout(people,250);});
  el('chatOlder').addEventListener('click',async()=>{if(loadingOlder)return;const version=revision;loadingOlder=true;el('chatOlder').disabled=true;try{await history(true);}catch(e){if(version===revision)error(e.message);}finally{loadingOlder=false;el('chatOlder').disabled=false;}});
  el('chatLatest').addEventListener('click',()=>{log.scrollTop=log.scrollHeight;el('chatLatest').hidden=true;scheduleRead();});
  log.addEventListener('scroll',()=>{el('chatLatest').hidden=bottom();scheduleRead();});
  input.addEventListener('input',saveDraft);
  input.addEventListener('keydown',event=>{if(event.key==='Enter'&&!event.shiftKey&&!event.isComposing&&event.keyCode!==229){event.preventDefault();el('chatSendForm').requestSubmit();}});
  el('chatSendForm').addEventListener('submit',async event=>{
    event.preventDefault();if(sending||!peer||!peer.available)return;const content=input.value.trim();if(!content)return;
    const id=peer.id,version=revision;const previous=retries.get(id);const pending=previous?.content===content?previous:{content,clientId:crypto.randomUUID()};retries.set(id,pending);sending=true;el('chatSend').disabled=true;error('');
    try{const m=await api(`/threads/${id}`,pending);retries.delete(id);if(drafts.get(id)?.trim()===content)drafts.delete(id);if(version===revision){if(input.value.trim()===content)input.value='';messages.set(m.id,m);render(false,true);input.focus();}await inbox();}
    catch(e){if(version===revision)error(e.message+' 입력한 메시지는 유지됩니다.');}
    finally{sending=false;el('chatSend').disabled=!peer?.available;}
  });
  document.addEventListener('keydown',event=>{if(event.key==='Escape'&&!panel.hidden&&!document.querySelector('dialog[open]')){event.preventDefault();setOpen(false);}});
  document.addEventListener('click',event=>{if(!panel.hidden&&!widget.contains(event.target))setOpen(false,false);});
  document.addEventListener('visibilitychange',()=>{if(!document.hidden)poll();});window.addEventListener('focus',()=>{poll();scheduleRead();});
  poll();
})();
