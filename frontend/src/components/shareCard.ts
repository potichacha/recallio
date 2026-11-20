/**
 * Une carte de partage (ADR 0045) : une image au format des aperçus de liens (1200 × 630), dessinée dans le navigateur.
 * Rien ne part vers un serveur tiers : l'image est partagée par le téléphone, ou téléchargée.
 *
 * @param kicker la petite ligne du haut : d'où vient l'exploit (« Carte des compétences », « Mode live »)
 * @param title l'exploit lui-même : « Chapitre maîtrisé », « 2e place »
 * @param detail le sujet ou le chapitre
 * @param url l'adresse à ouvrir pour en savoir plus
 * @param text le texte partagé, quand il doit dire autre chose que l'exploit : le défi du jour invite à jouer
 * @param marks des marques à droite de l'exploit, une par question : réussie ou non, sans dire la réponse (ADR 0067)
 */
export type ShareCard = {
  kicker: string
  title: string
  detail: string
  url: string
  text?: string
  marks?: boolean[]
}

export const SHARE_WIDTH = 1200
export const SHARE_HEIGHT = 630

const MARGIN = 80
const BRAND = 'Recallio'

/** Le texte qui accompagne l'image, ou la remplace quand le navigateur ne partage pas de fichier. */
export function shareText(card: ShareCard): string {
  return card.text ?? `${card.title} : ${card.detail}. ${card.kicker} sur ${BRAND}.`
}

/** Les couleurs du design system « Coach » (ADR 0049) : une image ne lit pas les variables CSS. */
const COACH = {
  brand: '#5b3df5',
  sun: '#ffc531',
  soft: '#ede9ff',
  line: '#c4b8fb',
  white: '#ffffff',
}
const FONT = '"Lexend Variable", system-ui, sans-serif'

/** La ligne de l'exploit : son haut, et la hauteur de sa police. */
const TITLE_TOP = 270
const TITLE_SIZE = 88

/** Les marques des questions : leur diamètre, l'espace entre deux, l'épaisseur de leur trait. */
const MARK_SIZE = 72
const MARK_GAP = 20
const MARK_LINE = 8

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

  // Sans marques, l'exploit a toute la largeur ; avec, il s'arrête avant elles.
  const titleRight = card.marks
    ? drawMarks(context, card.marks) - 2 * MARK_GAP
    : SHARE_WIDTH - MARGIN
  context.fillStyle = COACH.white
  context.font = `bold ${TITLE_SIZE}px ${FONT}`
  context.fillText(card.title, MARGIN, TITLE_TOP, titleRight - MARGIN)

  context.font = `48px ${FONT}`
  context.fillStyle = COACH.soft
  context.fillText(card.detail, MARGIN, 410, SHARE_WIDTH - 2 * MARGIN)

  context.font = `32px ${FONT}`
  context.fillStyle = COACH.line
  context.fillText(new URL(card.url).host, MARGIN, SHARE_HEIGHT - MARGIN - 32)
}

/**
 * Les marques des questions, alignées à droite sur la ligne de l'exploit. Une réussite est un rond plein marqué d'une
 * coche, un échec un rond vide barré d'une croix : la forme dit le résultat, pas seulement la couleur. Rend le bord
 * gauche des marques, que l'exploit ne doit pas dépasser.
 */
function drawMarks(context: CanvasRenderingContext2D, marks: boolean[]): number {
  const width = marks.length * MARK_SIZE + Math.max(0, marks.length - 1) * MARK_GAP
  const left = SHARE_WIDTH - MARGIN - width
  const radius = MARK_SIZE / 2
  const centreY = TITLE_TOP + TITLE_SIZE / 2
  context.lineWidth = MARK_LINE
  context.lineCap = 'round'
  context.lineJoin = 'round'
  marks.forEach((right, index) => {
    const centreX = left + index * (MARK_SIZE + MARK_GAP) + radius
    context.beginPath()
    if (right) {
      context.arc(centreX, centreY, radius, 0, 2 * Math.PI)
      context.fillStyle = COACH.sun
      context.fill()
      context.beginPath()
      context.moveTo(centreX - radius * 0.45, centreY)
      context.lineTo(centreX - radius * 0.1, centreY + radius * 0.35)
      context.lineTo(centreX + radius * 0.45, centreY - radius * 0.35)
      context.strokeStyle = COACH.brand
      context.stroke()
    } else {
      context.arc(centreX, centreY, radius - MARK_LINE / 2, 0, 2 * Math.PI)
      context.strokeStyle = COACH.line
      context.stroke()
      context.beginPath()
      context.moveTo(centreX - radius * 0.35, centreY - radius * 0.35)
      context.lineTo(centreX + radius * 0.35, centreY + radius * 0.35)
      context.moveTo(centreX + radius * 0.35, centreY - radius * 0.35)
      context.lineTo(centreX - radius * 0.35, centreY + radius * 0.35)
      context.stroke()
    }
  })
  return left
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
