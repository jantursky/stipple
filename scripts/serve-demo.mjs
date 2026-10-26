// Serves the built web gallery (./gradlew :samples:web:wasmJsBrowserDistribution) for the browser tests
// and the screenshots. The gallery is one static page, so a tiny file server is enough.
// Run on its own: node scripts/serve-demo.mjs [port]

import { readFileSync, statSync } from 'node:fs'
import { createServer } from 'node:http'
import { extname, join, normalize } from 'node:path'
import { fileURLToPath } from 'node:url'

const root = fileURLToPath(new URL('..', import.meta.url))
export const dist = join(root, 'samples/web/build/dist/wasmJs/productionExecutable')

const TYPES = {
  '.html': 'text/html',
  '.js': 'text/javascript',
  '.mjs': 'text/javascript',
  '.css': 'text/css',
  '.svg': 'image/svg+xml',
  '.png': 'image/png',
  '.wasm': 'application/wasm',
  '.ttf': 'font/ttf',
  '.json': 'application/json',
  '.map': 'application/json',
}

export const serve = (port = 0) => {
  if (!statSync(join(dist, 'index.html'), { throwIfNoEntry: false })) {
    throw new Error(`No gallery build in ${dist}. Run ./gradlew :samples:web:wasmJsBrowserDistribution first.`)
  }
  return new Promise((resolve) => {
    const server = createServer((request, response) => {
      const path = normalize(decodeURIComponent(new URL(request.url ?? '/', 'http://x').pathname))
      let file = join(dist, path)
      if (!file.startsWith(dist) || !statSync(file, { throwIfNoEntry: false })?.isFile()) {
        file = join(dist, 'index.html')
      }
      response.writeHead(200, { 'content-type': TYPES[extname(file)] ?? 'application/octet-stream' })
      response.end(readFileSync(file))
    })
    server.listen(port, '127.0.0.1', () => resolve(server))
  })
}

if (process.argv[1] === fileURLToPath(import.meta.url)) {
  const server = await serve(Number(process.argv[2] ?? 4173))
  console.log(`Gallery on http://127.0.0.1:${server.address().port}/`)
}
