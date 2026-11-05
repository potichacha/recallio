import { useState } from 'react'
import { useNavigate } from 'react-router'
import { useFollowSubjects, useProfile } from '../api/profile'
import { Chip, Choices } from '../components/ChoiceChips'
import { SubjectPicker } from '../components/SubjectPicker'
import { button } from '../components/ui/button'
import {
  DEFAULT_MINUTES,
  DEFAULT_MODE,
  saveSessionSettings,
  SESSION_MINUTES,
  type TimedMinutes,
} from '../review/sessionSettings'

/**
 * Premier login (ADR 0050) : les sujets à réviser, puis le temps à y passer chaque jour. La première session part
 * aussitôt, sur le premier sujet choisi : on révise dès la première minute, plutôt que de remplir des réglages.
 */
export function WelcomePage() {
  const profile = useProfile()

  return (
    <section className="flex flex-col gap-4">
      <h1 className="text-2xl font-bold">Quels sujets veux-tu réviser ?</h1>
      <p className="text-ink-muted">
        Tes révisions puiseront dans ces sujets. Tu pourras en changer à tout moment dans ton
        profil.
      </p>
      {profile.isPending && <p>Chargement…</p>}
      {profile.isError && <p role="alert">Impossible de charger ton profil.</p>}
      {profile.isSuccess && <WelcomeForm initial={profile.data.followedSubjects} />}
    </section>
  )
}

function WelcomeForm({ initial }: { initial: string[] }) {
  const [selected, setSelected] = useState(initial)
  const [minutes, setMinutes] = useState<TimedMinutes>(DEFAULT_MINUTES)
  const follow = useFollowSubjects()
  const navigate = useNavigate()

  const start = () => {
    const subject = selected.at(0)
    if (!subject) return
    follow.mutate(selected, {
      onSuccess: () => {
        const settings = { subject, minutes, mode: DEFAULT_MODE }
        saveSessionSettings(settings)
        navigate(`/review?${new URLSearchParams({ ...settings, minutes: String(minutes) })}`)
      },
    })
  }

  return (
    <>
      <SubjectPicker selected={selected} onChange={setSelected} />
      <Choices legend="Combien de temps par jour ?">
        {SESSION_MINUTES.map((value) => (
          <Chip
            key={value}
            name="daily-minutes"
            checked={minutes === value}
            onChange={() => setMinutes(value)}
          >
            {value} min
          </Chip>
        ))}
      </Choices>
      <p className="text-sm text-ink-muted">
        Ce sera la durée de tes sessions ; tu pourras la changer avant chacune.
      </p>
      <button
        type="button"
        onClick={start}
        disabled={selected.length === 0 || follow.isPending}
        className={`self-start ${button()}`}
      >
        Commencer à réviser
      </button>
      {follow.isError && <p role="alert">Tes sujets n’ont pas pu être enregistrés.</p>}
    </>
  )
}
