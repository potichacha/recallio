import {
  CalendarBlank,
  CalendarCheck,
  CalendarPlus,
  RocketLaunch,
  SealCheck,
  WarningCircle,
  type Icon,
} from '@phosphor-icons/react'
import { useState, type FormEvent } from 'react'
import { Link } from 'react-router'
import { ApiError } from '../api/errors'
import {
  useCancelInterview,
  usePlanInterview,
  type InterviewCountdown,
  type InterviewNotion,
  type InterviewStatus,
} from '../api/interviews'
import type { LearningTrack } from '../api/profile'
import { fullDate, interviewDateBounds, shortDate } from '../components/interviewDate'
import { MASTERY_LABELS, MASTERY_STYLES } from '../components/mastery'
import { button } from '../components/ui/button'
import { useFocusOnAppear } from '../components/useFocusOnAppear'

const plural = (count: number, word: string) => `${count} ${word}${count > 1 ? 's' : ''}`

/** Chaque statut a son icône et son texte : la couleur seule ne dit jamais rien (ADR 0058). */
const STATUSES: Record<
  Exclude<InterviewStatus, 'PASSED'>,
  { label: string; detail: string; icon: Icon; style: string }
> = {
  AHEAD: {
    label: 'En avance',
    detail: 'L’objectif de la semaine est déjà atteint.',
    icon: RocketLaunch,
    style: 'bg-success-soft text-success',
  },
  ON_TRACK: {
    label: 'Dans les temps',
    detail: 'Tu tiens le plan : continue chaque jour.',
    icon: CalendarCheck,
    style: 'bg-info-soft text-info',
  },
  BEHIND: {
    label: 'En retard',
    detail:
      'Des notions des semaines passées restent à acquérir : elles s’ajoutent à cette semaine.',
    icon: WarningCircle,
    style: 'bg-warning-soft text-warning',
  },
  READY: {
    label: 'Prêt',
    detail: 'Ton indice de préparation a passé les 80 % : entretiens-le jusqu’au jour J.',
    icon: SealCheck,
    style: 'bg-success-soft text-success',
  },
}

/**
 * Le compte à rebours avant l'entretien d'un sujet (ADR 0066), sous sa jauge « Prêt pour l'entretien » : sans date, une
 * action discrète pour en donner une ; avec une date, les jours qui restent, l'avance ou le retard, les notions
 * essentielles de la semaine et les semaines suivantes. Le plan est calculé par le serveur.
 */
export function InterviewCountdownPanel({
  subject,
  title,
  countdown,
  track,
}: {
  subject: string
  title: string
  countdown: InterviewCountdown | undefined
  track: LearningTrack
}) {
  const [editing, setEditing] = useState(false)

  return (
    <div role="group" aria-label={`Entretien ${title}`} className="flex flex-col gap-2">
      {editing ? (
        <InterviewDateForm
          subject={subject}
          title={title}
          initial={countdown?.date}
          onDone={() => setEditing(false)}
        />
      ) : countdown ? (
        <CountdownView countdown={countdown} track={track} onChange={() => setEditing(true)} />
      ) : (
        <button
          type="button"
          onClick={() => setEditing(true)}
          className={`self-start ${button({ variant: 'ghost', size: 'sm' })}`}
        >
          <CalendarPlus weight="bold" aria-hidden />
          Tu as un entretien ? Donne sa date
        </button>
      )}
    </div>
  )
}

function InterviewDateForm({
  subject,
  title,
  initial,
  onDone,
}: {
  subject: string
  title: string
  initial: string | undefined
  onDone: () => void
}) {
  const [date, setDate] = useState(initial ?? '')
  const plan = usePlanInterview()
  const input = useFocusOnAppear<HTMLInputElement>(true)
  const { min, max } = interviewDateBounds()

  const submit = async (event: FormEvent) => {
    event.preventDefault()
    try {
      await plan.mutateAsync({ subject, date })
      onDone()
    } catch {
      // L'erreur est dite sous le formulaire : la date se corrige sur place.
    }
  }

  return (
    <form
      onSubmit={submit}
      aria-label={`Date de ton entretien en ${title}`}
      className="flex flex-col gap-2 rounded-lg border border-line p-3"
    >
      <label className="flex flex-col gap-1">
        <span className="text-sm font-bold">Date de ton entretien</span>
        <input
          ref={input}
          type="date"
          value={date}
          min={min}
          max={max}
          required
          onChange={(event) => setDate(event.target.value)}
          className="self-start rounded-lg border border-line-strong bg-surface px-3 py-2"
        />
      </label>
      <div className="flex flex-wrap gap-2">
        <button
          type="submit"
          disabled={date === '' || plan.isPending}
          className={button({ size: 'sm' })}
        >
          Valider
        </button>
        <button type="button" onClick={onDone} className={button({ variant: 'ghost', size: 'sm' })}>
          Annuler
        </button>
      </div>
      {plan.isError && (
        <p role="alert" className="text-sm text-danger-strong">
          {plan.error instanceof ApiError
            ? plan.error.message
            : 'Ta date n’a pas pu être enregistrée.'}
        </p>
      )}
    </form>
  )
}

