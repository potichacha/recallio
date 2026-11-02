import { useState } from 'react'
import { Link, useParams } from 'react-router'
import { useSubjects } from '../api/content'
import { LIVE_CLOSE, useLiveGameSocket, type LiveGame } from '../api/live'
import { useProfile } from '../api/profile'

/** Une partie live (ADR 0021) : la salle d'attente, mise à jour en direct à chaque arrivée et départ. */
export function LivePage() {
  const { gameId = '' } = useParams()
  // Chaque tentative remonte la connexion : un état neuf, un nouveau WebSocket.
  const [attempt, setAttempt] = useState(0)
  return (
    <LiveConnectionView
      key={attempt}
      gameId={gameId}
      onReconnect={() => setAttempt((count) => count + 1)}
    />
  )
}

function LiveConnectionView({ gameId, onReconnect }: { gameId: string; onReconnect: () => void }) {
  const connection = useLiveGameSocket(gameId)

  if (connection.status === 'connecting') return <p>Connexion à la partie…</p>
  if (connection.status === 'closed') {
    if (connection.code === LIVE_CLOSE.NOT_FOUND) {
      return (
        <ClosedMessage
          title="Partie introuvable"
          detail="Elle est terminée, ou elle n'appartient pas à l'un de vos groupes."
        />
      )
    }
    if (connection.code === LIVE_CLOSE.CONFLICT) {
      return <ClosedMessage title="Partie complète" detail={connection.reason} />
    }
    return (
      <section className="flex flex-col items-start gap-3">
        <h1 className="text-2xl font-bold">Connexion perdue</h1>
        <button
          type="button"
          onClick={onReconnect}
          className="rounded-lg bg-indigo-600 px-4 py-2 font-medium text-white hover:bg-indigo-700"
        >
          Se reconnecter
        </button>
      </section>
    )
  }
  return <Lobby game={connection.game} />
}

function ClosedMessage({ title, detail }: { title: string; detail: string }) {
  return (
    <section className="flex flex-col items-start gap-3">
      <h1 className="text-2xl font-bold">{title}</h1>
      <p className="text-slate-600">{detail}</p>
      <Link to="/groups" className="text-indigo-600 hover:underline">
        Mes groupes
      </Link>
    </section>
  )
}

function Lobby({ game }: { game: LiveGame }) {
  const profile = useProfile()
  const subjects = useSubjects()
  const subjectTitle =
    subjects.data?.find((subject) => subject.slug === game.subject)?.title ?? game.subject
  const me = profile.data?.id
  const iAmHost = me !== undefined && me === game.hostId

  return (
    <section className="flex flex-col gap-4">
      <Link to={`/groups/${game.groupId}`} className="text-sm text-indigo-600 hover:underline">
        ← Le groupe
      </Link>
      <h1 className="text-2xl font-bold">Partie live : {subjectTitle}</h1>
      <p className="text-slate-600">
        {iAmHost
          ? 'Vous êtes l’hôte : vous lancerez la partie quand tout le monde sera là.'
          : 'En attente du lancement par l’hôte.'}
      </p>
      <section
        aria-labelledby="players-title"
        className="flex flex-col gap-2 rounded-xl bg-white p-4 shadow-sm"
      >
        <h2 id="players-title" className="text-lg font-semibold">
          Joueurs ({game.players.length})
        </h2>
        <ul className="flex flex-col gap-2">
          {game.players.map((player) => (
            <li
              key={player.userId}
              className="flex items-center gap-2 rounded-lg bg-slate-50 px-3 py-2"
            >
              {player.displayName}
              {player.userId === me && <span className="text-slate-500">(vous)</span>}
              {player.host && (
                <span className="rounded bg-indigo-50 px-1.5 py-0.5 text-xs font-medium text-indigo-700">
                  Hôte
                </span>
              )}
            </li>
          ))}
        </ul>
      </section>
    </section>
  )
}
