import { spawn } from 'node:child_process';
import { createServer } from 'node:http';
import { access, mkdir, mkdtemp, readFile, rm, writeFile } from 'node:fs/promises';
import { constants } from 'node:fs';
import { homedir, tmpdir } from 'node:os';
import { basename, dirname, extname, join, resolve } from 'node:path';
import { setTimeout as delay } from 'node:timers/promises';
import { fileURLToPath, pathToFileURL } from 'node:url';
import { writeWav } from './audio.mjs';

const ROOT=fileURLToPath(new URL('.',import.meta.url));
const DEFAULT={width:1920,height:1080,fps:60,duration:18};
const REVIEW_TIMES=[0,1.5,3,5,7,9,11,13,15,17];
const mime={'.html':'text/html; charset=utf-8','.js':'text/javascript; charset=utf-8','.mjs':'text/javascript; charset=utf-8','.css':'text/css; charset=utf-8','.png':'image/png'};

function argValue(name,fallback){
  const exact=process.argv.indexOf(`--${name}`);
  if(exact>=0&&process.argv[exact+1]&&!process.argv[exact+1].startsWith('--'))return process.argv[exact+1];
  const prefix=process.argv.find(a=>a.startsWith(`--${name}=`));
  return prefix?prefix.slice(name.length+3):fallback;
}
function config(){
  const c={
    width:Number(argValue('width',process.env.WIDTH||DEFAULT.width)),
    height:Number(argValue('height',process.env.HEIGHT||DEFAULT.height)),
    fps:Number(argValue('fps',process.env.FPS||DEFAULT.fps)),
    duration:Number(argValue('duration',process.env.DURATION||DEFAULT.duration))
  };
  for(const [k,v] of Object.entries(c))if(!Number.isFinite(v)||v<=0)throw new Error(`Invalid ${k}: ${v}`);
  c.width=Math.round(c.width);c.height=Math.round(c.height);c.fps=Math.round(c.fps);
  if(c.width%2)c.width++;if(c.height%2)c.height++;
  return c;
}
async function executable(candidates,label){
  for(const p of candidates){if(!p)continue;try{await access(p,constants.X_OK);return p;}catch{}}
  throw new Error(`${label} was not found. Install it or set ${label==='Chromium'?'CHROME_BIN':'FFMPEG_BIN'}.`);
}
async function findFfmpeg(){
  const env=process.env.FFMPEG_BIN;
  if(env)return executable([env],'FFmpeg');
  return new Promise((resolvePath,reject)=>{
    const p=spawn(process.platform==='win32'?'where':'which',['ffmpeg'],{stdio:['ignore','pipe','ignore']});
    let out='';p.stdout.on('data',d=>out+=d);p.on('error',()=>reject(new Error('FFmpeg was not found. Install it or set FFMPEG_BIN.')));
    p.on('close',code=>code===0&&out.trim()?resolvePath(out.trim().split(/\r?\n/)[0]):reject(new Error('FFmpeg was not found. Install it or set FFMPEG_BIN.')));
  });
}
function chromiumCandidates(){
  return [process.env.CHROME_BIN,'/usr/bin/chromium','/usr/bin/chromium-browser','/usr/bin/google-chrome','/opt/google/chrome/chrome',
    '/Applications/Google Chrome.app/Contents/MacOS/Google Chrome','C:/Program Files/Google/Chrome/Application/chrome.exe',
    'C:/Program Files (x86)/Google/Chrome/Application/chrome.exe','C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe',
    join(homedir(),'AppData/Local/Google/Chrome/Application/chrome.exe')].filter(Boolean);
}
async function findChromePath(){
  try{return await executable(chromiumCandidates(),'Chromium');}catch{
    if(process.platform==='win32'){
      const where=await runProcess('where',['chrome.exe'],{stdio:'pipe'}).catch(()=>null);
      if(where)return where;
    }
    throw new Error('Chromium was not found. Install it or set CHROME_BIN to its executable path.');
  }
}

