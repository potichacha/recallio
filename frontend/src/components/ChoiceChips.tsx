import type { ReactNode } from 'react'

/** Un groupe de choix exclusifs, présentés comme des pastilles : le sujet, la durée d'une session... */
export function Choices({ legend, children }: { legend: string; children: ReactNode }) {
  return (
    <fieldset className="flex flex-col gap-2">
      <legend className="mb-2 text-sm font-medium text-ink-soft">{legend}</legend>
      <div className="flex flex-wrap gap-2">{children}</div>
    </fieldset>
  )
}

/** Un choix parmi d'autres : un bouton radio, présenté comme une pastille. */
export function Chip({
  name,
  checked,
  onChange,
  children,
}: {
  name: string
  checked: boolean
  onChange: () => void
  children: ReactNode
}) {
  return (
    <label className="flex cursor-pointer flex-col rounded-lg border border-line bg-surface px-3 py-2 text-sm text-ink-soft hover:border-brand-line has-checked:border-brand has-checked:bg-brand-soft has-checked:text-brand-strong has-focus-visible:ring-2 has-focus-visible:ring-brand">
      <input type="radio" name={name} checked={checked} onChange={onChange} className="sr-only" />
      {children}
    </label>
  )
}
