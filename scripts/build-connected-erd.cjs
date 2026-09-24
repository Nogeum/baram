const fs=require('node:fs');
const path=require('node:path');
const {instance}=require('../.tools/erd/node_modules/@viz-js/viz');
const {chromium}=require('../.tools/browser/node_modules/playwright-core');
const dir=path.resolve('docs/database');
const meta=JSON.parse(fs.readFileSync(path.join(dir,'schema-metadata.json'),'utf8'));
const escape=s=>String(s).replaceAll('&','&amp;').replaceAll('<','&lt;').replaceAll('>','&gt;').replaceAll('"','&quot;');
const layout={
 ATTENDANCE:[0,0],CORRECTION:[1,0],AMENDMENT:[2,0],LEAVE:[3,0],
 SCHEDULE:[0,1],EMPLOYEE:[1,1],OVERTIME:[2,1],DOCUMENT:[3,1],
 NOTICE:[0,2],NOTIFICATION:[1,2],TASK:[2,2],DOCUMENT_STEP:[3,2],
 HOLIDAY:[0,3],LIBRARY:[1,3],TASK_COMMENT:[2,3],FILE:[3,3],
 CHAT_MESSAGE:[0,4],RESOURCE:[1,4],RESERVATION:[2,4]
};
const descriptions={
 EMPLOYEE:'직원·계정',ATTENDANCE:'출퇴근 기록',CORRECTION:'출퇴근 정정 신청',AMENDMENT:'승인 취소·변경 요청',
 LEAVE:'휴가 신청',OVERTIME:'초과근무 신청',SCHEDULE:'개인·부서 일정',
 DOCUMENT:'전자결재 문서',DOCUMENT_STEP:'전자결재 단계',TASK:'개인 업무',TASK_COMMENT:'업무 댓글',
 LIBRARY:'공유 자료실',FILE:'첨부파일',NOTICE:'사내 공지',NOTIFICATION:'개인 알림',HOLIDAY:'공휴일·회사 휴무일',
 RESOURCE:'회의실·장비',RESERVATION:'자원 예약·허가',CHAT_MESSAGE:'직원 간 채팅'
};
const color=t=>/EMPLOYEE/.test(t)?'#203e73':/ATTENDANCE|CORRECTION|AMENDMENT|LEAVE|OVERTIME|SCHEDULE/.test(t)?'#087e8b':/RESOURCE|RESERVATION/.test(t)?'#b46b15':/TASK|DOCUMENT|FILE|LIBRARY/.test(t)?'#7450a6':'#2854a2';
const type=c=>c.data_type==='NUMBER'?`NUMBER(${c.data_precision},${c.data_scale})`:c.data_type==='VARCHAR2'?`VARCHAR2(${c.char_length})`:c.data_type;
(async()=>{
 let dot='digraph Portal {\ngraph [layout=neato, overlap=true, splines=true, outputorder=edgesfirst, bgcolor="#f7f9fc", pad="0.5", fontname="Arial", fontsize=24, labelloc=t, label="PORTAL ERD - All columns\\n19 tables / 202 columns / 30 physical foreign keys\\nSolid arrow: parent PK -> child FK (1 : 0..N) | Dashed: logical reference, no FK\\nPK = primary key   UK* = part of composite unique key   ? = nullable FK"];\nnode [shape=plain,fontname="Arial",fontsize=12,pin=true];\nedge [color="#526b91",penwidth=1.7,arrowsize=1.2,arrowhead=normal];\n';
 for(const [name,[x,y]] of Object.entries(layout)){
  const table='PORTAL_'+name,cols=meta.columns.filter(c=>c.table_name===table).sort((a,b)=>(a.column_name==='ID'?-1:b.column_name==='ID'?1:Number(a.column_id)-Number(b.column_id)));
  const rows=cols.map(c=>{
   const keys=meta.keys.filter(k=>k.table_name===table&&k.column_name===c.column_name);
   const tags=keys.map(k=>k.constraint_type==='P'?'PK':k.constraint_type==='R'?'FK'+(c.nullable==='Y'?'?':''):meta.keys.filter(a=>a.constraint_name===k.constraint_name).length>1?'UK*':'UK').join('/');
   return `<TR><TD WIDTH="60" ALIGN="LEFT"><FONT COLOR="${color(table)}">${tags || '&#160;'}</FONT></TD><TD PORT="${c.column_name}" WIDTH="230" ALIGN="LEFT">${c.column_name}</TD><TD WIDTH="185" ALIGN="LEFT"><FONT COLOR="#63738b">${type(c)}${c.nullable==='N'?' NN':''}</FONT></TD></TR>`;
  }).join('');
  dot+=`"${table}" [pos="${x*790/72},${(4-y)*750/72}!", label=<<TABLE BORDER="1" COLOR="${color(table)}" CELLBORDER="0" CELLSPACING="0" CELLPADDING="5" BGCOLOR="white"><TR><TD COLSPAN="3" HEIGHT="34" ALIGN="LEFT" BGCOLOR="${color(table)}"><FONT COLOR="white" POINT-SIZE="17"><B>${table}</B></FONT></TD></TR>${rows}</TABLE>>];\n`;
 }
 for(const k of meta.keys.filter(k=>k.constraint_type==='R')){
  dot+=`"${k.target_table}" -> "${k.table_name}" [tooltip="${k.target_table}.${k.target_column} -> ${k.table_name}.${k.column_name}"];\n`;
 }
 for(const [source,col,targets]of [['PORTAL_EMPLOYEE','PROFILE_FILE_ID',['PORTAL_FILE']],['PORTAL_FILE','OWNER_ID',['PORTAL_TASK','PORTAL_DOCUMENT','PORTAL_LIBRARY','PORTAL_EMPLOYEE']],['PORTAL_AMENDMENT','REQUEST_ID',['PORTAL_LEAVE','PORTAL_OVERTIME','PORTAL_CORRECTION']],['PORTAL_AMENDMENT','REPLACEMENT_ID',['PORTAL_LEAVE','PORTAL_OVERTIME','PORTAL_CORRECTION']],['PORTAL_CORRECTION','WORK_DATE',['PORTAL_ATTENDANCE']]]){
  for(const target of targets)dot+=`"${target}" -> "${source}" [style=dashed,color="#c79762",penwidth=1.05,tooltip="Logical reference (no FK): ${target} -> ${source}.${col}"];\n`;
 }
 dot+='}\n';
 const viz=await instance();let svg=viz.renderString(dot,{format:'svg',engine:'neato'});
 svg=svg.replace('<svg ', '<svg data-column-count="202" ');
 fs.writeFileSync(path.join(dir,'portal-erd-connected.dot'),dot);
 fs.writeFileSync(path.join(dir,'portal-erd-connected.svg'),svg);
 const browser=await chromium.launch({executablePath:'C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe',headless:true});
 try{
  const p=await browser.newPage({deviceScaleFactor:1.5});
  const vb=svg.match(/viewBox="([\d. -]+)"/)[1].split(' ').map(Number);const width=Math.ceil(vb[2]),height=Math.ceil(vb[3]);
  await p.setViewportSize({width,height});
  await p.setContent('<html><body style="margin:0">'+svg.replace(/width="[\d.]+pt" height="[\d.]+pt"/,`width="${width}" height="${height}"`)+'</body></html>');
  await p.evaluate(()=>document.fonts.ready);
  await p.evaluate(descriptions=>{
   for(const node of document.querySelectorAll('g.node')){
    const table=node.querySelector('title').textContent;
    const title=[...node.querySelectorAll('text')].find(t=>t.textContent===table);
    const box=title.getBBox();
    const text=document.createElementNS('http://www.w3.org/2000/svg','text');
    text.setAttribute('x',box.x+box.width+12);text.setAttribute('y',title.getAttribute('y'));
    text.setAttribute('font-family','Malgun Gothic, sans-serif');text.setAttribute('font-size','14');text.setAttribute('fill','#ffffff');
    text.setAttribute('class','table-description');text.textContent='('+descriptions[table.replace('PORTAL_','')]+')';
    node.appendChild(text);
   }
  },descriptions);
  await p.evaluate(()=>document.fonts.ready);
  const clipped=await p.locator('g.node').evaluateAll(nodes=>nodes.filter(n=>{
   const label=n.querySelector('.table-description').getBBox();
   const right=Math.max(...[...n.querySelectorAll('polygon')].map(p=>{const b=p.getBBox();return b.x+b.width;}));
   return label.x+label.width>right-4;
  }).length);
  if(clipped)throw Error('Korean table description exceeds header width');
  if(await p.locator('g.node').count()!==19||await p.locator('g.edge').count()!==42)throw Error('Graph coverage failed');
  for(const table of [...new Set(meta.columns.map(c=>c.table_name))]){
   const contents=await p.locator('g.node').filter({has:p.locator('title',{hasText:new RegExp('^'+table+'$')})}).locator('text').allTextContents();
   for(const c of meta.columns.filter(c=>c.table_name===table))if(!contents.includes(c.column_name))throw Error('Missing '+table+'.'+c.column_name);
  }
  if(await p.locator('.table-description').count()!==19)throw Error('Korean description coverage failed');
  svg=await p.locator('svg').evaluate(el=>el.outerHTML);
  fs.writeFileSync(path.join(dir,'portal-erd-connected.svg'),svg);
  fs.writeFileSync(path.join(dir,'portal-erd-connected-ko.svg'),svg);
  await p.screenshot({path:path.join(dir,'portal-erd-connected.png'),fullPage:true});
  fs.copyFileSync(path.join(dir,'portal-erd-connected.png'),path.join(dir,'portal-erd-connected-ko.png'));
  console.log(`Verified 19 tables, all 202 columns, 30 FK arrows + 12 logical links; PNG ${Math.round(width*1.5)} x ${Math.round(height*1.5)}.`);
 }finally{await browser.close();}
})().catch(e=>{console.error(e.message);process.exitCode=1;});
