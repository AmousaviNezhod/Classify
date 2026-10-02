# Course Selection — product-motion reel

A self-contained, deterministic **18-second / 60 fps / 1920×1080** product-motion showcase for a Persian university course-selection app. The dedicated `course-selection-showreel/` folder is isolated from the app; no application files are modified. It uses no third-party npm packages, downloaded media, font files, network APIs, or framework. The active workspace contained no app source files to inspect, so the reel uses the requested product language with believable mock course data and a new dark, mint-accented visual identity.

## Storyboard

1. **Identity (0–2.1s):** a deterministic particle field resolves into a course-selection mark and Persian title.
2. **Discovery (2–5s):** RTL course cards appear, then responsive-looking filters reduce the displayed course list.
3. **Selection (5–7.5s):** courses get selected with check indicators and the credit total smoothly advances from 12 to 19.
4. **Validation (7.5–10s):** a weekly timetable shows an overlap, then moves an alternative course group to an open slot and resolves the conflict.
5. **Generation (10–13s):** multiple timetable candidates are evaluated; the invalid option recedes and the conflict-free plan is highlighted.
6. **Final week (13–16s):** the weekly schedule settles with 17 credits, four class days, and zero conflicts.
7. **Hero close (16–18s):** a clean title card resolves to “ترمت را هوشمند بچین” and remains on-screen through the ending.

`window.renderFrame(t)` in [showreel.html](showreel.html) draws each visual frame entirely from the supplied timestamp. Timelines, easing, layout, counters, deterministic particles, and scene transitions do not depend on render order. The browser-side review command checks that two renders at 9.0 seconds have identical canvas pixels. The generated audio is produced by [audio.mjs](audio.mjs), using an absolute-time, seeded score (120 BPM, kick, hats, clap, bass, pad, plucks, swells, and impacts).

## Requirements

- Node.js 22+ (built-in WebSocket; **no npm install needed**)
- Chromium or Chrome
- FFmpeg with `libx264` and AAC support for MP4 encoding

If automatic executable lookup cannot find a browser/encoder, configure `CHROME_BIN` and/or `FFMPEG_BIN`. The renderer streams PNG captures directly to FFmpeg (no 1,080-frame PNG sequence).

## Commands

From this directory:

```bash
npm run preview
npm run review
npm run frame -- --time=9.0 --output=output/frame-9.png
npm run render
```

- **Preview:** open `http://127.0.0.1:4173/showreel.html`; the `?t=8.5` query selects an initial frame. Call `window.renderFrame(8.5)` in DevTools to scrub.
- **Review:** writes `review-contact-sheet.png` with ten representative frames at 480×270 each (0, 1.5, 3, 5, 7, 9, 11, 13, 15, 17s), then runs the repeated-timestamp pixel check. It uses Chromium only.
- **Single frame:** writes a PNG for a chosen timestamp. `--time=…` and `--output=…` are supported.
- **Final MP4:** `npm run render` writes `output/course-selection-showreel.mp4`, combining the streamed image frames with the procedurally generated WAV. The temporary WAV is deleted after rendering.

Renderer options can also be set with CLI flags or environment variables:

```bash
npm run render -- --width=1920 --height=1080 --fps=60 --duration=18
```

**Note:** the scenes are art-directed for an 18-second duration. Changing `--duration` changes the capture/audio length but does not retime the authored scene cuts.

## Audio on its own

```bash
node audio.mjs --duration=18 --output=output/showreel-audio.wav
```

No commercial music or downloaded sound effects are used.
