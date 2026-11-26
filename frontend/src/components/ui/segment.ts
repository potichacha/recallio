import type { Door } from './doors'

/**
 * Le segment des maquettes « trois portes » (ADR 0077) : un choix parmi quelques-uns, côte à côte (le poste visé, le
 * nombre de questions). Choisi, il prend un contour plus épais et un fond doux, à la couleur de sa porte ; celui qui
 * l'écrit porte l'état (`aria-pressed`, ou `role="radio"` et `aria-checked`).
 */
const BASE =
  'flex min-h-[2.875rem] items-center justify-center rounded-lg px-1.5 text-[0.9375rem] text-ink transition-transform duration-75 active:translate-y-1 motion-reduce:transition-none motion-reduce:active:translate-y-0'

const UNSELECTED = 'border-2 border-line-strong bg-surface font-extrabold'

const SELECTED: Record<Door, string> = {
  learn: 'border-[3px] border-brand bg-brand-soft font-black',
  practice: 'border-[3px] border-info bg-info-soft font-black',
  play: 'border-[3px] border-ink bg-sun-soft font-black',
}

export function segment({
  selected = false,
  door = 'learn',
}: { selected?: boolean; door?: Door } = {}): string {
  return `${BASE} ${selected ? SELECTED[door] : UNSELECTED}`
}
