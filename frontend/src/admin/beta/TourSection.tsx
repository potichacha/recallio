import type { BetaTour } from '../../api/beta'
import { EmptyState } from '../../components/states/EmptyState'
import { HorizontalBars } from '../charts/HorizontalBars'
import { plural } from './format'
import { Section } from './Section'

/**
 * Le Tour : combien de comptes y ont validé une notion, puis, par sujet, le chapitre de la dernière page ouverte par
 * chacun. C'est une position, pas un abandon : rien ne dit si quelqu'un reprendra demain (ADR 0076).
 */
export function TourSection({ tour }: { tour: BetaTour }) {
  return (
    <Section id="beta-tour" title="Le Tour">
      <p className="text-ink-soft">
        <strong className="text-ink">{plural(tour.learners, 'compte')}</strong>{' '}
        {tour.learners > 1 ? 'ont' : 'a'} validé au moins une notion du Tour sur les trente jours.
      </p>
      {tour.stops.length === 0 ? (
        <EmptyState
          title="Personne n’a encore ouvert le Tour."
          text="Dès qu’un compte ouvre une page, tu verras ici dans quel chapitre chacun en est."
        />
      ) : (
        <>
          <p className="text-sm text-ink-muted">
            Où chacun en est, à ce jour : le chapitre de la dernière page qu’il a ouverte. Ce n’est
            pas un abandon, rien ne dit s’il reprendra demain.
          </p>
          {tour.stops.map((stop) => (
            <div
              key={stop.subject}
              className="flex flex-col gap-3 rounded-2xl border border-line bg-surface p-4 shadow-sm"
            >
              <h3 className="font-bold">{stop.title}</h3>
              <HorizontalBars
                title={`${stop.title} : comptes par chapitre du Tour`}
                rows={stop.chapters.map((chapter, index) => ({
                  key: `${index}-${chapter.chapterTitle}`,
                  label: chapter.chapterTitle,
                  hint: chapter.domainTitle,
                  value: chapter.accounts,
                  text: plural(chapter.accounts, 'compte'),
                }))}
              />
            </div>
          ))}
        </>
      )}
    </Section>
  )
}
