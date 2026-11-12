import { EmptyState } from '../components/states/EmptyState'
import { ErrorState } from '../components/states/ErrorState'
import { LoadingState } from '../components/states/LoadingState'
import { useState, type FormEvent } from 'react'
import { Link } from 'react-router'
import { useCreateChallenge, useGroupChallenges, type ChallengeSummary } from '../api/challenges'
import { ApiError } from '../api/errors'
import type { Group } from '../api/groups'
import { button } from '../components/ui/button'

const MIN_CARDS = 3
const MAX_CARDS = 20
const MIN_SECONDS = 10
const MAX_SECONDS = 120

const END = new Intl.DateTimeFormat('fr-FR', { dateStyle: 'long' })

/** Les défis du groupe (ADR 0020) : une série de QCM commune, avec un délai par carte. */
export function ChallengesPanel({ group }: { group: Group }) {
  const challenges = useGroupChallenges(group.id)

  return (
    <section
      aria-labelledby="challenges-title"
      className="flex flex-col gap-3 rounded-xl bg-surface p-4 shadow-sm"
    >
      <h2 id="challenges-title" className="text-lg font-semibold">
        Défis
      </h2>
      {challenges.isPending && <LoadingState label="Chargement des défis…" shape="line" />}
      {challenges.isError && (
        <ErrorState
          message="Impossible de charger les défis."
          onRetry={() => void challenges.refetch()}
        />
      )}
      {challenges.data?.length === 0 && (
        <EmptyState
          title="Aucun défi pour le moment."
          text="Un défi propose la même série de QCM à tous les membres, avec un temps limité par carte."
        />
      )}
      {challenges.data && challenges.data.length > 0 && (
        <ul className="flex flex-col gap-2">
          {challenges.data.map((challenge) => (
            <ChallengeRow key={challenge.id} challenge={challenge} />
          ))}
        </ul>
      )}
      {group.myRole === 'OWNER' && <CreateChallengeForm groupId={group.id} />}
    </section>
  )
}

function ChallengeRow({ challenge }: { challenge: ChallengeSummary }) {
  const action =
    challenge.myStatus === 'FINISHED'
      ? `Score : ${challenge.myScore} / ${challenge.cardCount}`
      : !challenge.open
        ? 'Terminé'
        : challenge.myStatus === 'IN_PROGRESS'
          ? 'Reprendre'
          : 'Jouer'
  return (
    <li>
      <Link
        to={`/challenges/${challenge.id}`}
        className="flex items-center justify-between gap-3 rounded-lg border border-line px-3 py-2 hover:border-brand-line"
      >
        <span className="flex flex-col">
          <span className="font-medium">{challenge.title}</span>
          <span className="text-xs text-ink-muted">
            {challenge.cardCount} QCM · {challenge.secondsPerCard} s par carte
            {challenge.open && ` · jusqu'au ${END.format(new Date(challenge.endsAt))}`}
          </span>
        </span>
        <span className="text-sm font-medium text-brand-strong">{action}</span>
      </Link>
    </li>
  )
}

function CreateChallengeForm({ groupId }: { groupId: string }) {
  const [title, setTitle] = useState('')
  const [cardCount, setCardCount] = useState(5)
  const [secondsPerCard, setSecondsPerCard] = useState(30)
  const create = useCreateChallenge(groupId)

  const submit = (event: FormEvent) => {
    event.preventDefault()
    create.mutate({ title, cardCount, secondsPerCard }, { onSuccess: () => setTitle('') })
  }

  return (
    <form
      onSubmit={submit}
      aria-label="Lancer un défi"
      className="flex flex-col gap-3 border-t border-line pt-3"
    >
      <label className="flex flex-col gap-1">
        <span className="text-sm font-medium">Titre du défi</span>
        <input
          value={title}
          onChange={(event) => setTitle(event.target.value)}
          maxLength={80}
          required
          className="rounded-lg border border-line-strong px-3 py-2"
        />
      </label>
      <div className="flex flex-wrap gap-4">
        <NumberField
          label="Nombre de QCM"
          value={cardCount}
          min={MIN_CARDS}
          max={MAX_CARDS}
          onChange={setCardCount}
        />
        <NumberField
          label="Secondes par carte"
          value={secondsPerCard}
          min={MIN_SECONDS}
          max={MAX_SECONDS}
          onChange={setSecondsPerCard}
        />
      </div>
      <button
        type="submit"
        disabled={title.trim() === '' || create.isPending}
        className={`self-start ${button()}`}
      >
        Lancer le défi
      </button>
      {create.isError && (
        <p role="alert">
          {create.error instanceof ApiError
            ? create.error.message
            : "Le défi n'a pas pu être lancé."}
        </p>
      )}
    </form>
  )
}

function NumberField({
  label,
  value,
  min,
  max,
  onChange,
}: {
  label: string
  value: number
  min: number
  max: number
  onChange: (value: number) => void
}) {
  return (
    <label className="flex flex-col gap-1">
      <span className="text-sm font-medium">{label}</span>
      <input
        type="number"
        min={min}
        max={max}
        required
        value={Number.isNaN(value) ? '' : value}
        onChange={(event) => onChange(event.target.valueAsNumber)}
        className="w-28 rounded-lg border border-line-strong px-3 py-2"
      />
    </label>
  )
}
