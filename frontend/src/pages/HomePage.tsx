import { useApiStatus } from '../api/health'

export function HomePage() {
  const status = useApiStatus()

  return (
    <main className="mx-auto flex min-h-dvh max-w-md flex-col justify-center gap-6 p-6">
      <h1 className="text-4xl font-bold text-indigo-600">Recallio</h1>
      <p className="text-slate-600">Révisez vos entretiens techniques, un peu chaque jour.</p>
      <p role="status" className="rounded-lg bg-slate-100 px-4 py-3 text-sm text-slate-700">
        {status.isPending && 'API : vérification…'}
        {status.isSuccess && `API : ${status.data}`}
        {status.isError && 'API : injoignable'}
      </p>
    </main>
  )
}
