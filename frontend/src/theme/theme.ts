/**
 * Le thème de l'application (ADR 0058) : celui du système par défaut, ou clair ou sombre au choix, gardé sur
 * l'appareil. La page porte `data-theme`, que lisent les tokens de index.css ; public/theme.js le pose avant le premier
 * affichage, avec la même règle.
 */
export type ThemeChoice = 'system' | 'light' | 'dark'

export const THEME_STORAGE_KEY = 'recallio.theme'

const DARK_SCHEME = '(prefers-color-scheme: dark)'

export function loadThemeChoice(): ThemeChoice {
  const stored = localStorage.getItem(THEME_STORAGE_KEY)
  return stored === 'light' || stored === 'dark' ? stored : 'system'
}

/** Le thème affiché pour ce choix : celui du système s'il n'y a pas de choix. */
export function resolveTheme(choice: ThemeChoice): 'light' | 'dark' {
  if (choice !== 'system') return choice
  return window.matchMedia?.(DARK_SCHEME).matches ? 'dark' : 'light'
}

export function applyTheme(choice: ThemeChoice) {
  document.documentElement.dataset.theme = resolveTheme(choice)
}

export function saveThemeChoice(choice: ThemeChoice) {
  if (choice === 'system') localStorage.removeItem(THEME_STORAGE_KEY)
  else localStorage.setItem(THEME_STORAGE_KEY, choice)
  applyTheme(choice)
}

/** Suit le système tant que l'utilisateur n'a rien choisi : passer son téléphone en sombre le soir change l'application. */
export function followSystemTheme() {
  window.matchMedia?.(DARK_SCHEME).addEventListener('change', () => applyTheme(loadThemeChoice()))
}
