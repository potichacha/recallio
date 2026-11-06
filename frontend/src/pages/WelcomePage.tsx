import { useState } from 'react'
import { useNavigate } from 'react-router'
import { useCatalogSubjects, type Subject } from '../api/content'
import { useChooseTrack, useFollowSubjects, useProfile } from '../api/profile'
import { Chip, Choices } from '../components/ChoiceChips'
import { button } from '../components/ui/button'
import { SubjectLogo } from '../subjects/SubjectLogo'
import { useIsAdmin } from '../subjects/useIsAdmin'

type Goal = 'beginner' | 'language' | 'interview'

const GOALS: { goal: Goal; label: string; detail: string }[] = [
  {
    goal: 'beginner',
    label: 'Je n’ai jamais programmé',
    detail: 'On part des bases, pas à pas, en écrivant de vrais programmes.',
  },
  {
    goal: 'language',
    label: 'Je connais déjà un autre langage',
    detail: 'Apprends-en un nouveau avec son Tour, en codant à chaque page.',
  },
  {
    goal: 'interview',
    label: 'Je prépare un entretien',
    detail: 'Révise chaque jour les questions qui tombent en entretien.',
  },
]

/** Le sujet de qui n'a jamais programmé (ADR 0039) : proposé dès qu'il s'ouvre. */
const BEGINNER_SUBJECT = 'programmer-de-zero'

/**
 * Premier login (ADR 0052) : le but, puis le sujet. Le but règle le parcours et la première étape : qui apprend part
 * dans le Tour, en parcours « apprendre » (ADR 0032) ; qui prépare un entretien choisit la durée de sa première
 * session. Le sujet choisi est suivi : il entre au tableau de bord.
 */
export function WelcomePage() {
  const profile = useProfile()
  const subjects = useCatalogSubjects()
  const admin = useIsAdmin()

  return (
    <section className="flex flex-col gap-4">
      <h1 className="text-2xl font-bold">Qu’est-ce qui t’amène ?</h1>
      <p className="text-ink-muted">
        Dis-nous ton but : Recallio te propose par où commencer. Tu pourras tout changer ensuite.
      </p>
      {(profile.isPending || subjects.isPending) && <p>Chargement…</p>}
      {(profile.isError || subjects.isError) && (
        <p role="alert">Impossible de charger tes sujets.</p>
      )}
      {profile.isSuccess && subjects.isSuccess && (
        <WelcomeForm
          followed={profile.data.followedSubjects}
          subjects={subjects.data.filter((subject) => subject.status === 'PUBLISHED' || admin)}
        />
      )}
    </section>
  )
}

function WelcomeForm({ followed, subjects }: { followed: string[]; subjects: Subject[] }) {
  const [goal, setGoal] = useState<Goal>()
  const [picked, setPicked] = useState<string>()
  const follow = useFollowSubjects()
  const chooseTrack = useChooseTrack()
  const navigate = useNavigate()

  const goals = GOALS.filter(
    ({ goal: value }) =>
      value !== 'beginner' || subjects.some((subject) => subject.slug === BEGINNER_SUBJECT),
  )
  const candidates = subjects.filter(
    (subject) =>
      subject.slug !== BEGINNER_SUBJECT &&
      (goal === 'language' ? subject.hasTour : subject.cardCount > 0),
  )
  const subject =
    goal === 'beginner'
      ? BEGINNER_SUBJECT
      : candidates.find((candidate) => candidate.slug === picked)?.slug

  const start = async () => {
    if (!goal || !subject) return
    try {
      await follow.mutateAsync([...new Set([...followed, subject])])
      if (goal !== 'interview') await chooseTrack.mutateAsync({ subject, track: 'LEARN' })
    } catch {
      // L'erreur est dite sous le bouton : on reste sur la page pour réessayer.
      return
    }
    const slug = encodeURIComponent(subject)
    navigate(goal === 'interview' ? `/review?subject=${slug}` : `/subjects/${slug}/tour`)
  }

  return (
    <>
      <Choices legend="Ton but">
        {goals.map(({ goal: value, label, detail }) => (
          <Chip key={value} name="goal" checked={goal === value} onChange={() => setGoal(value)}>
            <span className="font-medium">{label}</span>
            <span className="text-xs text-ink-muted">{detail}</span>
          </Chip>
        ))}
      </Choices>
      {goal !== undefined && goal !== 'beginner' && (
        <Choices
          legend={goal === 'language' ? 'Quel langage veux-tu apprendre ?' : 'Sur quel sujet ?'}
        >
          {candidates.map((candidate) => (
            <Chip
              key={candidate.slug}
              name="subject"
              checked={subject === candidate.slug}
              onChange={() => setPicked(candidate.slug)}
            >
              <span className="flex items-center gap-3">
                <SubjectLogo subject={candidate} />
                <span className="font-medium">{candidate.title}</span>
              </span>
            </Chip>
          ))}
        </Choices>
      )}
      <button
        type="button"
        onClick={start}
        disabled={!subject || follow.isPending || chooseTrack.isPending}
        className={`self-start ${button()}`}
      >
        C’est parti
      </button>
      {(follow.isError || chooseTrack.isError) && (
        <p role="alert">Ton choix n’a pas pu être enregistré.</p>
      )}
    </>
  )
}
