const {chromium}=require('../.tools/browser/node_modules/playwright-core');
const fs=require('node:fs');
const path=require('node:path');
const {pathToFileURL}=require('node:url');
(async()=>{
 const dir=path.resolve('docs/database');
 const browser=await chromium.launch({executablePath:'C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe',headless:true});
 try{
  const page=await browser.newPage({deviceScaleFactor:1});
  for(const name of process.argv.includes('--dictionary-only')?[]:['portal-erd','erd-people','erd-attendance','erd-collaboration','erd-resources','portal-erd-all-columns']){
   const source=fs.readFileSync(path.join(dir,name+'.svg'),'utf8');
   const width=Number(source.match(/width="(\d+)"/)[1]),height=Number(source.match(/height="(\d+)"/)[1]);
   await page.setViewportSize({width,height});await page.setContent('<html><meta charset="utf-8"><body style="margin:0">'+source+'</body></html>');
   await page.evaluate(()=>document.fonts.ready);await page.screenshot({path:path.join(dir,name+'.png'),fullPage:true});
   if(name==='portal-erd-all-columns'){
    const metadata=JSON.parse(fs.readFileSync(path.join(dir,'schema-metadata.json'),'utf8'));
    const textCount=await page.locator('svg text.mono').count();
    if(textCount!==18+metadata.columns.length*2)throw Error('Full ERD text coverage '+textCount);
    const clipped=await page.locator('svg').evaluate(svg=>[...svg.querySelectorAll('text')].filter(t=>{const b=t.getBBox();return b.x<0||b.y<0||b.x+b.width>svg.viewBox.baseVal.width||b.y+b.height>svg.viewBox.baseVal.height;}).length);
    if(clipped)throw Error('Clipped ERD labels: '+clipped);
    console.log('Full ERD verified: 18 tables, 193 column names and types; '+width+' x '+height+' pixels.');
   }
  }
  await page.goto(pathToFileURL(path.join(dir,'column-dictionary.html')).href);await page.evaluate(()=>document.fonts.ready);
  await page.pdf({path:path.join(dir,'column-dictionary.pdf'),format:'A4',landscape:true,printBackground:true,margin:{top:'14mm',bottom:'14mm',left:'12mm',right:'12mm'},displayHeaderFooter:true,headerTemplate:'<div></div>',footerTemplate:'<div style="font-size:9px;width:100%;text-align:center;color:#778198">PORTAL DATABASE · 2026-09-24 · <span class="pageNumber"></span> / <span class="totalPages"></span></div>'});
  const rows=await page.locator('.table-section').count();if(rows!==new Set(JSON.parse(fs.readFileSync(path.join(dir,'schema-metadata.json'),'utf8')).columns.map(c=>c.table_name)).size+2)throw Error('Unexpected section count '+rows);
  const dictionaryRows=await page.locator('.table-section[id] tbody tr').count();if(dictionaryRows!==JSON.parse(fs.readFileSync(path.join(dir,'schema-metadata.json'),'utf8')).columns.length)throw Error('Column coverage '+dictionaryRows);
  console.log('Rendered dictionary PDF. HTML has '+rows+' detail/reference sections.');
 }finally{await browser.close();}
})().catch(e=>{console.error(e.message);process.exitCode=1;});
