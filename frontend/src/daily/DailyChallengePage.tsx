import { CalendarCheck, ListNumbers, UsersThree } from '@phosphor-icons/react'
import { useState, type FormEvent } from 'react'
import { Link, useParams } from 'react-router'
import { useSubjects } from '../api/content'
import {
  useAnswerDailyChallenge,
  useDailyChallenge,
  useStoreDailyChallenge,
  withAnswer,
  type DailyChallenge,
  type DailyChallengeAnswerResult,
  type DailyChallengeQuestion,
  type DailyChallengeResponse,
} from '../api/dailyChallenges'
import { ApiError } from '../api/errors'
import { useSession } from '../auth/session'
import { CARD_TYPE_LABELS } from '../components/cardTypeLabels'
import { GradedQuestion } from '../components/GradedQuestion'
import { emptyDraft, isDraftComplete, type AnswerDraft } from '../components/gradedAnswers'
import { dayFromToday, shortDate } from '../components/interviewDate'
import { LessonLink } from '../components/LessonLink'
import { ReportButton } from '../components/ReportButton'
import { EmptyState } from '../components/states/EmptyState'
import { ErrorState } from '../components/states/ErrorState'
import { LoadingState } from '../components/states/LoadingState'
import { button } from '../components/ui/button'
import { useFocusOnAppear } from '../components/useFocusOnAppear'
import { answerFeedback } from '../review/feedback'
import { ChallengeMarks } from './ChallengeMarks'
import { DailyChallengeResult } from './DailyChallengeResult'
import { dailyChallengePath } from './dailyChallengeShare'

const PRIMARY = `self-start ${button()}`

/**
 * Le défi du jour d'un sujet (ADR 0067) : cinq questions, les mêmes pour tous aujourd'hui, une seule tentative. La
 * page est publique, pour qu'un lien partagé s'ouvre chez un visiteur ; jouer demande un compte, seul moyen de garantir
 * une seule tentative. Les questions viennent une à une, corrigées tout de suite, puis le résultat.
 */
export function DailyChallengePage() {
  const { subject = '' } = useParams()
  const session = useSession()
  const subjects = useSubjects()
  const title = subjects.data?.find((candidate) => candidate.slug === subject)?.title ?? subject

  return (
    <section className="flex flex-col gap-5">
      <h1 className="text-2xl font-bold">Défi du jour : {title}</h1>
      {session.status === 'loading' ? (
        <LoadingState label="Chargement du défi…" shape="card" />
      ) : session.status === 'authenticated' ? (
        <Challenge subject={subject} />
      ) : (
        <VisitorIntro subject={subject} title={title} onLogin={session.login} />
      )}
    </section>
  )
}

const RULES = [
  { icon: ListNumbers, text: 'Cinq questions, de la plus facile à la plus difficile.' },
  { icon: CalendarCheck, text: 'Une seule tentative par jour : chaque réponse compte.' },
  { icon: UsersThree, text: 'Le même défi pour tout le monde : compare ton score avec tes amis.' },
]

/** Ce qu'est le défi, pour un visiteur arrivé par un lien partagé : se connecter pour jouer, ou faire l'essai. */
function VisitorIntro({
  subject,
  title,
  onLogin,
}: {
  subject: string
  title: string
  onLogin: (returnTo?: string) => void
}) {
  return (
    <div className="flex flex-col items-start gap-4 rounded-2xl border border-line bg-surface p-5 shadow-sm">
      <p className="text-ink-soft">Chaque jour, un nouveau défi en {title}.</p>
      <ul className="flex flex-col gap-2">
        {RULES.map(({ icon: Icon, text }) => (
          <li key={text} className="flex items-center gap-3">
            <Icon weight="duotone" aria-hidden className="size-6 shrink-0 text-brand" />
            {text}
          </li>
        ))}
      </ul>
      <div className="flex flex-wrap gap-3">
        <button
          type="button"
          onClick={() => onLogin(dailyChallengePath(subject))}
          className={button()}
        >
          Connecte-toi pour jouer
        </button>
        <Link
          to={`/try/${encodeURIComponent(subject)}`}
          className={button({ variant: 'secondary' })}
        >
          Essayer sans compte
        </Link>
      </div>
    </div>
  )
}

