import { useState, type FormEvent } from 'react'
import { Link, useParams, useSearchParams } from 'react-router'
import { ApiError } from '../api/errors'
import { useCheckPracticeAnswer, usePractice, type PracticeCard } from '../api/practice'
import { useRunExercise, type ExerciseFile } from '../api/review'
import { CARD_TYPE_LABELS } from '../components/cardTypeLabels'
import { GradedQuestion } from '../components/GradedQuestion'
import { emptyDraft, isDraftComplete, toGradedAnswer } from '../components/gradedAnswers'
import { Markdown } from '../components/Markdown'
import { ReportButton } from '../components/ReportButton'
import { ExerciseEditor } from '../review/ExerciseEditor'
import { ExerciseRunResult } from '../review/ExerciseRunResult'

const CARD_STYLE = 'flex flex-col gap-4 rounded-xl border border-slate-200 bg-white p-5 shadow-sm'
const PRIMARY =
  'self-start rounded-lg bg-indigo-600 px-4 py-2 font-medium text-white hover:bg-indigo-700 disabled:opacity-50'
const SECONDARY =
  'self-start rounded-lg border border-indigo-200 px-4 py-2 font-medium text-indigo-700 hover:bg-indigo-50 disabled:opacity-50'

/** Les formats que le serveur corrige d'après la réponse : des choix cochés ou des trous remplis. */
const ANSWERED_FORMATS: ReadonlySet<PracticeCard['type']> = new Set([
  'TRUE_FALSE',
  'MULTIPLE_CHOICE',
  'FILL_IN',
])

/** Une carte passée : juste ou non quand le serveur l'a corrigée, `undefined` quand elle s'auto-évalue. */
type Outcome = boolean | undefined

/**
 * S'entraîner sur une notion (ADR 0038) : toutes ses cartes, une à une, les formats mêlés. Le serveur corrige les vrai
 * ou faux, les QCM et les codes à trous ; un exercice s'essaie contre ses tests ; les autres cartes se retournent. Rien
 * n'est enregistré : la révision espacée reprendra ces cartes, une par jour.
 */
export function PracticePage() {
  const { slug = '', notion = '' } = useParams()
  const [params] = useSearchParams()
  const practice = usePractice(slug, notion)
  const [position, setPosition] = useState<number | null>(null)
  const [outcomes, setOutcomes] = useState<Outcome[]>([])
  const subjectPath = `/subjects/${encodeURIComponent(slug)}`
  const back =
    params.get('from') === 'tour'
      ? { to: `${subjectPath}/tour/${encodeURIComponent(notion)}`, label: 'Revenir au Tour' }
      : { to: `${subjectPath}/path`, label: 'Voir mon parcours' }

  if (practice.error instanceof ApiError && practice.error.status === 404) {
    return (
      <section className="flex flex-col gap-3">
        <h1 className="text-2xl font-bold">Notion introuvable</h1>
        <Link to={subjectPath} className="text-indigo-600 hover:underline">
          Retour au sujet
        </Link>
      </section>
    )
  }
  if (practice.isError) return <p role="alert">Impossible de charger l’entraînement.</p>
  if (!practice.data) return <p className="text-slate-500">Chargement…</p>

  const { notionTitle, cards } = practice.data
  // Un lien vers une carte, comme un exercice de fin de chapitre, commence l'entraînement par elle.
  const current =
    position ??
    Math.max(
      0,
      cards.findIndex((candidate) => candidate.cardId === params.get('card')),
    )
  const card = cards[current]
  const next = (outcome: Outcome) => {
    setOutcomes((previous) => [...previous, outcome])
    setPosition(current + 1)
  }
  const restart = () => {
    setOutcomes([])
    setPosition(0)
  }

  return (
    <section className="flex flex-col gap-5">
      <header className="flex flex-col gap-1">
        <h1 className="text-2xl font-bold">S’entraîner : {notionTitle}</h1>
        <p className="text-sm text-slate-600">
          Rien n’est enregistré : ces cartes reviendront dans tes révisions, une par jour.
        </p>
      </header>
      {cards.length === 0 && <p>Cette notion n’a pas encore de cartes.</p>}
      {card && (
        <>
          <p className="text-sm text-slate-500">
            Carte {current + 1} sur {cards.length}
          </p>
          <PracticeCardView
            key={card.cardId}
            subject={slug}
            notion={notion}
            card={card}
            onNext={next}
          />
          <ReportButton key={`report-${card.cardId}`} target={{ cardId: card.cardId }} />
        </>
      )}
      {cards.length > 0 && !card && <Summary outcomes={outcomes} onRestart={restart} />}
      <Link to={back.to} className="self-start text-sm text-indigo-600 hover:underline">
        {back.label}
      </Link>
    </section>
  )
}

function Summary({ outcomes, onRestart }: { outcomes: Outcome[]; onRestart: () => void }) {
  const graded = outcomes.filter((outcome) => outcome !== undefined)
  const right = graded.filter(Boolean).length
  return (
    <div className={CARD_STYLE}>
      <p role="status" className="font-medium">
        Entraînement terminé !
      </p>
      {graded.length > 0 && (
        <p className="text-slate-600">
          {right} / {graded.length} juste{right > 1 ? 's' : ''} parmi les cartes corrigées par le
          serveur.
        </p>
      )}
      <button type="button" onClick={onRestart} className={SECONDARY}>
        Recommencer
      </button>
    </div>
  )
}

