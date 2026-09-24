const fs=require('fs'),path=require('path');
const {chromium}=require('../.tools/browser/node_modules/playwright-core');
const out=path.resolve('docs/presentation/assets');fs.mkdirSync(out,{recursive:true});
(async()=>{
 const source=fs.readFileSync('config/portal-admin-credentials.txt','utf8');
 const admin={loginId:source.match(/^username\s*[:=]\s*(.+)$/mi)[1].trim(),password:source.match(/^password\s*[:=]\s*(.+)$/mi)[1].trim()};
 const accounts=JSON.parse(fs.readFileSync('config/portal-demo-credentials.txt','utf8')).accounts;
 const browser=await chromium.launch({executablePath:'C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe',headless:true});
 try {
  async function login(a){const p=await browser.newPage({viewport:{width:1600,height:1000},deviceScaleFactor:1});await p.goto('http://localhost:8080/login');await p.locator('#username').fill(a.loginId);await p.locator('#password').fill(a.password);await Promise.all([p.waitForNavigation(),p.locator('form button').click()]);if(p.url().includes('/login'))throw Error('Presentation login failed');return p;}
  const ap=await login(admin);await ap.goto('http://localhost:8080/admin/employee-manage');
  const people=await ap.locator('[data-edit-employee]').evaluateAll(es=>es.map((e,i)=>({name:e.dataset.name,login:e.dataset.login,label:e.dataset.role==='ADMIN'?'관리자':`직원 ${String(i+1).padStart(2,'0')}`})));
  const staff=await login(accounts.find(a=>!a.departmentManager));const boss=await login(accounts.find(a=>a.departmentManager));
  async function mask(p){await p.evaluate(people=>{const walk=document.createTreeWalker(document.body,NodeFilter.SHOW_TEXT);let n;while(n=walk.nextNode()){if(['SCRIPT','STYLE'].includes(n.parentElement?.tagName))continue;let t=n.textContent;for(const e of people){if(e.name.length>1)t=t.split(e.name).join(e.label);if(e.login.length>2)t=t.split(e.login).join('demo-user');}n.textContent=t.replace(/[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}/g,'user@example.com').replace(/01[016789][- ]?\d{3,4}[- ]?\d{4}/g,'010-0000-0000');}document.querySelectorAll('.profile-avatar img').forEach(i=>i.remove());document.querySelectorAll('[data-employee-row] .profile-avatar span').forEach(e=>e.textContent='직');},people);}
  async function snap(p,url,name,action){await p.goto('http://localhost:8080'+url);if(action)await action(p);await mask(p);await p.screenshot({path:path.join(out,name+'.png')});}
  await snap(staff,'/employee/main','dashboard');
  await snap(staff,'/employee/attendance-status','attendance');
  await snap(staff,'/employee/correction-request','correction');
  await snap(boss,'/manager/leave-calendar','department-calendar');
  await snap(ap,'/admin/employee-manage','employees');
  await snap(ap,'/admin/employee-manage','employee-drawer',p=>p.locator('#openEmployeeCreate').click());
  await snap(staff,'/groupware/reservations','reservations');
  await snap(staff,'/groupware/tasks','tasks');
  fs.copyFileSync('.tools/browser/profile-chat-mobile.png',path.join(out,'chat-mobile.png'));
  console.log('Captured 8 read-only application screens with masked names/contact details; copied isolated-test chat screen.');
 } finally {await browser.close();}
})().catch(e=>{console.error(e.message);process.exitCode=1;});
