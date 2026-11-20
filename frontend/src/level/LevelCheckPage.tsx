import { CheckCircle, ListNumbers, ArrowUUpLeft } from '@phosphor-icons/react'
import { useState } from 'react'
import { Link, useParams } from 'react-router'
import { ApiError } from '../api/errors'
import { useLevelCheck, type LevelCheck, type LevelCheckDomainResult } from '../api/levelChecks'
import { EmptyState } from '../components/states/EmptyState'
import { ErrorState } from '../components/states/ErrorState'
import { LoadingState } from '../components/states/LoadingState'
import { button } from '../components/ui/button'
import { LevelCheckDomainStep } from './LevelCheckDomainStep'
import { LevelCheckResult } from './LevelCheckResult'
import { isOver, notionCount, type DomainVerdict } from './levelCheckRun'

/**
 * Le test de niveau d'un sujet (ADR 0068) : deux questions par domaine des bases, posés un par un, pour sauter dans le
 * Tour et en révision ce qu'on sait déjà. Deux domaines ratés de suite arrêtent le test. Réservé aux comptes : les
 * notions connues sont à l'utilisateur.
 */
export function LevelCheckPage() {
  const { slug = '' } = useParams()
  const check = useLevelCheck(slug)
  const [cancelled, setCancelled] = useState(false)

  return (
    <section className="flex flex-col gap-5">
      <h1 className="text-2xl font-bold">
        Test de niveau{check.data ? ` : ${check.data.subjectTitle}` : ''}
      </h1>
      {cancelled && (
        <p role="status" className="rounded-xl bg-surface-muted px-4 py-3 text-ink-soft">
          Test annulé : le Tour et tes révisions reprennent toutes les bases.
        </p>
      )}
      <Body
        subject={slug}
        check={check}
        onCancelled={() => setCancelled(true)}
        onStart={() => setCancelled(false)}
      />
    </section>
  )
}

function Body({
  subject,
  check,
  onCancelled,
  onStart,
}: {
  subject: string
  check: ReturnType<typeof useLevelCheck>
  onCancelled: () => void
  onStart: () => void
}) {
  if (check.error instanceof ApiError && [404, 422].includes(check.error.status)) {
    return (
      <EmptyState
        title="Pas de test de niveau pour ce sujet"
        text={check.error.message}
        mood="thinking"
      >
        <Link to="/tours" className={button({ variant: 'secondary', size: 'sm' })}>
          Voir les Tours
        </Link>
      </EmptyState>
    )
  }
  if (check.isError) {
    return (
      <ErrorState
        message="Impossible de charger le test de niveau."
        onRetry={() => void check.refetch()}
      />
    )
  }
  if (!check.data) return <LoadingState label="Chargement du test…" shape="card" />
  // Un nouveau tirage (après l'annulation) repart de l'introduction.
  return (
    <LevelCheckRun
      key={check.dataUpdatedAt}
      subject={subject}
      check={check.data}
      onCancelled={onCancelled}
      onStart={onStart}
    />
  )
}

/** Le déroulé du test : l'introduction, les domaines un par un, puis le résultat. */
function LevelCheckRun({
  subject,
  check,
  onCancelled,
  onStart,
}: {
  subject: string
  check: LevelCheck
  onCancelled: () => void
  onStart: () => void
}) {
  const [started, setStarted] = useState(false)
  const [verdicts, setVerdicts] = useState<DomainVerdict[]>([])
  const [knownNotions, setKnownNotions] = useState(check.knownNotions)
  const domainCount = check.domains.length

  if (!started) {
    return (
      <Intro
        check={check}
        onStart={() => {
          onStart()
          setStarted(true)
        }}
      />
    )
  }
  if (isOver(verdicts, domainCount)) {
    return (
      <LevelCheckResult
        subject={subject}
        check={check}
        verdicts={verdicts}
        knownNotions={knownNotions}
        onCancelled={onCancelled}
      />
    )
  }
  const domain = check.domains[verdicts.length]
  const next = (result: LevelCheckDomainResult) => {
    setVerdicts([...verdicts, { domain: result.domain, passed: result.passed }])
    setKnownNotions(result.knownNotions)
  }
  return (
    <LevelCheckDomainStep
      key={domain.id}
      subject={subject}
      domain={domain}
      rank={verdicts.length + 1}
      domainCount={domainCount}
      isLast={(passed) => isOver([...verdicts, { domain: domain.id, passed }], domainCount)}
      onNext={next}
    />
  )
}

/** Ce que le test pose et ce qu'il change, avant la première question. */
function Intro({ check, onStart }: { check: LevelCheck; onStart: () => void }) {
  const rules = [
    {
      icon: ListNumbers,
      text: `${check.domains.length} domaines des bases, environ cinq minutes si tu les connais tous.`,
    },
    {
      icon: CheckCircle,
      text: 'Un domaine réussi, c’est toutes ses réponses justes : ses notions sont sautées.',
    },
    {
      icon: ArrowUUpLeft,
      text: 'Tu pourras annuler le test à la fin, si tu t’es surestimé.',
    },
  ]
  return (
    <div className="flex flex-col items-start gap-4 rounded-2xl border border-line bg-surface p-5 shadow-sm">
      <p>
        Tu connais déjà un peu {check.subjectTitle} ? Deux questions par domaine des bases ; deux
        domaines ratés de suite arrêtent le test. Chaque domaine réussi est sauté dans le Tour et
        passe après le reste en révision.
      </p>
      <ul className="flex flex-col gap-2">
        {rules.map(({ icon: Icon, text }) => (
          <li key={text} className="flex items-center gap-3 text-ink-soft">
            <Icon weight="duotone" aria-hidden className="size-6 shrink-0 text-brand" />
            {text}
          </li>
        ))}
      </ul>
      {check.knownNotions > 0 && (
        <p className="text-sm text-ink-muted">
          Tu as déjà passé ce test : {notionCount(check.knownNotions)} sautée
          {check.knownNotions > 1 ? 's' : ''}. Le refaire n’enlève rien.
        </p>
      )}
      <button type="button" onClick={onStart} className={button()}>
        Commencer le test
      </button>
    </div>
  )
}
