import { useState } from 'react'
import { Link } from 'react-router'
import { ApiError } from '../api/errors'
import { useProfile } from '../api/profile'
import {
  useContentReports,
  useResolveReport,
  type AdminContentReport,
  type ReportStatus,
} from '../api/reports'
import { REPORT_REASON_LABELS } from '../components/reportReasons'
import { tourPath } from '../tour/tourSteps'

const DATE = new Intl.DateTimeFormat('fr-FR', { dateStyle: 'medium', timeStyle: 'short' })

/**
 * Les signalements à traiter (ADR 0042), pour les admins : le serveur refuse les autres. Corriger se fait par une pull
 * request de contenu ; ici, on marque le signalement corrigé, ou rejeté si le contenu est juste.
 */
export function ContentReportsPage() {
  const profile = useProfile()
  const [status, setStatus] = useState<ReportStatus>('OPEN')
  const [page, setPage] = useState(0)
  const reports = useContentReports(status, page)

  if (profile.data && !profile.data.roles.includes('admin')) {
    return <p role="alert">Cette page est réservée aux admins.</p>
  }
  const choose = (next: ReportStatus) => {
    setStatus(next)
    setPage(0)
  }

  return (
    <section className="flex flex-col gap-4">
      <h1 className="text-2xl font-bold">Signalements</h1>
      <div role="group" aria-label="Signalements à afficher" className="flex gap-2">
        {(['OPEN', 'RESOLVED'] as const).map((option) => (
          <button
            key={option}
            type="button"
            aria-pressed={status === option}
            onClick={() => choose(option)}
            className={`rounded-lg px-3 py-1.5 text-sm font-medium ${
              status === option ? 'bg-indigo-600 text-white' : 'bg-white text-slate-700 shadow-sm'
            }`}
          >
            {option === 'OPEN' ? 'À traiter' : 'Traités'}
          </button>
        ))}
      </div>
      {reports.isPending && <p>Chargement des signalements…</p>}
      {reports.isError && <p role="alert">Impossible de charger les signalements.</p>}
      {reports.data && (
        <>
          <p className="text-sm text-slate-600">
            {reports.data.totalItems} signalement{reports.data.totalItems > 1 ? 's' : ''}
          </p>
          <ul className="flex flex-col gap-3">
            {reports.data.items.map((report) => (
              <ReportItem key={report.id} report={report} />
            ))}
          </ul>
          {reports.data.totalPages > 1 && (
            <nav aria-label="Pages" className="flex items-center gap-3 text-sm">
              <button type="button" disabled={page === 0} onClick={() => setPage(page - 1)}>
                Précédents
              </button>
              <span>
                Page {page + 1} sur {reports.data.totalPages}
              </span>
              <button
                type="button"
                disabled={page + 1 >= reports.data.totalPages}
                onClick={() => setPage(page + 1)}
              >
                Suivants
              </button>
            </nav>
          )}
        </>
      )}
    </section>
  )
}

function ReportItem({ report }: { report: AdminContentReport }) {
  const resolve = useResolveReport()
  return (
    <li className="flex flex-col gap-2 rounded-xl bg-white p-4 shadow-sm">
      <div className="flex flex-wrap items-baseline justify-between gap-2">
        <span className="font-medium">{REPORT_REASON_LABELS[report.reason]}</span>
        <span className="text-xs text-slate-500">{DATE.format(new Date(report.createdAt))}</span>
      </div>
      <p className="text-sm text-slate-600">
        <ContentLink report={report} />
      </p>
      {report.excerpt && (
        <blockquote className="border-l-2 border-slate-200 pl-3 text-sm text-slate-700">
          {report.excerpt}
        </blockquote>
      )}
      {report.comment && <p className="text-sm whitespace-pre-line">« {report.comment} »</p>}
      {report.resolution ? (
        <p className="text-sm font-medium text-slate-500">
          {report.resolution === 'FIXED' ? 'Corrigé' : 'Rejeté'}
          {report.resolvedAt && ` le ${DATE.format(new Date(report.resolvedAt))}`}
        </p>
      ) : (
        <div className="flex gap-2">
          <button
            type="button"
            disabled={resolve.isPending}
            onClick={() => resolve.mutate({ id: report.id, resolution: 'FIXED' })}
            className="rounded-lg bg-emerald-600 px-3 py-1.5 text-sm font-medium text-white hover:bg-emerald-700 disabled:opacity-50"
          >
            Corrigé
          </button>
          <button
            type="button"
            disabled={resolve.isPending}
            onClick={() => resolve.mutate({ id: report.id, resolution: 'REJECTED' })}
            className="rounded-lg border border-slate-300 px-3 py-1.5 text-sm font-medium text-slate-700 hover:bg-slate-50 disabled:opacity-50"
          >
            Rejeté
          </button>
        </div>
      )}
      {resolve.isError && (
        <p role="alert" className="text-sm text-rose-700">
          {resolve.error instanceof ApiError
            ? resolve.error.message
            : 'Le signalement n’a pas pu être traité.'}
        </p>
      )}
    </li>
  )
}

/** Où est le contenu signalé : la page du Tour, ou le thème de la carte dans le catalogue. */
function ContentLink({ report }: { report: AdminContentReport }) {
  const where = [report.subject, report.notionTitle].filter(Boolean).join(' · ')
  if (report.notion && report.page) {
    return (
      <Link
        to={tourPath(report.subject, { notion: report.notion, page: report.page })}
        className="text-indigo-700 hover:underline"
      >
        {where} · page {report.page} du Tour
      </Link>
    )
  }
  if (report.themeSlug) {
    return (
      <Link
        to={`/themes/${encodeURIComponent(report.themeSlug)}`}
        className="text-indigo-700 hover:underline"
      >
        {where} · carte
      </Link>
    )
  }
  return <>{where} · carte retirée depuis</>
}
