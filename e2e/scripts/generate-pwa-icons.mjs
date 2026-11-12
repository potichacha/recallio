// Les icônes de la PWA (audit de novembre 2026, élément 9), dessinées depuis frontend/public/favicon.svg : Android et
// iOS n'installent pas une icône SVG. Les PNG sont versionnés ; ce script ne sert qu'à les refaire quand l'icône change
// (la refonte « Rico »). Il vit ici parce qu'il dessine avec le Chromium des parcours, sans dépendance de plus.
//
//   cd e2e && node scripts/generate-pwa-icons.mjs
import { readFile } from 'node:fs/promises'
import { fileURLToPath } from 'node:url'
import { chromium } from '@playwright/test'

const PUBLIC = new URL('../../frontend/public/', import.meta.url)

const rounded = await readFile(new URL('favicon.svg', PUBLIC), 'utf8')
// Une icône « maskable » ou Apple remplit tout le carré : le système la découpe lui-même. Le dessin tient dans la zone
// sûre, le cercle central de 80 %.
const square = rounded.replace('rx="112"', 'rx="0"')
if (square === rounded) throw new Error('favicon.svg : le fond arrondi (rx="112") est introuvable')

const ICONS = [
  { file: 'icon-192.png', size: 192, svg: rounded },
  { file: 'icon-512.png', size: 512, svg: rounded },
  { file: 'icon-maskable-512.png', size: 512, svg: square },
  { file: 'apple-touch-icon.png', size: 180, svg: square },
]

const browser = await chromium.launch()
for (const { file, size, svg } of ICONS) {
  const page = await browser.newPage({ viewport: { width: size, height: size } })
  await page.setContent(
    `<style>html, body { margin: 0; background: transparent } svg { display: block; width: ${size}px; height: ${size}px }</style>${svg}`,
  )
  await page.screenshot({ path: fileURLToPath(new URL(file, PUBLIC)), omitBackground: true })
  await page.close()
  console.log(`${file} : ${size} px`)
}
await browser.close()
