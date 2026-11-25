import type { BetaFeatures } from '../../api/beta'
import { EmptyState } from '../../components/states/EmptyState'
import { HorizontalBars } from '../charts/HorizontalBars'
import { USAGE_FEATURE_LABELS } from '../usageFeatureLabels'
import { formatPercent, plural } from './format'
import { Section } from './Section'

const FULL = 100

/**
 * L'usage de chaque fonctionnalité, en part des comptes actifs. Dix sont datées et bornées aux trente jours ; quatre
 * sont des états sans histoire (un rappel accepté, un profil public), comptés « à ce jour » parmi les comptes actifs.
 */
export function FeaturesSection({ features }: { features: BetaFeatures }) {
  return (
    <Section id="beta-features" title="Fonctionnalités">
      {features.activeAccounts === 0 ? (
        <EmptyState
          title="Aucun compte actif sur ces trente jours."
          text="Dès qu’un compte révise, valide une notion du Tour ou joue, tu verras ici la part des comptes actifs qui utilise chaque fonctionnalité."
        />
      ) : (
        <>
          <p className="text-sm text-ink-muted">
            La part des {plural(features.activeAccounts, 'compte')}{' '}
            {features.activeAccounts > 1 ? 'actifs' : 'actif'} qui a utilisé chaque fonctionnalité
            au moins une fois sur les trente jours. « À ce jour » : l’usage n’est pas daté, on
            compte les comptes actifs qui l’ont aujourd’hui.
          </p>
          <div className="rounded-2xl border border-line bg-surface p-4 shadow-sm">
            <HorizontalBars
              title="Part des comptes actifs par fonctionnalité"
              max={FULL}
              rows={features.features.map(({ feature, accounts, percent, dated }) => ({
                key: feature,
                label: USAGE_FEATURE_LABELS[feature],
                hint: dated ? undefined : 'à ce jour',
                value: percent ?? 0,
                text: `${plural(accounts, 'compte')} · ${formatPercent(percent)}`,
              }))}
            />
          </div>
        </>
      )}
    </Section>
  )
}
