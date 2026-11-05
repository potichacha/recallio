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

/** Les couleurs du design system « Coach » (ADR 0049) : une image ne lit pas les variables CSS. */
const COACH = {
  brand: '#0e6b63',
  sun: '#ffc83d',
  soft: '#ddf1ee',
  line: '#a8d8d1',
  white: '#ffffff',
}
const FONT = '"Lexend Variable", system-ui, sans-serif'

/** Dessine la carte : un fond sarcelle, le nom de l'application, puis l'exploit, en gros, souligné de jaune. */
export function drawShareCard(context: CanvasRenderingContext2D, card: ShareCard) {
  context.fillStyle = COACH.brand
  context.fillRect(0, 0, SHARE_WIDTH, SHARE_HEIGHT)
  context.fillStyle = COACH.sun
  context.fillRect(MARGIN, 380, 120, 10)

  context.fillStyle = COACH.white
  context.textBaseline = 'top'
  context.font = `bold 40px ${FONT}`
  context.fillText(BRAND, MARGIN, MARGIN)

  context.font = `500 36px ${FONT}`
  context.fillStyle = COACH.sun
  context.fillText(card.kicker, MARGIN, 220)

  context.fillStyle = COACH.white
  context.font = `bold 88px ${FONT}`
  context.fillText(card.title, MARGIN, 270, SHARE_WIDTH - 2 * MARGIN)

  context.font = `48px ${FONT}`
  context.fillStyle = COACH.soft
  context.fillText(card.detail, MARGIN, 410, SHARE_WIDTH - 2 * MARGIN)

  context.font = `32px ${FONT}`
  context.fillStyle = COACH.line
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
