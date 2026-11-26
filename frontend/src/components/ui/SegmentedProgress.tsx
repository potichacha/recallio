/** Les couleurs des segments : celles d'une carte, ou celles de la carte dominante de l'accueil, jaune sur sa piste. */
const TONES = {
  default: { filled: 'bg-brand', empty: 'bg-surface-sunken' },
  hero: { filled: 'bg-sun', empty: 'bg-hero-track' },
}

/**
 * La progression en segments des maquettes « trois portes » (ADR 0077) : une leçon de huit questions, dont trois sont
 * faites. C'est une image : son nom (« Question 3 sur 8 ») dit tout, les segments ne se lisent pas un à un.
 *
 * @param total le nombre de segments
 * @param filled le nombre de segments remplis, borné entre zéro et `total`
 * @param label le nom de l'image pour un lecteur d'écran, obligatoire
 * @param tone `hero` quand elle est posée sur la carte dominante de l'accueil
 */
export function SegmentedProgress({
  total,
  filled,
  label,
  tone = 'default',
}: {
  total: number
  filled: number
  label: string
  tone?: keyof typeof TONES
}) {
  const done = Math.min(Math.max(filled, 0), total)
  const colors = TONES[tone]
  return (
    <div
      role="img"
      aria-label={label}
      className="grid flex-1 gap-1"
      style={{ gridTemplateColumns: `repeat(${total}, minmax(0, 1fr))` }}
    >
      {Array.from({ length: total }, (_, index) => (
        <span
          key={index}
          className={`h-3 rounded-full ${index < done ? colors.filled : colors.empty}`}
        />
      ))}
    </div>
  )
}
