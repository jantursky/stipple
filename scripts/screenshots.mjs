// Builds the web gallery, serves it and records the README GIFs to .github/assets/: one per group of pages, in the
// light and the dark theme. Run it with `bun run screenshots` whenever the gallery or a look changes.
//
// The pages open in embed mode (`?embed=1&animated=1`), so the stage is the whole viewport and everything moves on
// its own. The page runs on Playwright's fake clock, so every frame is exactly FRAME_MS after the last however long a
// screenshot takes. ffmpeg (on the PATH) turns the frames into a GIF.

import { spawnSync } from 'node:child_process'
import { mkdirSync, mkdtempSync, rmSync, statSync, writeFileSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import { fileURLToPath } from 'node:url'

import { chromium } from '@playwright/test'
import sharp from 'sharp'

import { serve } from './serve-demo.mjs'

const root = fileURLToPath(new URL('..', import.meta.url))
const out = join(root, '.github/assets')
const FRAME_MS = 50
const SHOW_MS = 1800
const WIDTH = 560

/** Each GIF plays a few gallery pages one after another. `height` is the part of the page that holds the stage. */
const GIFS = [
  {
    name: 'engine',
    pages: [
      { id: 'custom-fields', height: 320 },
      { id: 'renderers', height: 400 },
      { id: 'dither-modifier', height: 340 },
    ],
  },
  {
    name: 'components',
    pages: [
      { id: 'gauge', height: 344 },
      { id: 'progress-bar', height: 300 },
      { id: 'bar-chart', height: 320 },
      { id: 'stat-card', height: 300 },
    ],
  },
  {
    name: 'backgrounds',
    pages: [
      { id: 'scenes', height: 376 },
      { id: 'background', height: 320 },
      { id: 'interface', height: 320 },
    ],
  },
]

const build = () => {
  const result = spawnSync('./gradlew', [':samples:web:wasmJsBrowserDistribution'], { cwd: root, stdio: 'inherit' })
  if (result.status !== 0) process.exit(result.status ?? 1)
}

/** Runs the fake clock until the canvas has drawn, since nothing paints while the clock stands still. */
const settle = async (page) => {
  for (let i = 0; i < 300; i++) {
    await page.clock.runFor(100)
    if (await page.locator('canvas').isVisible()) break
  }
  await page.evaluate(() => document.fonts.ready)
  await page.clock.runFor(1000)
}

const record = async (browser, url, gif, theme, dir) => {
  let frame = 0
  for (const view of gif.pages) {
    const context = await browser.newContext({
      viewport: { width: 640, height: view.height },
      deviceScaleFactor: 2,
      colorScheme: theme,
      reducedMotion: 'no-preference',
    })
    const page = await context.newPage()
    await page.clock.install()
    await page.goto(`${url}?embed=1&animated=1#${view.id}`)
    await settle(page)
    for (let i = 0; i < SHOW_MS / FRAME_MS; i++) {
      await page.clock.runFor(FRAME_MS)
      const png = await sharp(await page.screenshot())
        .resize({ width: WIDTH * 2, height: 400 * 2, fit: 'contain', background: theme === 'dark' ? '#09090b' : '#ffffff' })
        .png()
        .toBuffer()
      writeFileSync(join(dir, `${String(frame++).padStart(5, '0')}.png`), png)
    }
    await context.close()
  }

  const file = join(out, `${gif.name}-${theme}.gif`)
  const filters = `fps=${1000 / FRAME_MS},scale=${WIDTH}:-1:flags=lanczos,split[a][b];[a]palettegen=max_colors=64:stats_mode=diff[p];[b][p]paletteuse=dither=bayer:bayer_scale=4:diff_mode=rectangle`
  const ffmpeg = spawnSync(
    'ffmpeg',
    ['-y', '-loglevel', 'error', '-framerate', String(1000 / FRAME_MS), '-i', join(dir, '%05d.png'), '-vf', filters, file],
    { stdio: 'inherit' },
  )
  if (ffmpeg.status !== 0) throw new Error(`ffmpeg failed for ${file}`)
  console.log(`${file.slice(root.length)}  ${Math.round(statSync(file).size / 1024)} kB`)
}

if (!process.argv.includes('--no-build')) build()
mkdirSync(out, { recursive: true })
const server = await serve()
const url = `http://127.0.0.1:${server.address().port}/`
const browser = await chromium.launch()
try {
  for (const gif of GIFS) {
    for (const theme of ['light', 'dark']) {
      const dir = mkdtempSync(join(tmpdir(), `stipple-${gif.name}-`))
      try {
        await record(browser, url, gif, theme, dir)
      } finally {
        rmSync(dir, { recursive: true, force: true })
      }
    }
  }
} finally {
  await browser.close()
  server.close()
}