class CDP {
  constructor(url){
    this.ws=new WebSocket(url);this.id=0;this.pending=new Map();
    this.ready=new Promise((resolve,reject)=>{
      this.ws.addEventListener('open',resolve,{once:true});
      this.ws.addEventListener('error',()=>reject(new Error('Could not connect to Chromium DevTools.')),{once:true});
    });
    this.ws.addEventListener('message',event=>{
      let message;try{message=JSON.parse(event.data);}catch{return;}
      if(!message.id)return;
      const waiter=this.pending.get(message.id);if(!waiter)return;
      this.pending.delete(message.id);clearTimeout(waiter.timer);
      if(message.error)waiter.reject(new Error(message.error.message));else waiter.resolve(message.result);
    });
    this.ws.addEventListener('close',()=>{
      for(const waiter of this.pending.values()){clearTimeout(waiter.timer);waiter.reject(new Error('Chromium DevTools connection closed.'));}
      this.pending.clear();
    });
  }
  async send(method,params={}){
    await this.ready;const id=++this.id;
    return new Promise((resolve,reject)=>{
      const timer=setTimeout(()=>{this.pending.delete(id);reject(new Error(`CDP timeout: ${method}`));},45000);
      this.pending.set(id,{resolve,reject,timer});this.ws.send(JSON.stringify({id,method,params}));
    });
  }
  close(){try{this.ws.close();}catch{}}
}

