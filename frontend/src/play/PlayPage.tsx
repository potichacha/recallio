import { CaretRight, Trophy, UsersThree, type Icon } from '@phosphor-icons/react'
import { Link } from 'react-router'
import { card } from '../components/ui/card'
import { DoorBanner } from '../components/ui/DoorBanner'
import { DailyChallengeSection } from '../daily/DailyChallengeLinks'

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

/**
 * Jouer (ADR 0077, après l'ADR 0052) : ce qui se joue contre les autres, le classé et les groupes, et le défi du jour,
 * le même pour tous (ADR 0067). Les parties à code arrivent à l'étape 7 de la refonte.
 */
export function PlayPage() {
  return (
    <section className="flex flex-col gap-6">
      <DoorBanner door="play" eyebrow="Jouer" title="Mesure-toi aux autres">
        <p className="max-w-xs text-[0.9375rem] leading-[1.4] font-bold">
          Le défi du jour, des duels classés, et ton équipe.
        </p>
      </DoorBanner>
      <ul className="grid gap-3 sm:grid-cols-2">
        {GAMES.map(({ to, icon: GameIcon, title, detail }) => (
          <li key={to}>
            <Link
              to={to}
              className={`flex h-full items-center gap-3 ${card()} motion-safe:transition-transform motion-safe:duration-75 motion-safe:active:translate-y-1`}
            >
              <span
                aria-hidden="true"
                className="flex size-11 shrink-0 items-center justify-center rounded-[0.875rem] bg-sun-soft text-warning"
              >
                <GameIcon weight="fill" className="size-6" />
              </span>
              <span className="flex flex-1 flex-col">
                <span className="font-black">{title}</span>{' '}
                <span className="text-[0.8125rem] leading-snug font-bold text-ink-muted">
                  {detail}
                </span>
              </span>
              <CaretRight weight="bold" aria-hidden className="size-5 shrink-0 text-ink-muted" />
            </Link>
          </li>
        ))}
      </ul>
      <DailyChallengeSection />
    </section>
  )
}
