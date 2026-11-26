import { CheckCircle, Circle, FastForward, XCircle } from '@phosphor-icons/react'
import { useState } from 'react'
import { Link } from 'react-router'
import { ApiError } from '../api/errors'
import { useCancelLevelCheck, type LevelCheck, type LevelCheckDomain } from '../api/levelChecks'
import { useTour, useTourProgress } from '../api/tour'
import { Celebration } from '../components/Celebration'
import { Rico } from '../components/Rico'
import { button } from '../components/ui/button'
import { useFocusOnAppear } from '../components/useFocusOnAppear'
import { firstStepToDiscover, tourPath } from '../tour/tourSteps'
import { notionCount, type DomainVerdict } from './levelCheckRun'

const skipped = (count: number) => `${notionCount(count)} sautée${count > 1 ? 's' : ''}`

/**
 * La fin du test de niveau (ADR 0068) : les domaines et leur verdict, les notions sautées, la suite dans le Tour, et de
 * quoi annuler le test si l'on s'est surestimé.
 *
 * @param knownNotions les notions du sujet connues après le test, celles des tests précédents comprises
 */
export function LevelCheckResult({
  subject,
  check,
  verdicts,
  knownNotions,
  onCancelled,
}: {
  subject: string
  check: LevelCheck
  verdicts: DomainVerdict[]
  knownNotions: number
  onCancelled: () => void
}) {
  const stopped = verdicts.length < check.domains.length
  const passedNow = verdicts.some((verdict) => verdict.passed)
  return (
    <section aria-labelledby="level-check-result" className="flex flex-col gap-5">
      <div className="relative flex items-center gap-4">
        {passedNow && <Celebration />}
        <Rico pose={passedNow ? 'cheering' : 'neutral'} decorative />
        <div className="flex flex-1 flex-col gap-1 rounded-2xl bg-brand p-5 text-on-brand shadow-press">
          <h2 id="level-check-result" className="text-sm font-bold uppercase">
            Test terminé
          </h2>
          <p className="text-2xl font-extrabold motion-safe:animate-pop">
            {knownNotions > 0 ? skipped(knownNotions) : 'Aucune notion sautée'}
          </p>
        </div>
      </div>
      <p className="text-ink-soft">
        {stopped
          ? 'Deux domaines ratés de suite : le test s’arrête là. Le Tour t’attend pour la suite.'
          : 'Tous les domaines des bases sont passés.'}{' '}
        Les notions sautées le sont dans le Tour, et passent après le reste en révision.
      </p>
      <ul aria-label="Les domaines des bases" className="flex flex-col gap-2">
        {check.domains.map((domain) => (
          <DomainLine
            key={domain.id}
            domain={domain}
            verdict={verdicts.find((verdict) => verdict.domain === domain.id)}
          />
        ))}
      </ul>
      <ContinueTour subject={subject} />
      <CancelLevelCheck subject={subject} title={check.subjectTitle} onCancelled={onCancelled} />
    </section>
  )
}

/** Un domaine et son verdict : une icône et un texte, jamais la couleur seule. */
function DomainLine({ domain, verdict }: { domain: LevelCheckDomain; verdict?: DomainVerdict }) {
  const { icon, text } = verdictOf(domain, verdict)
  return (
    <li className="flex items-center gap-3 rounded-xl border border-line bg-surface px-4 py-3">
      {icon}
      <span className="flex flex-col">
        <span className="font-semibold">{domain.title}</span>
        <span className="text-sm text-ink-muted">{text}</span>
      </span>
    </li>
  )
}

function verdictOf(domain: LevelCheckDomain, verdict?: DomainVerdict) {
  if (verdict?.passed) {
    return {
      icon: <CheckCircle weight="fill" aria-hidden className="size-7 shrink-0 text-success" />,
      text: `Réussi : ${skipped(domain.beginnerNotions)}`,
    }
  }
  // Refaire le test n'enlève rien : un domaine connu d'un test précédent le reste, même raté cette fois.
  if (domain.known) {
    return {
      icon: <FastForward weight="bold" aria-hidden className="size-7 shrink-0 text-brand" />,
      text: 'Déjà connu, d’un test précédent',
    }
  }
  if (verdict) {
    return {
      icon: <XCircle weight="bold" aria-hidden className="size-7 shrink-0 text-danger" />,
      text: 'À voir dans le Tour',
    }
  }
  return {
    icon: <Circle weight="bold" aria-hidden className="size-7 shrink-0 text-ink-faint" />,
    text: 'Pas posé : à voir dans le Tour',
  }
}

/** « Continuer le Tour » : vers la première notion ni validée, ni connue. */
function ContinueTour({ subject }: { subject: string }) {
  const tour = useTour(subject)
  const progress = useTourProgress(subject, true)
  const step =
    tour.data && progress.data
      ? firstStepToDiscover(
          tour.data,
          new Set(progress.data.validatedNotions),
          new Set(progress.data.knownNotions),
        )
      : undefined
  return (
    <Link
      to={step ? tourPath(subject, step) : `/subjects/${encodeURIComponent(subject)}/tour`}
      className={`self-start ${button()}`}
    >
      Continuer le Tour
    </Link>
  )
}

/** Annuler le test, en deux temps : un clic de trop ne fait pas revoir toutes les bases. */
function CancelLevelCheck({
  subject,
  title,
  onCancelled,
}: {
  subject: string
  title: string
  onCancelled: () => void
}) {
  const [confirming, setConfirming] = useState(false)
  const cancel = useCancelLevelCheck(subject)
  const keep = useFocusOnAppear<HTMLButtonElement>(confirming)

  if (!confirming) {
    return (
      <button
        type="button"
        onClick={() => setConfirming(true)}
        className={`self-start ${button({ variant: 'danger', size: 'sm' })}`}
      >
        Annuler le test
      </button>
    )
  }
  return (
    <div
      role="group"
      aria-labelledby="level-check-cancel"
      className="flex flex-col items-start gap-3 rounded-2xl border border-danger-line bg-danger-soft p-4"
    >
      <p id="level-check-cancel" className="font-semibold">
        Annuler le test ? Le Tour et tes révisions reprendront toutes les bases de {title}.
      </p>
      <div className="flex flex-wrap gap-3">
        <button
          type="button"
          // Le test se tire de nouveau une fois annulé, ce qui démonte ce résultat : la promesse, elle, aboutit.
          onClick={() => void cancel.mutateAsync().then(onCancelled, () => undefined)}
          disabled={cancel.isPending}
          className={button({ variant: 'danger', size: 'sm' })}
        >
          Oui, annuler le test
        </button>
        <button
          ref={keep}
          type="button"
          onClick={() => setConfirming(false)}
          className={button({ variant: 'ghost', size: 'sm' })}
        >
          Garder mes résultats
        </button>
      </div>
      {cancel.error && (
        <p role="alert" className="text-sm text-danger-strong">
          {cancel.error instanceof ApiError
            ? cancel.error.message
            : 'Le test n’a pas pu être annulé : réessaie.'}
        </p>
      )}
    </div>
  )
}