async function getPageTarget(port){
  for(let i=0;i<100;i++){
    try{
      const response=await fetch(`http://127.0.0.1:${port}/json/list`);
      const targets=await response.json();const page=targets.find(t=>t.type==='page'&&t.webSocketDebuggerUrl);
      if(page)return page;
    }catch{}
    await delay(100);
  }
  throw new Error('Chromium opened but its page target was not available.');
}
async function openRenderer(c){
  const chrome=await findChromePath();
  const profile=await mkdtemp(join(tmpdir(),'course-reel-chrome-'));
  const portFile=join(profile,'DevToolsActivePort');
  const url=pathToFileURL(join(ROOT,'showreel.html'));
  url.searchParams.set('width',String(c.width));url.searchParams.set('height',String(c.height));
  url.searchParams.set('fps',String(c.fps));url.searchParams.set('duration',String(c.duration));
  const child=spawn(chrome,[
    '--headless=new','--no-sandbox','--disable-gpu','--hide-scrollbars','--no-first-run','--no-default-browser-check',
    '--disable-background-networking','--disable-extensions','--disable-features=Translate,BackForwardCache',
    '--remote-debugging-port=0','--remote-allow-origins=*',`--user-data-dir=${profile}`,
    `--window-size=${c.width},${c.height}`,url.href
  ],{stdio:'ignore'});
  child.on('error',()=>{});
  try{
    let port;
    for(let i=0;i<300;i++){
      try{port=Number((await readFile(portFile,'utf8')).split(/\r?\n/)[0]);if(port)break;}catch{}
      if(child.exitCode!==null)throw new Error(`Chromium exited with code ${child.exitCode}.`);
      await delay(100);
    }
    if(!port)throw new Error('Timed out waiting for Chromium remote debugging port.');
    const target=await getPageTarget(port);const cdp=new CDP(target.webSocketDebuggerUrl);
    await cdp.ready;
    await cdp.send('Page.enable');await cdp.send('Runtime.enable');
    await cdp.send('Emulation.setDeviceMetricsOverride',{width:c.width,height:c.height,deviceScaleFactor:1,mobile:false});
    await cdp.send('Runtime.evaluate',{expression:'document.fonts.ready.then(()=>true)',awaitPromise:true,returnByValue:true});
    await waitForRenderer(cdp);
    return {cdp,child,profile};
  }catch(error){
    child.kill();await rm(profile,{recursive:true,force:true});throw error;
  }
}
async function waitForRenderer(cdp){
  for(let i=0;i<100;i++){
    const result=await cdp.send('Runtime.evaluate',{expression:'typeof window.renderFrame === "function"',returnByValue:true});
    if(result.result?.value===true)return;
    await delay(50);
  }
  throw new Error('showreel.html did not expose renderFrame(t).');
}
async function capture(cdp,t){
  const expression=`window.renderFrame(${Number(t).toFixed(8)}); true`;
  const evaluated=await cdp.send('Runtime.evaluate',{expression,returnByValue:true});
  if(evaluated.exceptionDetails){
    const details=evaluated.exceptionDetails.exception?.description||evaluated.exceptionDetails.text;
    throw new Error(`Frame ${t}s: ${details}`);
  }
  const shot=await cdp.send('Page.captureScreenshot',{format:'png',fromSurface:true,captureBeyondViewport:false});
  return Buffer.from(shot.data,'base64');
}
async function closeChromium(cdp,child,profile){
  cdp.close();
  if(child.exitCode===null&&!child.killed)child.kill();
  await Promise.race([new Promise(resolve=>child.once('close',resolve)),delay(2000)]);
  await rm(profile,{recursive:true,force:true,maxRetries:8,retryDelay:250}).catch(()=>{});
}
function writeChunk(stream,buffer){
  return new Promise((resolve,reject)=>{
    const onError=e=>{cleanup();reject(e);};
    const onDrain=()=>{cleanup();resolve();};
    const cleanup=()=>{stream.off('error',onError);stream.off('drain',onDrain);};
    stream.once('error',onError);
    if(stream.write(buffer)){cleanup();resolve();}else stream.once('drain',onDrain);
  });
}
function runProcess(command,args,options={}){
  return new Promise((resolve,reject)=>{
    const child=spawn(command,args,{stdio:options.stdio||'inherit',cwd:options.cwd||ROOT});
    let stdout='',stderr='';
    if(child.stdout)child.stdout.on('data',d=>stdout+=d.toString());
    if(child.stderr)child.stderr.on('data',d=>stderr+=d.toString());
    child.on('error',reject);child.on('close',code=>code===0?resolve(stdout.trim().split(/\r?\n/)[0]||null):reject(new Error(`${basename(command)} exited ${code}.\n${stderr.slice(-4000)}`)));
  });
}
async function renderVideo(c){
  const ffmpeg=await findFfmpeg();const output=resolve(ROOT,argValue('output','output/course-selection-showreel.mp4'));
  await mkdir(dirname(output),{recursive:true});
  const temp=await mkdtemp(join(tmpdir(),'course-reel-'));const wav=join(temp,'score.wav');
  await writeWav(wav,{duration:c.duration});
  const {cdp,child,profile}=await openRenderer(c);
  const encoder=spawn(ffmpeg,['-y','-loglevel','error','-f','image2pipe','-vcodec','png','-framerate',String(c.fps),'-i','pipe:0','-i',wav,
    '-map','0:v:0','-map','1:a:0','-c:v','libx264','-preset','medium','-crf','17','-pix_fmt','yuv420p',
    '-c:a','aac','-b:a','192k','-t',String(c.duration),'-shortest','-movflags','+faststart',output],{stdio:['pipe','ignore','pipe']});
  let ffmpegError='';encoder.stderr.on('data',d=>ffmpegError+=d.toString());
  const encoderClosed=new Promise((resolve,reject)=>{encoder.on('error',reject);encoder.on('close',code=>code===0?resolve():reject(new Error(`FFmpeg exited ${code}: ${ffmpegError.slice(-3000)}`)));});
  try{
    const total=Math.ceil(c.duration*c.fps);
    for(let frame=0;frame<total;frame++){
      const t=frame/c.fps;const png=await capture(cdp,t);await writeChunk(encoder.stdin,png);
      if(frame===0||frame%120===119||frame===total-1)console.log(`Rendered ${frame+1}/${total} frames (${t.toFixed(2)}s)`);
    }
    encoder.stdin.end();await encoderClosed;
    console.log(`\nVideo ready: ${output}`);
  }catch(error){encoder.kill();encoder.stdin.destroy();throw error;}
  finally{await closeChromium(cdp,child,profile);await rm(temp,{recursive:true,force:true,maxRetries:8,retryDelay:250}).catch(()=>{});}
}
async function renderReview(c){
  const folder=await mkdtemp(join(tmpdir(),'course-reel-review-'));
  const contact=resolve(ROOT,argValue('output','review-contact-sheet.png'));
  const {cdp,child,profile}=await openRenderer(c);
  try{
    const files=[];
    for(let i=0;i<REVIEW_TIMES.length;i++){
      const t=REVIEW_TIMES[i],file=join(folder,`frame-${String(i).padStart(2,'0')}.png`);
      const png=await capture(cdp,t);await writeFile(file,png);files.push(file);console.log(`Review frame ${t.toFixed(1)}s`);
    }
    const determinism=await cdp.send('Runtime.evaluate',{expression:`(()=>{
      const c=document.querySelector('canvas');const g=c.getContext('2d');
      c.width=CONFIG.width;c.height=CONFIG.height;c.style.width='100vw';c.style.height='100vh';
      const fingerprint=()=>{let h=2166136261;const bytes=g.getImageData(0,0,c.width,c.height).data;for(let i=0;i<bytes.length;i+=4){h^=bytes[i];h=Math.imul(h,16777619);h^=bytes[i+1];h=Math.imul(h,16777619);h^=bytes[i+2];h=Math.imul(h,16777619);}return h>>>0;};
      renderFrame(9);const a=fingerprint();renderFrame(9);const b=fingerprint();return {a,b};
    })()`,returnByValue:true});
    if(determinism.result.value.a!==determinism.result.value.b)throw new Error('Determinism check failed: canvas pixels differ at identical timestamps.');
    console.log('Determinism check passed: repeated 9.0s canvas pixels match.');
    const payload=[];
    for(const file of files)payload.push(`data:image/png;base64,${(await readFile(file)).toString('base64')}`);
    const encoded=JSON.stringify(payload);
    await cdp.send('Runtime.evaluate',{expression:`(async()=>{
      const sources=${encoded}; const c=document.querySelector('canvas');
      c.width=2400; c.height=540; c.style.width='2400px'; c.style.height='540px';
      const g=c.getContext('2d'); g.fillStyle='#080d14'; g.fillRect(0,0,c.width,c.height);
      for(let i=0;i<sources.length;i++){
        const image=new Image(); image.src=sources[i]; await image.decode();
        const x=(i%5)*480, y=Math.floor(i/5)*270;
        g.drawImage(image,x,y,480,270);
      }
      return true;
    })()`,awaitPromise:true,returnByValue:true});
    await cdp.send('Emulation.setDeviceMetricsOverride',{width:2400,height:540,deviceScaleFactor:1,mobile:false});
    const contactPng=await cdp.send('Page.captureScreenshot',{format:'png',fromSurface:true,captureBeyondViewport:false});
    await writeFile(contact,Buffer.from(contactPng.data,'base64'));
    console.log(`Contact sheet ready: ${contact}`);
  }finally{await closeChromium(cdp,child,profile);await rm(folder,{recursive:true,force:true,maxRetries:8,retryDelay:250}).catch(()=>{});}
}
async function renderFrame(c){
  const t=Number(argValue('time','0'));if(!Number.isFinite(t)||t<0)throw new Error('--time must be a non-negative number of seconds.');
  const {cdp,child,profile}=await openRenderer(c);
  const output=resolve(ROOT,argValue('output',`output/frame-${t.toFixed(2)}s.png`));
  try{await mkdir(dirname(output),{recursive:true});await writeFile(output,await capture(cdp,t));console.log(`Frame ready: ${output}`);}
  finally{await closeChromium(cdp,child,profile);}
}
async function preview(){
  const port=Number(argValue('port',process.env.PORT||4173));
  const server=createServer(async(req,res)=>{
    try{
      const pathname=decodeURIComponent(new URL(req.url,'http://localhost').pathname);
      const relative=pathname==='/'?'/showreel.html':pathname;
      const path=resolve(ROOT,`.${relative}`);
      const body=await readFile(path);res.writeHead(200,{'Content-Type':mime[extname(path)]||'application/octet-stream','Cache-Control':'no-store'});res.end(body);
    }catch{res.writeHead(404);res.end('Not found');}
  });
  server.listen(port,'127.0.0.1',()=>console.log(`Preview: http://127.0.0.1:${port}/showreel.html`));
}

const c=config();
if(process.argv.includes('--preview'))await preview();
else if(process.argv.includes('--review'))await renderReview(c);
else if(process.argv.includes('--frame'))await renderFrame(c);
else await renderVideo(c);
