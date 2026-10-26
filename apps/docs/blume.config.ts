import { readFileSync } from 'node:fs'

import { defineConfig } from 'blume'

const properties = readFileSync(new URL('../../gradle.properties', import.meta.url), 'utf8')
const version = /^VERSION_NAME=(.+)$/m.exec(properties)?.[1]?.trim()
if (!version) throw new Error('VERSION_NAME is missing from gradle.properties')

export default defineConfig({
  title: 'Stipple',
  description: 'Dithered UI for Compose Multiplatform: a dither engine, modifiers and ready-made components.',
  logo: '/logo.svg',
  github: {
    owner: 'jantursky',
    repo: 'stipple',
    dir: 'apps/docs',
  },
  navigation: {
    actions: [
      { href: 'https://stipple-demo.vercel.app', label: 'Demo' },
      { href: 'https://jitpack.io/#jantursky/stipple', label: 'JitPack' },
    ],
  },
  content: {
    root: 'docs',
  },
  theme: {
    accent: '#615fff',
    radius: 'lg',
  },
  agents: {
    // The repository's skill (skills/stipple/SKILL.md) replaces the one Blume would generate for the site.
    skills: '../../skills',
  },
  deployment: {
    site: 'https://stipple-docs.vercel.app',
  },
  variables: {
    version,
    demo: 'https://stipple-demo.vercel.app',
  },
})
