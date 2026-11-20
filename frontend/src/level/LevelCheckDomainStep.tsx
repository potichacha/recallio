import { useState } from 'react'
import { ApiError } from '../api/errors'
import {
  useCheckLevelCheckDomain,
  type LevelCheckAnswer,
  type LevelCheckDomain,
  type LevelCheckDomainResult,
  type LevelCheckQuestion,
} from '../api/levelChecks'
import { AnswerVerdict } from '../components/AnswerVerdict'
import { CARD_TYPE_LABELS } from '../components/cardTypeLabels'
import { GradedQuestion } from '../components/GradedQuestion'
import { emptyDraft, isDraftComplete, type AnswerDraft } from '../components/gradedAnswers'
import { ReportButton } from '../components/ReportButton'
import { button } from '../components/ui/button'
import { useFocusOnAppear } from '../components/useFocusOnAppear'
import { notionCount } from './levelCheckRun'

const PRIMARY = `self-start ${button()}`

/**
 * Un domaine des bases : ses questions, une ou deux, corrigées ensemble par le serveur, puis son verdict. Réussi, il
 * est sauté dans le Tour ; raté, il est à voir dans le Tour.
 *
 * @param isLast vrai si ce domaine, avec ce verdict, finit le test : le bouton mène alors au résultat
 */
export function LevelCheckDomainStep({
  subject,
  domain,
  rank,
  domainCount,
  isLast,
  onNext,
}: {
  subject: string
  domain: LevelCheckDomain
  rank: number
  domainCount: number
  isLast: (passed: boolean) => boolean
  onNext: (result: LevelCheckDomainResult) => void
}) {
  const check = useCheckLevelCheckDomain(subject)
  const [drafts, setDrafts] = useState<Record<string, AnswerDraft>>(() =>
    Object.fromEntries(domain.questions.map((question) => [question.cardId, emptyDraft(question)])),
  )
  const result = check.data
  const next = useFocusOnAppear<HTMLButtonElement>(result !== undefined)
  const complete = domain.questions.every((question) =>
    isDraftComplete(question, drafts[question.cardId] ?? emptyDraft(question)),
  )

  const submit = () => {
    check.mutate({
      domain: domain.id,
      answers: domain.questions.map((question) =>
        toAnswer(question, drafts[question.cardId] ?? emptyDraft(question)),
      ),
    })
  }

  // Une section, pas un formulaire : chaque question a son « Signaler une erreur », dont le formulaire ne doit pas être
  // imbriqué dans un autre (l'envoi d'un signalement remonterait jusqu'ici et validerait le domaine). Les réponses
  // vivent dans l'état, pas dans les champs : « Valider » n'a pas besoin d'un formulaire.
  return (
    <section
      aria-labelledby="level-check-domain"
      className="flex flex-col gap-4 rounded-2xl border border-line bg-surface p-5 shadow-sm"
    >
      <h2 id="level-check-domain" className="text-lg font-bold">
        Domaine {rank} sur {domainCount} : {domain.title}
      </h2>
      {domain.questions.map((question) => (
        <div key={question.cardId} className="flex flex-col gap-2">
          <GradedQuestion
            question={question}
            label={CARD_TYPE_LABELS[question.type]}
            draft={drafts[question.cardId] ?? emptyDraft(question)}
            correction={result?.corrections.find(
              (correction) => correction.cardId === question.cardId,
            )}
            onChange={(draft) => setDrafts({ ...drafts, [question.cardId]: draft })}
          />
          <ReportButton key={`report-${question.cardId}`} target={{ cardId: question.cardId }} />
        </div>
      ))}
      {result && (
        <AnswerVerdict correct={result.passed}>
          {result.passed
            ? `Domaine réussi : tu sautes ${notionCount(domain.beginnerNotions)}`
            : 'À voir dans le Tour'}
        </AnswerVerdict>
      )}
      {result ? (
        <button ref={next} type="button" onClick={() => onNext(result)} className={PRIMARY}>
          {isLast(result.passed) ? 'Voir le résultat' : 'Domaine suivant'}
        </button>
      ) : (
        <button
          type="button"
          onClick={submit}
          disabled={!complete || check.isPending}
          className={PRIMARY}
        >
          {check.isPending ? 'Vérification…' : 'Valider'}
        </button>
      )}
      {check.error && (
        <p role="alert" className="text-sm text-danger">
          {check.error instanceof ApiError
            ? check.error.message
            : 'Tes réponses ne sont pas parties : réessaie.'}
        </p>
      )}
    </section>
  )
}

/** La réponse telle que le serveur l'attend : les choix cochés, ou les trous remplis. */
function toAnswer(question: LevelCheckQuestion, draft: AnswerDraft): LevelCheckAnswer {
  return {
    cardId: question.cardId,
    answer:
      question.type === 'FILL_IN'
        ? { filledBlanks: draft.blanks }
        : { selectedChoices: draft.choices },
  }
}
