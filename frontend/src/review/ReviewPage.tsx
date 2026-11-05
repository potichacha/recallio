import { useState } from 'react'
import { useSearchParams } from 'react-router'
import { useSubjects, type CardType } from '../api/content'
import { useGroup } from '../api/groups'
import type { ReviewFilter } from '../api/review'
import { RequireFollowedSubjects } from '../auth/RequireFollowedSubjects'
import { CARD_TYPE_LABELS } from '../components/cardTypeLabels'
import { ReviewSession } from './ReviewSession'
import { SessionSetup } from './SessionSetup'
import { SESSION_MINUTES, sessionTypes, type SessionSettings } from './sessionSettings'

/**
 * Page de révision. On y lance une session sur un sujet à la fois, pour une durée choisie (ADR 0048) : un sujet
 * (`?subject=go`) est proposé d'emblée, même s'il n'est pas suivi ; avec sa durée et ses questions
 * (`?subject=go&minutes=10&mode=all`, le bouton « Continuer » de l'accueil), la session démarre aussitôt (ADR 0050). Un
 * groupe (`?group=...`, selon ses réglages), un thème (`?theme=...`) ou un format (`?type=CODING_EXERCISE`) ouvrent
 * directement une session sans limite de durée.
 */
export function ReviewPage() {
  const [searchParams] = useSearchParams()
  const subject = searchParams.get('subject') ?? undefined
  const direct = directFilter(searchParams)

  // Changer de lien démarre une nouvelle session : la position et les compteurs repartent de zéro.
  if (direct) {
    return <DirectSession key={JSON.stringify(direct)} filter={direct} />
  }
  const ready = readySettings(searchParams)
  return <SubjectSession key={searchParams.toString()} subject={subject} ready={ready} />
}

/** Un sujet, une durée et des questions donnés par le lien : la session n'a plus rien à demander. */
function readySettings(params: URLSearchParams): SessionSettings | undefined {
  const subject = params.get('subject')
  const minutes = params.get('minutes')
  const mode = params.get('mode')
  if (!subject || minutes === null || (mode !== 'all' && mode !== 'graded')) return undefined
  if (minutes === 'none') return { subject, minutes: null, mode }
  const timed = SESSION_MINUTES.find((allowed) => allowed === Number(minutes))
  return timed === undefined ? undefined : { subject, minutes: timed, mode }
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
function SubjectSession({ subject, ready }: { subject?: string; ready?: SessionSettings }) {
  const subjects = useSubjects()
  const [session, setSession] = useState<{ settings: SessionSettings; run: number } | undefined>(
    ready && { settings: ready, run: 0 },
  )

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
      onMore={() => setSession({ settings: { ...settings, minutes: 5 }, run: run + 1 })}
      onChangeSettings={() => setSession(undefined)}
    />
  )
}
