import type { Door } from './doors'

/**
 * La pastille à cocher des maquettes « trois portes » (ADR 0077) : un sujet, un thème, qu'on ajoute ou retire d'un
 * choix. Elle fait 44 px de haut et s'enfonce au clic, sans mouvement pour qui en demande moins. Cochée, elle prend la
 * couleur de sa porte ; celui qui l'écrit porte l'état (`aria-pressed`) et y ajoute une coche : la couleur ne dit
 * jamais seule qu'elle est cochée.
 */
const BASE =
  'inline-flex min-h-11 items-center gap-1.5 rounded-full border-2 px-3.5 py-[0.5625rem] text-[0.9375rem] transition-transform duration-75 active:translate-y-1 motion-reduce:transition-none motion-reduce:active:translate-y-0'

const UNSELECTED = 'border-line-strong bg-surface font-extrabold text-ink'

const SELECTED: Record<Door, string> = {
  learn: 'border-brand bg-brand font-black text-on-brand',
  practice: 'border-info bg-info font-black text-on-info',
  play: 'border-ink bg-ink font-black text-surface',
}

export function chip({
  selected = false,
  door = 'learn',
}: { selected?: boolean; door?: Door } = {}): string {
  return `${BASE} ${selected ? SELECTED[door] : UNSELECTED}`
}
