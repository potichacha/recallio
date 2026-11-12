import {
  ArrowsClockwise,
  Barbell,
  Briefcase,
  CheckCircle,
  Cursor,
  GraduationCap,
  type Icon,
} from '@phosphor-icons/react'
import { useState, type FormEvent } from 'react'
import { Link } from 'react-router'
import { useCatalogSubjects, type Subject } from '../api/content'
import { ApiError } from '../api/errors'
import { useCheckTrialAnswer, useTrial, type TrialQuestion } from '../api/trial'
import { useSession } from '../auth/session'
import { CARD_TYPE_LABELS } from '../components/cardTypeLabels'
import { GradedQuestion } from '../components/GradedQuestion'
import { emptyDraft, isDraftComplete, toGradedAnswer } from '../components/gradedAnswers'
import { Rico } from '../components/Rico'
import { button } from '../components/ui/button'
import { answerFeedback } from '../review/feedback'
import { SubjectGallery } from '../subjects/SubjectGallery'
import type { TrialStart } from './trialStart'

const CARD = 'rounded-2xl border border-line bg-surface p-5 shadow-sm'

const AUDIENCES: { icon: Icon; title: string; text: string }[] = [
  {
    icon: GraduationCap,
    title: 'Tu apprends un langage',
    text: 'Suis son Tour page par page : un texte court, un programme à modifier et à exécuter.',
  },
  {
    icon: Briefcase,
    title: 'Tu prépares un entretien',
    text: 'Révise d’abord ce qui tombe le plus souvent, et vois si tu es prêt.',
  },
  {
    icon: Barbell,
    title: 'Tu entretiens ton niveau',
    text: 'Garde en mémoire les notions avancées, dix minutes par jour.',
  },
]

const STEPS: { icon: Icon; title: string; text: string }[] = [
  {
    icon: Cursor,
    title: 'Tu réponds',
    text: 'Une question à la fois : un choix, un code à compléter, un exercice.',
  },
  {
    icon: CheckCircle,
    title: 'Le serveur corrige',
    text: 'Tu sais tout de suite si c’est juste, et pourquoi.',
  },
  {
    icon: ArrowsClockwise,
    title: 'La carte revient au bon moment',
    text: 'Juste avant que tu l’oublies : c’est la répétition espacée.',
  },
]

/**
 * La page publique (ADR 0058, audit élément 49) : un visiteur répond à une vraie question avant de lire quoi que ce
 * soit, puis voit pour qui est l'application, comment elle marche et ce qu'on y apprend.
 */
export function LandingPage() {
  // Tout le catalogue, sujets en préparation compris : ils sont annoncés en bas de page, comme sur la page Cours.
  const subjects = useCatalogSubjects()
  const session = useSession()
  // Pour l'essai et les chiffres, seuls les sujets publiés comptent : la page dit la même chose à tout le monde.
  const published = subjects.data?.filter((subject) => subject.status === 'PUBLISHED') ?? []
  const trials = published.filter((subject) => subject.hasTour)

  return (
    <div className="flex flex-col gap-10 py-4">
      <header className="flex flex-col gap-4">
        <h1 className="text-4xl leading-tight font-bold">
          Prêt pour ton entretien technique, dix minutes par jour.
        </h1>
        <div className="flex items-center gap-3">
          <Rico decorative />
          <p className={`${CARD} flex-1 py-3 text-ink-soft`}>
            Je te pose les bonnes questions au bon moment, et je te les repose avant que tu les
            oublies.
          </p>
        </div>
      </header>

      {trials.length > 0 && (
        <section aria-labelledby="trial-title" className="flex flex-col gap-3">
          <h2 id="trial-title" className="text-xl font-semibold">
            Essaie sans compte
          </h2>
          <FirstQuestion subject={trials[0]} />
          <div className="flex flex-wrap items-center gap-3">
            <span className="text-sm text-ink-muted">Cinq questions, rien à remplir :</span>
            {trials.map((subject) => (
              <Link
                key={subject.slug}
                to={`/try/${subject.slug}`}
                className={button({ variant: 'secondary', size: 'sm' })}
              >
                Essayer {subject.title}
              </Link>
            ))}
          </div>
        </section>
      )}

      {published.length > 0 && <Figures subjects={published} />}

      <Features id="audiences-title" title="Pour qui" items={AUDIENCES} />
      <Features id="steps-title" title="Comment ça marche" items={STEPS} ordered />

      {subjects.data && subjects.data.length > 0 && (
        <section aria-labelledby="subjects-title" className="flex flex-col gap-4">
          <h2 id="subjects-title" className="text-xl font-semibold">
            Ce qu’on y apprend
          </h2>
          <SubjectGallery
            subjects={subjects.data}
            headingLevel={3}
            isOpen={(subject) => subject.status === 'PUBLISHED' && subject.cardCount > 0}
            linkTo={(subject) => `/subjects/${encodeURIComponent(subject.slug)}`}
          />
        </section>
      )}

      <section aria-labelledby="start-title" className={`${CARD} flex flex-col items-start gap-3`}>
        <h2 id="start-title" className="text-xl font-semibold">
          On commence ?
        </h2>
        <p className="text-ink-muted">
          Crée ton compte pour garder ta progression : ta série commence aujourd’hui.
        </p>
        <div className="flex flex-wrap gap-3">
          <button type="button" onClick={() => session.login('/')} className={button()}>
            Me connecter ou créer un compte
          </button>
          <Link to="/learn" className={button({ variant: 'ghost' })}>
            Voir ce qu’on peut apprendre
          </Link>
        </div>
      </section>
    </div>
  )
}

