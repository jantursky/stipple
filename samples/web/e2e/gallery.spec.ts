import { readFileSync } from 'node:fs'

import { expect, test, type Page } from '@playwright/test'
import sharp from 'sharp'

// Skiko warns about APIs it uses itself, and WebKit reports a WebGL extension query as an error. Nothing else may
// reach the console as an error.
const KNOWN_ERRORS = [
  /Accessing `memory` via `wasmExports` is deprecated/,
  /WEBGL_debug_renderer_info not enabled/,
]

const collectErrors = (page: Page) => {
  const errors: string[] = []
  page.on('pageerror', (error) => errors.push(error.message))
  page.on('console', (message) => {
    if (message.type() === 'error' && !KNOWN_ERRORS.some((known) => known.test(message.text()))) {
      errors.push(message.text())
    }
  })
  return errors
}

// Every page id of the gallery, read from the catalog so a new page is covered without touching this file.
const catalog = readFileSync(new URL('../../gallery/src/commonMain/kotlin/com/jantursky/stipple/samples/gallery/Catalog.kt', import.meta.url), 'utf8')
const PAGES = [...catalog.matchAll(/Page\(\s*"([a-z-]+)"/g)].map((match) => match[1])

// The gallery is one Compose canvas, so the page is ready when it has been created and has a size.
const ready = async (page: Page) => {
  await expect(page.locator('canvas')).toHaveCount(1, { timeout: 30_000 })
  await expect
    .poll(async () => (await page.locator('canvas').evaluate((c: HTMLCanvasElement) => c.width)) > 0, { timeout: 30_000 })
    .toBe(true)
}

// How much the pixels vary: the largest standard deviation of a channel. A blank page has none.
const variation = async (png: Buffer) => Math.max(...(await sharp(png).stats()).channels.map((channel) => channel.stdev))

// How far two screenshots are apart: the mean difference per channel, 0 to 255.
const difference = async (a: Buffer, b: Buffer) => {
  const [x, y] = await Promise.all([a, b].map((png) => sharp(png).raw().toBuffer()))
  let sum = 0
  for (let i = 0; i < x.length; i++) sum += Math.abs(x[i] - y[i])
  return sum / x.length
}

test('the catalog has pages to test', () => {
  expect(PAGES.length).toBeGreaterThan(20)
  expect(PAGES).toContain('overview')
})

test('the gallery loads without console errors', async ({ page }) => {
  const errors = collectErrors(page)
  await page.goto('/')
  await expect(page).toHaveTitle('Stipple gallery')
  await ready(page)
  expect(errors).toEqual([])
})

for (const id of PAGES) {
  test(`${id} opens from a deep link, draws something and keeps its address`, async ({ page }) => {
    const errors = collectErrors(page)
    await page.goto(`/#${id}`)
    await ready(page)
    await page.waitForTimeout(800)
    expect(await variation(await page.screenshot())).toBeGreaterThan(5)
    await expect(page).toHaveURL(new RegExp(`#${id}$`))
    expect(errors).toEqual([])
  })
}

test('an unknown address opens the overview', async ({ page }) => {
  await page.goto('/#no-such-page')
  await ready(page)
  await expect(page).toHaveURL(/#overview$/)
})

for (const path of ['/', '/#scenes', '/#gauge']) {
  test(`the page never scrolls sideways (${path})`, async ({ page }) => {
    await page.goto(path)
    await ready(page)
    await page.waitForTimeout(500)
    const overflow = await page.evaluate(() => document.documentElement.scrollWidth - window.innerWidth)
    expect(overflow).toBeLessThanOrEqual(0)
  })
}

test('examples animate by default', async ({ page }) => {
  await page.goto('/?embed=1#scenes')
  await ready(page)
  await page.waitForTimeout(800)
  const first = await page.screenshot()
  expect(await variation(first)).toBeGreaterThan(5)
  await expect.poll(async () => difference(first, await page.screenshot()), { timeout: 5_000 }).toBeGreaterThan(0.5)
})

test('animated=0 makes an example hold still', async ({ page }) => {
  await page.goto('/?embed=1&animated=0#scenes')
  await ready(page)
  await page.waitForTimeout(800)
  const first = await page.screenshot()
  await page.waitForTimeout(1_500)
  expect(await difference(first, await page.screenshot())).toBeLessThan(0.5)
})

test('reduced motion holds an animated example still', async ({ page }) => {
  await page.emulateMedia({ reducedMotion: 'reduce' })
  await page.goto('/?embed=1&animated=1#scenes')
  await ready(page)
  await page.waitForTimeout(800)
  const first = await page.screenshot()
  await page.waitForTimeout(1_500)
  expect(await difference(first, await page.screenshot())).toBeLessThan(0.5)
})

test('the page follows the system theme', async ({ page }) => {
  await page.emulateMedia({ colorScheme: 'dark' })
  await page.goto('/')
  await ready(page)
  await page.waitForTimeout(500)
  const dark = await sharp(await page.screenshot()).stats()
  await page.emulateMedia({ colorScheme: 'light' })
  await page.reload()
  await ready(page)
  await page.waitForTimeout(500)
  const light = await sharp(await page.screenshot()).stats()
  expect(light.channels[0].mean).toBeGreaterThan(dark.channels[0].mean + 100)
})
