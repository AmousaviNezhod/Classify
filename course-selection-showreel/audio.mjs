import { writeFile } from 'node:fs/promises';
import { fileURLToPath } from 'node:url';

export const AUDIO_CONFIG = Object.freeze({ sampleRate: 48000, bpm: 120 });
const TAU = Math.PI * 2;
const clamp = (x, a=0, b=1) => Math.max(a, Math.min(b, x));
const fract = x => x - Math.floor(x);
const hash = n => fract(Math.sin(n * 127.1 + 311.7) * 43758.5453123);
const midi = n => 440 * Math.pow(2, (n - 69) / 12);

function envelope(t, attack, decay, sustain, release, duration) {
  if (t < 0 || t > duration) return 0;
  if (t < attack) return t / attack;
  if (t < attack + decay) return 1 - (1 - sustain) * ((t - attack) / decay);
  if (t < duration - release) return sustain;
  return sustain * clamp((duration - t) / release);
}

/** Deterministic 48 kHz stereo WAV; each sample is a pure function of absolute time. */
export function makeWav({ duration=18, sampleRate=AUDIO_CONFIG.sampleRate, bpm=AUDIO_CONFIG.bpm }={}) {
  const frames = Math.ceil(duration * sampleRate);
  const dataBytes = frames * 4;
  const wav = Buffer.alloc(44 + dataBytes);
  wav.write('RIFF', 0); wav.writeUInt32LE(36 + dataBytes, 4); wav.write('WAVE', 8);
  wav.write('fmt ', 12); wav.writeUInt32LE(16, 16); wav.writeUInt16LE(1, 20);
  wav.writeUInt16LE(2, 22); wav.writeUInt32LE(sampleRate, 24);
  wav.writeUInt32LE(sampleRate * 4, 28); wav.writeUInt16LE(4, 32);
  wav.writeUInt16LE(16, 34); wav.write('data', 36); wav.writeUInt32LE(dataBytes, 40);

  const beat = 60 / bpm;
  const accents = [2, 5, 7.5, 10, 13, 16];
  const notes = [43, 43, 50, 48, 46, 46, 53, 50, 41, 41, 48, 46, 43, 43, 50, 48];
  for (let i=0; i<frames; i++) {
    const t=i/sampleRate;
    const beatPos=t/beat;
    const beatNo=Math.floor(beatPos+1e-9);
    const eighthPos=t/(beat/2);
    const eighthPhase=eighthPos-Math.floor(eighthPos);
    let left=0,right=0;

    // Soft four-on-the-floor kick: a quick downward pitch sweep and a short, rounded body.
    const kickPhase=t%beat;
    const kickEnv=Math.exp(-kickPhase*19);
    const kickFreq=48+48*Math.exp(-kickPhase*35);
    const kick=Math.sin(TAU*kickFreq*kickPhase)*kickEnv*.24;
    left+=kick; right+=kick;

    // Warm clap on beats 2 and 4, with a small deterministic filtered-noise tail.
    if (beatNo%4===1 || beatNo%4===3) {
      const clapT=t-(beatNo%4===1?beat:beat*3);
      const clapEnv=Math.exp(-clapT*31);
      const noise=(hash(i+beatNo*7919)-0.5)*2;
      const clap=(Math.sin(TAU*188*clapT)*.42+noise*.22)*clapEnv*.12;
      left+=clap; right+=clap*.92;
    }

    // Ticking, alternating eighth-note hats with a restrained noise texture.
    const hatNoise=(hash(i+773)-.5)*2;
    const hatAccent=eighthPhase<.07?Math.exp(-eighthPhase*62):0;
    const hat=(hatNoise*.024+Math.sin(TAU*9100*t)*.004)*hatAccent;
    left+=hat*(beatNo%2?.78:1); right+=hat*(beatNo%2?1:.78);

    // Rounded sub bass follows a small minor-key phrase and breathes with the kick.
    const note=notes[Math.floor(t/(beat*2))%notes.length];
    const bassFreq=midi(note-12);
    const duck=1-.34*Math.exp(-kickPhase*12);
    const bassEnv=envelope((t%(beat*2)),.035,.18,.72,.2,beat*2);
    const bass=(Math.sin(TAU*bassFreq*t)+.22*Math.sin(TAU*bassFreq*2*t))*bassEnv*duck*.105;
    left+=bass;right+=bass;

    // Glassy but mellow pluck on offbeats, never pitched sharply or excessively bright.
    const pluckStart=Math.floor(beatPos*2+.5)/2*beat;
    const pluckAge=t-pluckStart;
    if(pluckAge>=0&&pluckAge<.42){
      const scale=[0,3,7,10,7,3,5,10][Math.floor(pluckStart/(beat/2))%8];
      const f=midi(67+scale);
      const env=Math.exp(-pluckAge*8.6);
      const pluck=(Math.sin(TAU*f*pluckAge)+.17*Math.sin(TAU*f*2.01*pluckAge))*env*.045;
      left+=pluck;right+=pluck*.86;
    }

    // Quiet sustained harmonic bed in scene changes, side-chained on every downbeat.
    const chordRoot=t<5?55:t<10?53:t<13?58:55;
    const chordPhase=t%(beat*4);
    const padEnv=.5+.5*Math.sin(Math.PI*clamp(chordPhase/(beat*4)));
    const padDuck=1-.42*Math.exp(-kickPhase*10);
    const pad=(Math.sin(TAU*midi(chordRoot)*t)+.48*Math.sin(TAU*midi(chordRoot+7)*t+.4)+.24*Math.sin(TAU*midi(chordRoot+12)*t+1.2))*padEnv*padDuck*.027;
    left+=pad;right+=pad*.96;

    // Low, soft transition swells peak at the scene edits; they resolve on the beat.
    let swell=0;
    for(const mark of accents){
      const dt=t-mark;
      if(dt>=-.72&&dt<.12){
        const rise=clamp((dt+.72)/.72);
        const release=dt<0?1:Math.exp(-dt*13);
        const age=Math.max(0,dt+.72);
        const phase=92*age+19*age*age;
        const white=(hash(Math.floor(phase*sampleRate)+Math.floor(mark*10))-0.5)*2;
        swell+=(Math.sin(TAU*phase)*.68+white*.32)*Math.sin(Math.PI*rise)*release*.035;
      }
    }
    left+=swell;right+=swell*.92;

    // Airy, seeded transient impacts at schedule lock-in and end-card arrival.
    for(const mark of [13,16]){
      const dt=t-mark;
      if(dt>=0&&dt<.38){
        const e=Math.exp(-dt*13);
        const transientNoise=(hash(Math.floor(dt*sampleRate)+Math.floor(mark*1000))-.5)*2;
        const hit=(Math.sin(TAU*(74-32*dt)*dt)*.65+transientNoise*.7)*e*.105;
        left+=hit;right+=hit*.9;
      }
    }

    const fadeIn=clamp(t/.42);
    const fadeOut=clamp((duration-t)/.62);
    const master=Math.min(fadeIn,fadeOut)*.83;
    const l=Math.tanh(left*master*1.65);
    const r=Math.tanh(right*master*1.65);
    const offset=44+i*4;
    wav.writeInt16LE(Math.round(clamp(l,-1,1)*32767),offset);
    wav.writeInt16LE(Math.round(clamp(r,-1,1)*32767),offset+2);
  }
  return wav;
}

export async function writeWav(path, options={}) {
  const wav=makeWav(options);
  await writeFile(path,wav);
  return { path, bytes:wav.length, seconds:options.duration??18 };
}

if (process.argv[1] && fileURLToPath(import.meta.url) === process.argv[1]) {
  const durationArg=process.argv.find(a=>a.startsWith('--duration='));
  const outArg=process.argv.find(a=>a.startsWith('--output='));
  const duration=durationArg?Number(durationArg.split('=')[1]):18;
  const path=outArg?outArg.split('=').slice(1).join('='):'showreel-audio.wav';
  await writeWav(path,{duration});
  console.log(`WAV ready: ${path} (${duration}s, deterministic procedural score)`);
}
