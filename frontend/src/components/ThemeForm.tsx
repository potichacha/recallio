import { useState } from 'react'
import { loadThemeChoice, saveThemeChoice, type ThemeChoice } from '../theme/theme'

const CHOICES: { value: ThemeChoice; label: string }[] = [
  { value: 'system', label: 'Comme mon appareil' },
  { value: 'light', label: 'Clair' },
  { value: 'dark', label: 'Sombre' },
]

/** Le thème de l'application (ADR 0058), pour cet appareil : celui du système, à moins d'en choisir un. */
export function ThemeForm() {
  const [choice, setChoice] = useState<ThemeChoice>(loadThemeChoice)
  const choose = (next: ThemeChoice) => {
    saveThemeChoice(next)
    setChoice(next)
  }

  return (
    <fieldset className="flex max-w-md flex-col gap-2">
      <legend className="mb-1 text-lg font-semibold">Thème</legend>
      <p className="text-sm text-ink-muted">Clair ou sombre, sur cet appareil.</p>
      {CHOICES.map(({ value, label }) => (
        <label key={value} className="flex min-h-11 items-center gap-3">
          <input
            type="radio"
            name="theme"
            checked={choice === value}
            onChange={() => choose(value)}
            className="size-5 accent-brand"
          />
          {label}
        </label>
      ))}
    </fieldset>
  )
}
