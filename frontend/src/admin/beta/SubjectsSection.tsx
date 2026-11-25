import type { BetaSubject } from '../../api/beta'
import { EmptyState } from '../../components/states/EmptyState'
import { ScrollTable } from '../charts/ScrollTable'
import { formatNumber, formatPercent } from './format'
import { Section } from './Section'

const CELL = 'px-3 py-2 text-right tabular-nums'

/** Les sujets, le plus révisé d'abord : qui les suit, qui les révise, et la part de réponses justes. */
export function SubjectsSection({ subjects }: { subjects: BetaSubject[] }) {
  return (
    <Section id="beta-subjects" title="Sujets">
      {subjects.length === 0 ? (
        <EmptyState
          title="Aucun sujet à montrer."
          text="Chaque sujet publié s’affichera ici, avec les comptes qui le suivent et le révisent."
        />
      ) : (
        <>
          <p className="text-sm text-ink-muted">
            Les suivis se comptent à ce jour ; le reste, sur les trente jours. Sans réponse, pas de
            taux de réussite.
          </p>
          <ScrollTable
            caption="Sujets"
            head={['Sujet', 'Suivis', 'Comptes qui révisent', 'Réponses', 'Réussite']}
          >
            {subjects.map((subject) => (
              <tr key={subject.subject} className="border-b border-line last:border-b-0">
                <th scope="row" className="px-3 py-2 text-left font-semibold">
                  {subject.title ?? subject.subject}
                </th>
                <td className={CELL}>{formatNumber(subject.followers)}</td>
                <td className={CELL}>{formatNumber(subject.reviewers)}</td>
                <td className={CELL}>{formatNumber(subject.reviews)}</td>
                <td className={CELL}>{formatPercent(subject.successPercent)}</td>
              </tr>
            ))}
          </ScrollTable>
        </>
      )}
    </Section>
  )
}
