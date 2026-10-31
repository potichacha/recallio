import { useState } from 'react'
import { useNavigate } from 'react-router'
import { useFollowSubjects, useProfile } from '../api/profile'
import { SubjectPicker } from '../components/SubjectPicker'

/** Premier login : l'utilisateur choisit les sujets qu'il veut réviser, parmi tous ceux qui sont publiés. */
export function WelcomePage() {
  const profile = useProfile()

  return (
    <section className="flex flex-col gap-4">
      <h1 className="text-2xl font-bold">Quels sujets voulez-vous réviser ?</h1>
      <p className="text-slate-600">
        Vos révisions puiseront dans ces sujets. Vous pourrez en changer à tout moment dans votre
        profil.
      </p>
      {profile.isPending && <p>Chargement…</p>}
      {profile.isError && <p role="alert">Impossible de charger le profil.</p>}
      {profile.isSuccess && <WelcomeForm initial={profile.data.followedSubjects} />}
    </section>
  )
}

function WelcomeForm({ initial }: { initial: string[] }) {
  const [selected, setSelected] = useState(initial)
  const follow = useFollowSubjects()
  const navigate = useNavigate()

  const start = () => follow.mutate(selected, { onSuccess: () => navigate('/review') })

  return (
    <>
      <SubjectPicker selected={selected} onChange={setSelected} />
      <button
        type="button"
        onClick={start}
        disabled={selected.length === 0 || follow.isPending}
        className="self-start rounded-lg bg-indigo-600 px-4 py-2 font-medium text-white hover:bg-indigo-700 disabled:opacity-50"
      >
        Commencer à réviser
      </button>
      {follow.isError && <p role="alert">Vos sujets n'ont pas pu être enregistrés.</p>}
    </>
  )
}
