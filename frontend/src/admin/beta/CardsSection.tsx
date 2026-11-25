import { Link } from 'react-router'
import type { BetaCard, BetaFailedCard, BetaReportedCard } from '../../api/beta'
import { CARD_TYPE_LABELS } from '../../components/cardTypeLabels'
import { EmptyState } from '../../components/states/EmptyState'
import { TEXT_LINK } from '../../legal/textLink'
import { ScrollTable } from '../charts/ScrollTable'
import { formatNumber } from './format'
import { Section } from './Section'

const CELL = 'px-3 py-2 text-right align-top tabular-nums'

/** Les cartes au plus fort taux d'échec : une question mal posée ou une notion mal expliquée se voit ici. */
export function FailedCardsSection({ cards }: { cards: BetaFailedCard[] }) {
  return (
    <Section id="beta-failed" title="Cartes les plus ratées">
      {cards.length === 0 ? (
        <EmptyState
          title="Aucune carte assez jouée."
          text="Une carte entre ici après cinq réponses sur les trente jours, dont au moins une fausse."
        />
      ) : (
        <>
          <p className="text-sm text-ink-muted">
            Parmi les cartes qui ont reçu au moins cinq réponses sur les trente jours, la plus ratée
            d’abord.
          </p>
          <ScrollTable
            caption="Cartes les plus ratées"
            head={['Carte', 'Réponses', 'Échecs', 'Signalements ouverts']}
          >
            {cards.map(({ card, answers, failures, failurePercent, openReports }) => (
              <tr key={card.cardId} className="border-b border-line last:border-b-0">
                <th scope="row" className="min-w-48 px-3 py-2 text-left align-top font-normal">
                  <CardCell card={card} />
                </th>
                <td className={CELL}>{formatNumber(answers)}</td>
                <td className={CELL}>
                  <span className="block font-bold">{failurePercent} %</span>
                  <span className="block text-xs whitespace-nowrap text-ink-muted">
                    {formatNumber(failures)} sur {formatNumber(answers)}
                  </span>
                </td>
                <td className={CELL}>{formatNumber(openReports)}</td>
              </tr>
            ))}
          </ScrollTable>
        </>
      )}
    </Section>
  )
}

/** Les cartes où les signalements s'accumulent ; ils se traitent un par un sur la page des signalements. */
export function ReportedCardsSection({ cards }: { cards: BetaReportedCard[] }) {
  return (
    <Section id="beta-reported" title="Cartes les plus signalées">
      {cards.length === 0 ? (
        <EmptyState
          title="Aucun signalement ouvert."
          text="Les cartes que les testeurs signalent s’afficheront ici, la plus signalée d’abord."
        />
      ) : (
        <ScrollTable caption="Cartes les plus signalées" head={['Carte', 'Signalements ouverts']}>
          {cards.map(({ card, openReports }) => (
            <tr key={card.cardId} className="border-b border-line last:border-b-0">
              <th scope="row" className="px-3 py-2 text-left align-top font-normal">
                <CardCell card={card} />
              </th>
              <td className={CELL}>{formatNumber(openReports)}</td>
            </tr>
          ))}
        </ScrollTable>
      )}
      <p className="text-sm text-ink-muted">
        Pour les lire et les traiter un par un :{' '}
        <Link to="/admin/reports" className={TEXT_LINK}>
          signalements à traiter
        </Link>
        .
      </p>
    </Section>
  )
}

/**
 * De quoi reconnaître une carte : son sujet et sa notion, le début de sa question, son format. Une carte retirée, ou
 * d'un sujet qui n'est plus publié, n'a plus que son sujet.
 */
function CardCell({ card }: { card: BetaCard }) {
  const where = [card.subjectTitle ?? card.subject, card.notionTitle].filter(Boolean).join(' · ')
  if (!card.excerpt) {
    return (
      <span className="text-ink-muted">
        {where ? `${where} · ` : ''}carte retirée ou sujet dépublié
      </span>
    )
  }
  return (
    <span className="flex flex-col gap-0.5">
      <span className="text-xs font-semibold text-ink-muted">
        {where}
        {card.type && ` · ${CARD_TYPE_LABELS[card.type]}`}
      </span>
      {card.themeSlug ? (
        <Link to={`/themes/${encodeURIComponent(card.themeSlug)}`} className={TEXT_LINK}>
          {card.excerpt}
        </Link>
      ) : (
        <span className="font-semibold">{card.excerpt}</span>
      )}
    </span>
  )
}
