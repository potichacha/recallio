/**
 * Une carte de partage (ADR 0045) : une image au format des aperçus de liens (1200 × 630), dessinée dans le navigateur.
 * Rien ne part vers un serveur tiers : l'image est partagée par le téléphone, ou téléchargée.
 *
 * @param kicker la petite ligne du haut : d'où vient l'exploit (« Carte des compétences », « Mode live »)
 * @param title l'exploit lui-même : « Chapitre maîtrisé », « 2e place »
 * @param detail le sujet ou le chapitre
 * @param url l'adresse à ouvrir pour en savoir plus
 */
export type ShareCard = { kicker: string; title: string; detail: string; url: string }

export const SHARE_WIDTH = 1200
export const SHARE_HEIGHT = 630

const MARGIN = 80
const BRAND = 'Recallio'

/** Le texte qui accompagne l'image, ou la remplace quand le navigateur ne partage pas de fichier. */
export function shareText(card: ShareCard): string {
  return `${card.title} : ${card.detail}. ${card.kicker} sur ${BRAND}.`
}

/** Dessine la carte : un fond indigo, le nom de l'application, puis l'exploit, en gros. */
export function drawShareCard(context: CanvasRenderingContext2D, card: ShareCard) {
  const background = context.createLinearGradient(0, 0, SHARE_WIDTH, SHARE_HEIGHT)
  background.addColorStop(0, '#4f46e5')
  background.addColorStop(1, '#7c3aed')
  context.fillStyle = background
  context.fillRect(0, 0, SHARE_WIDTH, SHARE_HEIGHT)

  context.fillStyle = '#ffffff'
  context.textBaseline = 'top'
  context.font = 'bold 40px system-ui, sans-serif'
  context.fillText(BRAND, MARGIN, MARGIN)

  context.font = '600 36px system-ui, sans-serif'
  context.fillStyle = '#c7d2fe'
  context.fillText(card.kicker.toUpperCase(), MARGIN, 220)

  context.fillStyle = '#ffffff'
  context.font = 'bold 88px system-ui, sans-serif'
  context.fillText(card.title, MARGIN, 280, SHARE_WIDTH - 2 * MARGIN)

  context.font = '48px system-ui, sans-serif'
  context.fillText(card.detail, MARGIN, 400, SHARE_WIDTH - 2 * MARGIN)

  context.font = '32px system-ui, sans-serif'
  context.fillStyle = '#e0e7ff'
  context.fillText(new URL(card.url).host, MARGIN, SHARE_HEIGHT - MARGIN - 32)
}

/** L'image de la carte en PNG ; `null` si le navigateur ne sait pas dessiner (un environnement de test, par exemple). */
export function shareCardImage(card: ShareCard): Promise<Blob | null> {
  const canvas = document.createElement('canvas')
  canvas.width = SHARE_WIDTH
  canvas.height = SHARE_HEIGHT
  const context = canvas.getContext('2d')
  if (!context) return Promise.resolve(null)
  drawShareCard(context, card)
  return new Promise((resolve) => canvas.toBlob(resolve, 'image/png'))
}