function PracticeCardView({
  subject,
  notion,
  card,
  onNext,
}: {
  subject: string
  notion: string
  card: PracticeCard
  onNext: (outcome: Outcome) => void
}) {
  if (ANSWERED_FORMATS.has(card.type)) {
    return <AnsweredCard subject={subject} notion={notion} card={card} onNext={onNext} />
  }
  if (card.type === 'CODING_EXERCISE') return <ExerciseTry card={card} onNext={onNext} />
  return <SelfAssessedCard card={card} onNext={onNext} />
}

/** Un vrai ou faux, un QCM ou un code à trous : le serveur corrige, sans rien enregistrer. */
function AnsweredCard({
  subject,
  notion,
  card,
  onNext,
}: {
  subject: string
  notion: string
  card: PracticeCard
  onNext: (outcome: Outcome) => void
}) {
  const check = useCheckPracticeAnswer(subject, notion)
  const [draft, setDraft] = useState(() => emptyDraft(card))
  const correction = check.data

  const submit = (event: FormEvent) => {
    event.preventDefault()
    check.mutate(toGradedAnswer(card, draft))
  }

  return (
    <form onSubmit={submit} className={CARD_STYLE}>
      <GradedQuestion
        question={card}
        label={CARD_TYPE_LABELS[card.type]}
        draft={draft}
        correction={correction}
        onChange={setDraft}
      />
      {correction ? (
        <button type="button" onClick={() => onNext(correction.correct)} className={PRIMARY}>
          Carte suivante
        </button>
      ) : (
        <button
          type="submit"
          disabled={!isDraftComplete(card, draft) || check.isPending}
          className={PRIMARY}
        >
          {check.isPending ? 'Vérification…' : 'Vérifier'}
        </button>
      )}
      {check.error && (
        <p role="alert" className="text-sm text-rose-700">
          {check.error instanceof ApiError ? check.error.message : 'La vérification a échoué.'}
        </p>
      )}
    </form>
  )
}

/** Une carte qui s'auto-évalue : on y réfléchit (une mise en situation ou un bug s'écrivent), puis on la retourne. */
function SelfAssessedCard({
  card,
  onNext,
}: {
  card: PracticeCard
  onNext: (outcome: Outcome) => void
}) {
  const [revealed, setRevealed] = useState(false)
  const [draft, setDraft] = useState('')
  const writes = card.type === 'OPEN_SCENARIO' || card.type === 'BUG_FIX'
  return (
    <article className={CARD_STYLE}>
      <p className="text-sm font-medium text-slate-500">{CARD_TYPE_LABELS[card.type]}</p>
      <Markdown>{card.prompt}</Markdown>
      {writes && (
        <label className="flex flex-col gap-1">
          <span className="text-sm font-medium">Ta réponse</span>
          <textarea
            value={draft}
            onChange={(event) => setDraft(event.target.value)}
            readOnly={revealed}
            rows={5}
            className="rounded-lg border border-slate-300 px-3 py-2 text-sm"
          />
        </label>
      )}
      {revealed ? (
        <>
          <section aria-label="Réponse" className="rounded-lg bg-slate-50 p-3">
            <Markdown>{card.answer ?? ''}</Markdown>
          </section>
          <button type="button" onClick={() => onNext(undefined)} className={PRIMARY}>
            Carte suivante
          </button>
        </>
      ) : (
        <button type="button" onClick={() => setRevealed(true)} className={PRIMARY}>
          Voir la réponse
        </button>
      )}
    </article>
  )
}

/** Un exercice de code : on lance ses tests autant de fois qu'on veut, sans rien rendre. */
function ExerciseTry({ card, onNext }: { card: PracticeCard; onNext: (outcome: Outcome) => void }) {
  const [files, setFiles] = useState<ExerciseFile[]>(() => card.exercise?.starter ?? [])
  const run = useRunExercise()
  const edit = (path: string, content: string) =>
    setFiles((current) => current.map((file) => (file.path === path ? { path, content } : file)))
  return (
    <article className={CARD_STYLE}>
      <p className="text-sm font-medium text-slate-500">{CARD_TYPE_LABELS[card.type]}</p>
      <Markdown>{card.prompt}</Markdown>
      <ExerciseEditor
        files={files}
        tests={card.exercise?.tests ?? []}
        readOnly={false}
        onEdit={edit}
      />
      <div className="flex flex-wrap gap-2">
        <button
          type="button"
          disabled={run.isPending}
          onClick={() => run.mutate({ cardId: card.cardId, files })}
          className={SECONDARY}
        >
          {run.isPending ? 'Exécution…' : 'Lancer les tests'}
        </button>
        <button
          type="button"
          onClick={() => onNext(run.data ? run.data.status === 'PASSED' : undefined)}
          className={PRIMARY}
        >
          Carte suivante
        </button>
      </div>
      {run.error && (
        <p role="alert" className="text-sm text-rose-700">
          {run.error instanceof ApiError ? run.error.message : 'L’exécution a échoué.'}
        </p>
      )}
      {run.data && <ExerciseRunResult run={run.data} />}
    </article>
  )
}