/**
 * La première question de l'essai d'un sujet (ADR 0050), posée dans la page : le serveur la corrige, puis l'essai
 * continue à la deuxième. L'emplacement est réservé pendant le chargement, pour que la page ne saute pas. Si l'essai
 * n'est pas disponible, la page s'en passe : les boutons « Essayer » restent.
 */
function FirstQuestion({ subject }: { subject: Subject }) {
  const trial = useTrial(subject.slug)
  const question = trial.data?.questions.at(0)
  if (trial.isError || (trial.data && !question)) return null
  if (!question) {
    return (
      <div className={`${CARD} min-h-56 text-ink-muted`} aria-busy="true">
        Préparation d’une question de {subject.title}…
      </div>
    )
  }
  return (
    <QuestionCard subject={subject} question={question} total={trial.data?.questions.length ?? 1} />
  )
}

function QuestionCard({
  subject,
  question,
  total,
}: {
  subject: Subject
  question: TrialQuestion
  total: number
}) {
  const { card, notion } = question
  const check = useCheckTrialAnswer(subject.slug, notion)
  const [draft, setDraft] = useState(() => emptyDraft(card))
  const correction = check.data
  const remaining = total - 1

  const submit = (event: FormEvent) => {
    event.preventDefault()
    check.mutate(toGradedAnswer(card, draft), {
      onSuccess: (result) => answerFeedback(result.correct),
    })
  }

  return (
    <form onSubmit={submit} className={`${CARD} flex min-h-56 flex-col gap-4`}>
      <GradedQuestion
        question={card}
        label={`${subject.title} · ${CARD_TYPE_LABELS[card.type]}`}
        draft={draft}
        correction={correction}
        onChange={setDraft}
      />
      {correction ? (
        <Link
          to={`/try/${subject.slug}`}
          state={{ answered: 1, right: correction.correct ? 1 : 0 } satisfies TrialStart}
          className={`self-start ${button()}`}
        >
          {remaining > 0
            ? `Continuer : encore ${remaining} question${remaining > 1 ? 's' : ''}`
            : 'Voir le bilan'}
        </Link>
      ) : (
        <button
          type="submit"
          disabled={!isDraftComplete(card, draft) || check.isPending}
          className={`self-start ${button()}`}
        >
          {check.isPending ? 'Vérification…' : 'Vérifier'}
        </button>
      )}
      {check.error && (
        <p role="alert" className="text-sm text-danger">
          {check.error instanceof ApiError ? check.error.message : 'La vérification a échoué.'}
        </p>
      )}
    </form>
  )
}

/** Les chiffres du catalogue publié, donnés par le serveur : ils suivent le contenu, sans texte à tenir à jour. */
function Figures({ subjects }: { subjects: Subject[] }) {
  const sum = (count: (subject: Subject) => number) =>
    subjects.reduce((total, subject) => total + count(subject), 0)
  const figures = [
    { label: 'cartes', value: sum((subject) => subject.cardCount) },
    { label: 'notions', value: sum((subject) => subject.notionCount) },
    { label: 'exercices de code', value: sum((subject) => subject.exerciseCount) },
  ].filter(({ value }) => value > 0)
  if (figures.length === 0) return null
  return (
    <ul aria-label="Le catalogue en chiffres" className="grid grid-cols-3 gap-3 text-center">
      {figures.map(({ label, value }) => (
        <li key={label} className={`${CARD} flex flex-col p-3`}>
          <strong className="text-2xl font-extrabold text-brand-strong">
            {value.toLocaleString('fr-FR')}
          </strong>
          <span className="text-sm text-ink-muted">{label}</span>
        </li>
      ))}
    </ul>
  )
}

function Features({
  id,
  title,
  items,
  ordered = false,
}: {
  id: string
  title: string
  items: { icon: Icon; title: string; text: string }[]
  ordered?: boolean
}) {
  const List = ordered ? 'ol' : 'ul'
  return (
    <section aria-labelledby={id} className="flex flex-col gap-4">
      <h2 id={id} className="text-xl font-semibold">
        {title}
      </h2>
      <List className="grid gap-3 sm:grid-cols-3">
        {items.map(({ icon: ItemIcon, title: itemTitle, text }, index) => (
          <li key={itemTitle} className={`${CARD} flex flex-col gap-2`}>
            <span className="flex items-center gap-2">
              <ItemIcon weight="bold" aria-hidden className="size-6 shrink-0 text-brand" />
              <h3 className="font-bold">
                {ordered && `${index + 1}. `}
                {itemTitle}
              </h3>
            </span>
            <p className="text-sm text-ink-muted">{text}</p>
          </li>
        ))}
      </List>
    </section>
  )
}
