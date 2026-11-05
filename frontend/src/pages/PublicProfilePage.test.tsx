import { screen, within } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import type { PublicProfile } from '../api/publicProfile'
import { renderAt, stubApi } from '../test/render'

const PROFILE: PublicProfile = {
  handle: 'alice-dev',
  displayName: 'Alice',
  streak: 12,
  subjects: [
    {
      slug: 'go',
      title: 'Go',
      level: 4,
      xp: 820,
      verified: { exercisesPassed: 3, correctAnswers: 41, tourNotionsValidated: 1 },
      rank: { tier: 'GOLD', division: 2 },
      declared: { notions: 223, acquired: 60, mastered: 12, expert: 2, readinessPercent: 34 },
    },
  ],
  achievements: [{ achievement: 'STREAK_7', unlockedAt: '2026-11-01T08:00:00Z' }],
}

describe('PublicProfilePage', () => {
  it('shows what the server verified apart from what the user declares, without an account', async () => {
    stubApi({ '/api/v1/public-profiles/alice-dev': { body: PROFILE } })
    renderAt('/u/alice-dev')

    expect(await screen.findByRole('heading', { name: 'Alice' })).toBeInTheDocument()
    expect(screen.getByText(/série de 12 jours/)).toBeInTheDocument()
    const goVerified = screen.getByLabelText('Go : vérifié')
    expect(goVerified).toHaveTextContent('Classé : Or II')
    expect(goVerified).toHaveTextContent('3 exercices réussis')
    expect(goVerified).toHaveTextContent('41 réponses corrigées justes')
    expect(goVerified).toHaveTextContent('1 notion du Tour validée')
    const goDeclared = screen.getByLabelText('Go : déclaré')
    expect(goDeclared).toHaveTextContent('60 / 223 notions acquises, dont 12 maîtrisées')
    expect(goDeclared).toHaveTextContent('Prêt pour l’entretien à 34 %')
    const achievements = screen.getByRole('region', { name: 'Succès' })
    expect(within(achievements).getByText(/Une semaine/)).toBeInTheDocument()
  })

  it('says when the profile does not exist or is private', async () => {
    stubApi({ '/api/v1/public-profiles/bob': { status: 404, body: { code: 'x', detail: 'x' } } })
    renderAt('/u/bob')

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'Ce profil n’existe pas, ou il est privé.',
    )
  })
})
