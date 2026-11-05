/** Signale un sujet en brouillon : l'API ne renvoie les brouillons qu'aux admins, qui les relisent avant publication. */
export function DraftBadge() {
  return (
    <span className="rounded bg-warning-soft px-1.5 py-0.5 text-xs font-medium text-warning">
      Brouillon
    </span>
  )
}