/** Le défi d'un utilisateur connecté : la question suivante, ou le résultat une fois le défi fini. */
function Challenge({ subject }: { subject: string }) {
  const challenge = useDailyChallenge(subject)
  const store = useStoreDailyChallenge(subject)

  if (challenge.error instanceof ApiError && [404, 422].includes(challenge.error.status)) {
    return (
      <EmptyState
        title="Pas de défi du jour pour ce sujet"
        text={challenge.error.message}
        mood="thinking"
      >
        <Link to="/play" className={button({ variant: 'secondary', size: 'sm' })}>
          Voir les autres défis
        </Link>
      </EmptyState>
    )
  }
  if (challenge.isError) {
    return (
      <ErrorState
        message="Impossible de charger le défi du jour."
        onRetry={() => void challenge.refetch()}
      />
    )
  }
  if (!challenge.data) return <LoadingState label="Chargement du défi…" shape="card" />

  const current = challenge.data
  const answered = new Set(current.answers.map((answer) => answer.position))
  const question = current.questions.find((candidate) => !answered.has(candidate.position))
  if (current.state === 'DONE' || !question) return <DailyChallengeResult challenge={current} />

  const index = current.questions.indexOf(question)
  return (
    <div className="flex flex-col gap-4">
      <DayLine day={current.day} />
      <div className="flex flex-wrap items-center justify-between gap-3">
        <p className="font-semibold">
          Question {index + 1} sur {current.questions.length}
        </p>
        <ChallengeMarks challenge={current} />
      </div>
      <QuestionCard
        key={`${current.id}-${question.position}`}
        subject={subject}
        challenge={current}
        question={question}
        onNext={(result) => store(withAnswer(current, result))}
      />
      <ReportButton key={`report-${question.cardId}`} target={{ cardId: question.cardId }} />
    </div>
  )
}

/** Le jour du défi ; celui de la veille se dit, puisqu'il passe avant celui du jour. */
function DayLine({ day }: { day: string }) {
  if (day === dayFromToday(-1)) {
    return (
      <p className="text-ink-soft">
        Ton défi d’hier, commencé avant minuit : finis-le, celui d’aujourd’hui t’attend ensuite.
      </p>
    )
  }
  return <p className="text-ink-soft">Défi du {shortDate(day)} : une seule réponse par question.</p>
}

/** Une question : une seule réponse, corrigée tout de suite par le serveur, puis la suivante. */
function QuestionCard({
  subject,
  challenge,
  question,
  onNext,
}: {
  subject: string
  challenge: DailyChallenge
  question: DailyChallengeQuestion
  onNext: (result: DailyChallengeAnswerResult) => void
}) {
  const answer = useAnswerDailyChallenge(subject)
  // Gardé tel quel pour un nouvel essai après une coupure : le serveur rend alors la même correction.
  const [requestId] = useState(() => crypto.randomUUID())
  const [draft, setDraft] = useState(() => emptyDraft(question))
  const result = answer.data
  const next = useFocusOnAppear<HTMLButtonElement>(result !== undefined)

  const submit = (event: FormEvent) => {
    event.preventDefault()
    answer.mutate(
      {
        challengeId: challenge.id,
        requestId,
        position: question.position,
        answer: toResponse(question, draft),
      },
      { onSuccess: (outcome) => answerFeedback(outcome.correction.correct) },
    )
  }

  return (
    <form
      onSubmit={submit}
      className="flex flex-col gap-4 rounded-2xl border border-line bg-surface p-5 shadow-sm"
    >
      <GradedQuestion
        question={question}
        label={CARD_TYPE_LABELS[question.type]}
        draft={draft}
        correction={result?.correction}
        onChange={setDraft}
      />
      {result && !result.correction.correct && question.notionSlug && (
        <LessonLink subject={subject} notion={question.notionSlug} />
      )}
      {result ? (
        <button ref={next} type="button" onClick={() => onNext(result)} className={PRIMARY}>
          {result.state === 'DONE' ? 'Voir mon résultat' : 'Question suivante'}
        </button>
      ) : (
        <button
          type="submit"
          disabled={!isDraftComplete(question, draft) || answer.isPending}
          className={PRIMARY}
        >
          {answer.isPending ? 'Vérification…' : 'Valider'}
        </button>
      )}
      {answer.error && (
        <p role="alert" className="text-sm text-danger">
          {answer.error instanceof ApiError
            ? answer.error.message
            : 'Ta réponse n’est pas partie : réessaie.'}
        </p>
      )}
    </form>
  )
}

/** La réponse telle que le serveur l'attend : les choix cochés, ou les trous remplis. */
function toResponse(question: DailyChallengeQuestion, draft: AnswerDraft): DailyChallengeResponse {
  return question.type === 'FILL_IN'
    ? { filledBlanks: draft.blanks }
    : { selectedChoices: draft.choices }
}
