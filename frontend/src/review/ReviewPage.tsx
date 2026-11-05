import { useState } from 'react'
import { useSearchParams } from 'react-router'
import { useSubjects, type CardType } from '../api/content'
import { useGroup } from '../api/groups'
import type { ReviewFilter } from '../api/review'
import { RequireFollowedSubjects } from '../auth/RequireFollowedSubjects'
import { CARD_TYPE_LABELS } from '../components/cardTypeLabels'
import { ReviewSession } from './ReviewSession'
import { SessionSetup } from './SessionSetup'
import { sessionTypes, type SessionSettings } from './sessionSettings'

/**
 * Page de révision. On y lance une session sur un sujet à la fois, pour une durée choisie (ADR 0048) : un sujet
 * (`?subject=go`) est proposé d'emblée, même s'il n'est pas suivi. Un groupe (`?group=...`, selon ses réglages), un thème
 * (`?theme=...`) ou un format (`?type=CODING_EXERCISE`) ouvrent directement une session sans limite de durée.
 */
export function ReviewPage() {
  const [searchParams] = useSearchParams()
  const subject = searchParams.get('subject') ?? undefined
  const direct = directFilter(searchParams)

  // Changer de lien démarre une nouvelle session : la position et les compteurs repartent de zéro.
  if (direct) {
    return <DirectSession key={JSON.stringify(direct)} filter={direct} />
  }
  return <SubjectSession key={subject ?? ''} subject={subject} />
}

/** Un groupe, un thème ou un format précis : ce qu'il faut réviser est déjà dit par le lien. */
function directFilter(params: URLSearchParams): ReviewFilter | undefined {
  const group = params.get('group') ?? undefined
  const theme = params.get('theme') ?? undefined
  const type = cardType(params.get('type'))
  if (!group && !theme && !type) return undefined
  return {
    group,
    theme,
    subject: params.get('subject') ?? undefined,
    types: type ? [type] : undefined,
  }
}

/** Un format connu, ou aucun : un format inconnu dans l'URL est ignoré plutôt que refusé par l'API. */
function cardType(value: string | null): CardType | undefined {
  return value !== null && value in CARD_TYPE_LABELS ? (value as CardType) : undefined
}

function DirectSession({ filter }: { filter: ReviewFilter }) {
  const [run, setRun] = useState(0)
  return (
    <ReviewSession
      key={run}
      title={filter.group ? <GroupSessionTitle groupId={filter.group} /> : 'Réviser'}
      filter={filter}
      minutes={null}
      onRestart={() => setRun((current) => current + 1)}
    />
  )
}

function GroupSessionTitle({ groupId }: { groupId: string }) {
  const group = useGroup(groupId)
  return group.data ? `Réviser avec « ${group.data.name} »` : 'Réviser avec le groupe'
}

/** Le lancement d'une session, puis la session, puis, au bilan, une autre session ou d'autres réglages. */
function SubjectSession({ subject }: { subject?: string }) {
  const subjects = useSubjects()
  const [session, setSession] = useState<{ settings: SessionSettings; run: number }>()

  if (!session) {
    const setup = (
      <SessionSetup subject={subject} onStart={(settings) => setSession({ settings, run: 0 })} />
    )
    return subject ? setup : <RequireFollowedSubjects>{setup}</RequireFollowedSubjects>
  }

  const { settings, run } = session
  const subjectTitle =
    subjects.data?.find((candidate) => candidate.slug === settings.subject)?.title ??
    settings.subject
  return (
    <ReviewSession
      key={run}
      title={`Réviser : ${subjectTitle}`}
      subjectTitle={subjectTitle}
      filter={{ subject: settings.subject, types: sessionTypes(settings.mode) }}
      minutes={settings.minutes}
      onRestart={() => setSession({ settings, run: run + 1 })}
      onChangeSettings={() => setSession(undefined)}
    />
  )
}