function CountdownView({
  countdown,
  track,
  onChange,
}: {
  countdown: InterviewCountdown
  track: LearningTrack
  onChange: () => void
}) {
  const cancel = useCancelInterview()
  const { status } = countdown
  const passed = status === 'PASSED'
  const headline = passed
    ? 'Ton entretien est passé'
    : countdown.daysLeft === 0
      ? 'C’est aujourd’hui'
      : `Entretien dans ${plural(countdown.daysLeft, 'jour')}`

  return (
    <div className="flex flex-col gap-3 rounded-lg border border-line p-3">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <p className="flex items-center gap-2 font-extrabold">
          <CalendarBlank weight="bold" aria-hidden className="text-brand" />
          {headline}
        </p>
        {status !== 'PASSED' && <StatusBadge status={status} />}
      </div>
      <p className="-mt-2 text-xs text-ink-muted">Le {fullDate(countdown.date)}</p>
      {status !== 'PASSED' && (
        <>
          <p className="text-sm text-ink-soft">{STATUSES[status].detail}</p>
          <ThisWeek countdown={countdown} />
          {countdown.nextWeeks.length > 0 && <NextWeeks countdown={countdown} />}
          {track !== 'PREPARE' && (
            <p className="text-xs text-ink-muted">
              Ton parcours n’est pas « Préparer un entretien » : c’est lui qui te sert d’abord les
              notions essentielles.{' '}
              <Link to="/profile/subjects" className="font-bold text-brand-strong underline">
                Changer de parcours
              </Link>
            </p>
          )}
        </>
      )}
      <div className="flex flex-wrap gap-2">
        {!passed && (
          <button
            type="button"
            onClick={onChange}
            className={button({ variant: 'secondary', size: 'sm' })}
          >
            Changer la date
          </button>
        )}
        <button
          type="button"
          onClick={() => cancel.mutate(countdown.subjectSlug)}
          disabled={cancel.isPending}
          className={button({ variant: 'ghost', size: 'sm' })}
        >
          Retirer la date
        </button>
      </div>
      {cancel.isError && (
        <p role="alert" className="text-sm text-danger-strong">
          Ta date n’a pas pu être retirée.
        </p>
      )}
    </div>
  )
}

function StatusBadge({ status }: { status: Exclude<InterviewStatus, 'PASSED'> }) {
  const { label, icon: StatusIcon, style } = STATUSES[status]
  return (
    <span
      className={`inline-flex items-center gap-1 rounded-full px-2 py-0.5 text-xs font-extrabold ${style}`}
    >
      <StatusIcon weight="bold" aria-hidden />
      {label}
    </span>
  )
}

/**
 * La semaine en cours : ses notions, chacune menant à sa leçon. La dernière semaine sert à réviser ; elle ne montre que
 * ce qu'il reste à rattraper, s'il en reste.
 */
function ThisWeek({ countdown }: { countdown: InterviewCountdown }) {
  const { thisWeek, week, weeks, reviewWeek, essentialsLeft } = countdown
  const notions = thisWeek.notions
  let heading: string
  if (reviewWeek) heading = 'Dernière semaine : révise, sans nouvelle notion'
  else if (thisWeek.quota > 0) heading = `Cette semaine : ${plural(thisWeek.quota, 'notion')}`
  else heading = 'Cette semaine : objectif atteint'

  return (
    <div className="flex flex-col gap-2">
      <div className="flex flex-col">
        <p className="text-sm font-extrabold">{heading}</p>
        <p className="text-xs text-ink-muted">
          Semaine {week} sur {weeks} · du {shortDate(thisWeek.startsOn)} au{' '}
          {shortDate(thisWeek.endsOn)}
        </p>
      </div>
      {reviewWeek && thisWeek.quota > 0 && (
        <p className="text-sm">
          Encore {plural(thisWeek.quota, 'notion')} à rattraper avant l’entretien :
        </p>
      )}
      {essentialsLeft === 0 && (
        <p className="text-sm text-ink-soft">
          Toutes tes notions essentielles sont acquises : révise-les pour les maîtriser d’ici
          l’entretien.
        </p>
      )}
      {notions.length > 0 && (
        <ul aria-label="Les notions de la semaine" className="flex flex-col gap-1">
          {notions.map((notion) => (
            <NotionItem key={notion.slug} subject={countdown.subjectSlug} notion={notion} />
          ))}
        </ul>
      )}
    </div>
  )
}

function NotionItem({ subject, notion }: { subject: string; notion: InterviewNotion }) {
  return (
    <li className="flex items-center justify-between gap-3 rounded-lg bg-surface-muted px-3 py-2">
      <Link
        to={`/subjects/${encodeURIComponent(subject)}/course/${encodeURIComponent(notion.slug)}`}
        className="text-sm font-bold hover:text-brand hover:underline"
      >
        {notion.title}
      </Link>
      <span
        className={`shrink-0 rounded-full px-2 py-0.5 text-xs font-bold ${MASTERY_STYLES[notion.mastery]}`}
      >
        {MASTERY_LABELS[notion.mastery]}
      </span>
    </li>
  )
}

function NextWeeks({ countdown }: { countdown: InterviewCountdown }) {
  return (
    <details>
      <summary className="cursor-pointer text-sm font-bold text-brand-strong">
        Les semaines suivantes
      </summary>
      <ul className="mt-2 flex flex-col gap-1 text-sm">
        {countdown.nextWeeks.map((week) => (
          <li key={week.number} className="flex flex-wrap justify-between gap-x-3">
            <span>
              Semaine {week.number} · du {shortDate(week.startsOn)} au {shortDate(week.endsOn)}
            </span>
            <span className="text-ink-muted">
              {week.review ? 'révision, sans nouvelle notion' : plural(week.notions, 'notion')}
            </span>
          </li>
        ))}
      </ul>
    </details>
  )
}
