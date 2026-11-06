import { Trophy, UsersThree, type Icon } from '@phosphor-icons/react'
import { Link } from 'react-router'

const GAMES: { to: string; icon: Icon; title: string; detail: string }[] = [
  {
    to: '/ranked',
    icon: Trophy,
    title: 'Classé',
    detail: 'Des duels sur un sujet, et un rang qui monte à chaque victoire.',
  },
  {
    to: '/groups',
    icon: UsersThree,
    title: 'Groupes',
    detail: 'Le classement de la semaine, les défis et les parties live avec ton équipe.',
  },
]

/** Jouer (ADR 0052) : ce qui se joue contre les autres, le classé et les groupes, réunis sous une seule entrée. */
export function PlayPage() {
  return (
    <section className="flex flex-col gap-6">
      <header className="flex flex-col gap-1">
        <h1 className="text-2xl font-bold">Jouer</h1>
        <p className="text-ink-muted">Mesure-toi aux autres, en duel ou en équipe.</p>
      </header>
      <ul className="grid gap-3 sm:grid-cols-2">
        {GAMES.map(({ to, icon: Icon, title, detail }) => (
          <li key={to}>
            <Link
              to={to}
              className="flex h-full items-center gap-4 rounded-xl border border-line bg-surface p-4 shadow-sm hover:border-brand-line"
            >
              <Icon weight="duotone" aria-hidden className="size-10 shrink-0 text-brand" />
              <span className="flex flex-col gap-0.5">
                <span className="text-lg font-semibold">{title}</span>{' '}
                <span className="text-sm text-ink-muted">{detail}</span>
              </span>
            </Link>
          </li>
        ))}
      </ul>
    </section>
  )
}
