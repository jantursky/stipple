// Fails when a page of the built docs site links to a path the build did not produce.
// Run after `bun run build:docs`: node scripts/check-links.mjs
//  - looks at href and src attributes in dist/**/*.html that point inside the site (absolute path or relative)
//  - a path is found when dist has the file, a folder with an index.html, or a .html file of that name
//  - anchors, queries and links to other sites are ignored
import { existsSync, readdirSync, readFileSync, statSync } from 'node:fs'
import { dirname, join, relative, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'

const dist = fileURLToPath(new URL('../dist/', import.meta.url))
if (!existsSync(dist)) {
  console.error(`check-links: ${dist} is missing. Run the docs build first.`)
  process.exit(1)
}

const pages = []
const walk = (dir) => {
  for (const name of readdirSync(dir)) {
    const path = join(dir, name)
    if (statSync(path).isDirectory()) walk(path)
    else if (name.endsWith('.html')) pages.push(path)
  }
}
walk(dist)

const exists = (path) =>
  path.startsWith(dist) &&
  ((existsSync(path) && statSync(path).isFile()) || existsSync(join(path, 'index.html')) || existsSync(`${path}.html`))

const broken = new Map()
for (const page of pages) {
  const html = readFileSync(page, 'utf8')
  for (const match of html.matchAll(/\s(?:href|src)="([^"]+)"/g)) {
    const target = match[1]
    if (/^([a-z][a-z0-9+.-]*:|\/\/|#)/i.test(target)) continue
    const path = decodeURIComponent(target.split('#')[0].split('?')[0])
    if (path === '') continue
    const resolved = path.startsWith('/') ? join(dist, path) : resolve(dirname(page), path)
    if (!exists(resolved)) {
      const from = relative(dist, page)
      broken.set(`${from} -> ${target}`, true)
    }
  }
}

if (broken.size > 0) {
  console.error(`check-links: ${broken.size} broken link${broken.size === 1 ? '' : 's'}:`)
  for (const entry of broken.keys()) console.error(`  ${entry}`)
  process.exit(1)
}
console.log(`check-links: ${pages.length} pages, no broken links`)
