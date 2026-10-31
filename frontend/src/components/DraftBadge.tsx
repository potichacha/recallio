/** Signale un sujet en brouillon : l'API ne renvoie les brouillons qu'aux admins, qui les relisent avant publication. */
export function DraftBadge() {
  return (
    <span className="rounded bg-amber-100 px-1.5 py-0.5 text-xs font-medium text-amber-800">
      Brouillon
    </span>
  )
}
