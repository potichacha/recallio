import { afterEach, describe, expect, it, vi } from 'vitest'
import bootScript from '../../public/theme.js?raw'
import {
  applyTheme,
  loadThemeChoice,
  resolveTheme,
  saveThemeChoice,
  THEME_STORAGE_KEY,
} from './theme'

/** Le système de l'utilisateur, en clair ou en sombre. */
function systemPrefers(scheme: 'light' | 'dark') {
  vi.stubGlobal(
    'matchMedia',
    vi.fn(() => ({ matches: scheme === 'dark', addEventListener: vi.fn() })),
  )
}

afterEach(() => {
  delete document.documentElement.dataset.theme
})

describe('the theme of the application (ADR 0058)', () => {
  it('follows the system until the user chooses', () => {
    systemPrefers('dark')

    expect(loadThemeChoice()).toBe('system')
    expect(resolveTheme('system')).toBe('dark')
    applyTheme(loadThemeChoice())
    expect(document.documentElement.dataset.theme).toBe('dark')
  })

  it('keeps the chosen theme on this device, whatever the system says', () => {
    systemPrefers('dark')

    saveThemeChoice('light')

    expect(document.documentElement.dataset.theme).toBe('light')
    expect(localStorage.getItem(THEME_STORAGE_KEY)).toBe('light')
    expect(loadThemeChoice()).toBe('light')
  })

  it('goes back to the system when the user says so', () => {
    systemPrefers('light')
    saveThemeChoice('dark')

    saveThemeChoice('system')

    expect(localStorage.getItem(THEME_STORAGE_KEY)).toBeNull()
    expect(document.documentElement.dataset.theme).toBe('light')
  })

  it('sets the same theme before the first paint, from the script of the page', () => {
    systemPrefers('light')
    localStorage.setItem(THEME_STORAGE_KEY, 'dark')

    // Le script de public/theme.js, tel que le navigateur l'exécute avant d'afficher la page.
    new Function(bootScript)()

    expect(bootScript).toContain(`'${THEME_STORAGE_KEY}'`)
    expect(document.documentElement.dataset.theme).toBe('dark')
  })
})
