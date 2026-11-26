import type { ReactNode } from 'react'
import type { Door } from './doors'
import { eyebrow as eyebrowRecipe, heading } from './typography'

/** Le fond d'une porte, son texte et son bord épais ; puis la couleur de son surtitre. */
const DOORS: Record<Door, { banner: string; eyebrow: string }> = {
  learn: { banner: 'bg-brand text-on-brand shadow-door-learn', eyebrow: 'text-brand-soft' },
  practice: { banner: 'bg-info text-on-info shadow-door-practice', eyebrow: 'text-info-soft' },
  play: { banner: 'bg-sun text-on-sun shadow-door-play', eyebrow: 'text-on-sun' },
}

/**
 * Le bandeau en tête d'une porte (ADR 0077) : un fond à sa couleur, aux coins bas arrondis, avec un surtitre, le titre
 * de l'écran et une illustration qui dépasse en haut à droite.
 *
 * @param illustration un dessin décoratif, caché aux lecteurs d'écran ; le texte passe devant
 * @param children ce qui suit le titre : une phrase, un lien
 */
export function DoorBanner({
  door,
  eyebrow,
  title,
  illustration,
  children,
}: {
  door: Door
  eyebrow: string
  title: string
  illustration?: ReactNode
  children?: ReactNode
}) {
  const colors = DOORS[door]
  return (
    <header className={`relative overflow-hidden rounded-b-3xl px-5 pt-6 pb-5.5 ${colors.banner}`}>
      {illustration && (
        <div aria-hidden="true" className="absolute -top-5 -right-6">
          {illustration}
        </div>
      )}
      <p className={`relative ${eyebrowRecipe()} ${colors.eyebrow}`}>{eyebrow}</p>
      <h1 className={`relative mt-1.5 max-w-60 ${heading()}`}>{title}</h1>
      {children && <div className="relative mt-2">{children}</div>}
    </header>
  )
}
