// Le budget de la PWA (ADR 0050) : le JavaScript chargé à l'ouverture de l'application, compressé comme nginx le
// sert (gzip, niveau 6). Les morceaux chargés à la demande (l'éditeur de code) n'en font pas partie. À lancer après
// `npm run build` ; échoue si le budget est dépassé, pour qu'un ajout trop lourd se voie dans la pull request.
import { readFileSync } from 'node:fs'
import { gzipSync } from 'node:zlib'

// 285 Ko en novembre 2026 : la marge laisse grandir l'application, pas doubler une bibliothèque.
const BUDGET_KB = 300
const GZIP_LEVEL = 6

const html = readFileSync('dist/index.html', 'utf8')
const scripts = [
  ...html.matchAll(/<script[^>]+src="\/([^"]+\.js)"/g),
  ...html.matchAll(/<link[^>]+rel="modulepreload"[^>]+href="\/([^"]+\.js)"/g),
].map((match) => match[1])

const sizes = scripts.map((file) => ({
  file,
  kb: gzipSync(readFileSync(`dist/${file}`), { level: GZIP_LEVEL }).length / 1024,
}))
const total = sizes.reduce((sum, { kb }) => sum + kb, 0)

for (const { file, kb } of sizes) console.log(`${kb.toFixed(1).padStart(7)} Ko  ${file}`)
console.log(`JavaScript à l'ouverture : ${total.toFixed(1)} Ko compressés, budget ${BUDGET_KB} Ko.`)
if (scripts.length === 0 || total > BUDGET_KB) {
  console.error(
    scripts.length === 0
      ? 'Aucun script trouvé dans dist/index.html : lancer `npm run build` avant.'
      : 'Budget dépassé : charger à la demande ce qui ne sert pas à l’ouverture (ADR 0050).',
  )
  process.exit(1)
}
