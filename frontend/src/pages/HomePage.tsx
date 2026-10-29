import { Link } from 'react-router'
import { useApiStatus } from '../api/health'

export function HomePage() {
  const status = useApiStatus()

  return (
    <section className="flex flex-col gap-6 py-10">
      <h1 className="text-4xl font-bold text-indigo-600">Recallio</h1>
      <p className="text-slate-600">Révisez vos entretiens techniques, un peu chaque jour.</p>
      <div className="flex flex-wrap gap-3">
        <Link
          to="/review"
          className="rounded-lg bg-indigo-600 px-4 py-2 font-medium text-white hover:bg-indigo-700"
        >
          Réviser maintenant
        </Link>
        <Link
          to="/themes"
          className="rounded-lg border border-indigo-200 px-4 py-2 font-medium text-indigo-700 hover:bg-indigo-50"
        >
          Parcourir les thèmes
        </Link>
      </div>
      <p role="status" className="rounded-lg bg-slate-100 px-4 py-3 text-sm text-slate-700">
        {status.isPending && 'API : vérification…'}
        {status.isSuccess && `API : ${status.data}`}
        {status.isError && 'API : injoignable'}
      </p>
    </section>
  )
}
