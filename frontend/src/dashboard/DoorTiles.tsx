import type { ReactNode } from 'react'
import { Link } from 'react-router'

const TILE =
  'flex flex-col gap-2.5 rounded-2xl p-4 motion-safe:transition-transform motion-safe:duration-75 motion-safe:active:translate-y-1 md:gap-2 md:rounded-3xl md:p-5 xl:flex-1'

/** L'écusson de « S'entraîner » : un éclair, sur le bleu de la porte. */
function PracticeBadge() {
  return (
    <svg aria-hidden="true" viewBox="0 0 52 52" className="size-13 shrink-0">
      <rect width="52" height="52" rx="16" className="fill-info" />
      <path d="M29 8L13 29h11l-2 15 17-22H28z" className="fill-on-badge-practice" />
    </svg>
  )
}

/** L'écusson de « Jouer » : une coupe. */
function PlayBadge() {
  return (
    <svg aria-hidden="true" viewBox="0 0 52 52" className="size-13 shrink-0">
      <rect width="52" height="52" rx="16" className="fill-badge-play" />
      <path d="M18 12h16v9a8 8 0 0 1-16 0z" className="fill-on-badge-play" />
      <path
        d="M18 15h-5v3a5 5 0 0 0 5 5M34 15h5v3a5 5 0 0 1-5 5"
        strokeWidth="3"
        className="fill-none stroke-on-badge-play"
      />
      <path
        d="M26 29v6M19 39h14"
        strokeWidth="4"
        strokeLinecap="round"
        className="stroke-on-badge-play"
      />
    </svg>
  )
}

function DoorTile({
  to,
  className,
  badge,
  title,
  text,
}: {
  to: string
  className: string
  badge: ReactNode
  title: string
  text: string
}) {
  return (
    <Link to={to} className={`${TILE} ${className}`}>
      {badge}
      <span className="text-[1.1875rem] font-black tracking-[-0.0125rem] md:text-[1.375rem]">
        {title}
      </span>{' '}
      <span className="text-[0.84375rem] leading-[1.35] font-bold text-ink-soft md:text-[0.90625rem]">
        {text}
      </span>
    </Link>
  )
}

/**
 * Les deux autres portes, sous la carte dominante de l'accueil (ADR 0077) : « S'entraîner » en bleu, « Jouer » en
 * jaune. Leur texte dit ce qu'on y trouve aujourd'hui : la révision intensive et les parties à code arrivent plus tard.
 */
export function DoorTiles() {
  return (
    <div className="grid grid-cols-2 gap-3.5 md:gap-5 xl:flex xl:flex-[1_1_0%] xl:flex-col">
      <DoorTile
        to="/practice"
        className="bg-info-soft shadow-tile-practice"
        badge={<PracticeBadge />}
        title="S’entraîner"
        text="Un examen blanc, et tes erreurs à refaire"
      />
      <DoorTile
        to="/play"
        className="bg-sun-soft shadow-tile-play"
        badge={<PlayBadge />}
        title="Jouer"
        text="Un chrono, ton équipe, un classement"
      />
    </div>
  )
}
