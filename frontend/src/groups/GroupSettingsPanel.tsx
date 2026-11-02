import { useState, type FormEvent } from 'react'
import { Link } from 'react-router'
import { useSubjects, useThemesOfSubjects, type CardType } from '../api/content'
import { ApiError } from '../api/errors'
import {
  useUpdateGroupSettings,
  type Group,
  type GroupSettings,
  type LeaderboardVisibility,
} from '../api/groups'

const FORMAT_LABELS: Record<CardType, string> = {
  FLASHCARD: 'Questions techniques',
  CODE_OUTPUT: "Qu'affiche ce code ?",
  OPEN_SCENARIO: 'Mises en situation',
  DEFINITION: 'Définitions',
  MULTIPLE_CHOICE: 'QCM',
  TRUE_FALSE: 'Vrai ou faux',
  BUG_FIX: 'Bugs à corriger',
  FILL_IN: 'Codes à compléter',
}

const VISIBILITY_LABELS: Record<LeaderboardVisibility, string> = {
  NAMED: 'Nominatif',
  ANONYMOUS: 'Anonymisé',
  HIDDEN: 'Désactivé',
}

const MIN_DAILY_GOAL = 1
const MAX_DAILY_GOAL = 200

/** Ce que le groupe révise ensemble ; le créateur peut le modifier. */
export function GroupSettingsPanel({ group }: { group: Group }) {
  const [editing, setEditing] = useState(false)
  const isOwner = group.myRole === 'OWNER'

  return (
    <section
      aria-labelledby="settings-title"
      className="flex flex-col gap-3 rounded-xl bg-white p-4 shadow-sm"
    >
      <h2 id="settings-title" className="text-lg font-semibold">
        Réglages
      </h2>
      {editing ? (
        <SettingsForm group={group} onDone={() => setEditing(false)} />
      ) : (
        <>
          <SettingsSummary settings={group.settings} />
          <div className="flex flex-wrap items-center gap-4">
            {group.settings.subjects.length > 0 && (
              <Link
                to={`/review?group=${group.id}`}
                className="rounded-lg bg-indigo-600 px-4 py-2 font-medium text-white hover:bg-indigo-700"
              >
                Réviser avec le groupe
              </Link>
            )}
            {isOwner && (
              <button
                type="button"
                onClick={() => setEditing(true)}
                className="text-sm font-medium text-indigo-600 hover:underline"
              >
                Modifier les réglages
              </button>
            )}
          </div>
        </>
      )}
    </section>
  )
}

function SettingsSummary({ settings }: { settings: GroupSettings }) {
  const themeCount = settings.themes.length

  return (
    <dl className="grid grid-cols-[auto_1fr] gap-x-4 gap-y-1 text-sm">
      <dt className="text-slate-500">Sujets</dt>
      <dd>
        {settings.subjects.length > 0 ? (
          <SubjectTitles slugs={settings.subjects} />
        ) : (
          'Aucun sujet choisi pour le moment'
        )}
      </dd>
      <dt className="text-slate-500">Thèmes</dt>
      <dd>
        {themeCount > 0
          ? `${themeCount} thème${themeCount > 1 ? 's' : ''} sélectionné${themeCount > 1 ? 's' : ''}`
          : 'Tous les thèmes'}
      </dd>
      <dt className="text-slate-500">Formats</dt>
      <dd>
        {settings.cardTypes.length > 0
          ? settings.cardTypes.map((type) => FORMAT_LABELS[type]).join(', ')
          : 'Tous les formats'}
      </dd>
      <dt className="text-slate-500">Objectif quotidien</dt>
      <dd>{settings.dailyGoal} cartes</dd>
      <dt className="text-slate-500">Classement</dt>
      <dd>{VISIBILITY_LABELS[settings.leaderboardVisibility]}</dd>
    </dl>
  )
}

/** Le catalogue n'est chargé que s'il y a des sujets à nommer. */
function SubjectTitles({ slugs }: { slugs: string[] }) {
  const subjects = useSubjects()
  const titleOf = (slug: string) =>
    subjects.data?.find((subject) => subject.slug === slug)?.title ?? slug
  return <>{slugs.map(titleOf).join(', ')}</>
}

/** Les ensembles sont remplacés en entier à l'enregistrement (PUT) : le brouillon part des réglages actuels. */
function SettingsForm({ group, onDone }: { group: Group; onDone: () => void }) {
  const [draft, setDraft] = useState<GroupSettings>(group.settings)
  const update = useUpdateGroupSettings(group.id)
  const subjects = useSubjects()
  const { themes, pending: themesPending } = useThemesOfSubjects(draft.subjects)
  const published = subjects.data?.filter((subject) => subject.status === 'PUBLISHED') ?? []

  const toggle = <T,>(values: T[], value: T) =>
    values.includes(value) ? values.filter((other) => other !== value) : [...values, value]

  const submit = (event: FormEvent) => {
    event.preventDefault()
    // Un thème d'un sujet retiré n'a plus de sens : le serveur le refuserait. D'où l'attente de tous les thèmes.
    const shownThemes = new Set(themes.map((theme) => theme.slug))
    update.mutate(
      { ...draft, themes: draft.themes.filter((slug) => shownThemes.has(slug)) },
      { onSuccess: onDone },
    )
  }

  return (
    <form onSubmit={submit} aria-label="Réglages du groupe" className="flex flex-col gap-4">
      <fieldset className="flex flex-col gap-1">
        <legend className="text-sm font-medium">Sujets</legend>
        {published.map((subject) => (
          <label key={subject.slug} className="flex items-center gap-2">
            <input
              type="checkbox"
              checked={draft.subjects.includes(subject.slug)}
              onChange={() =>
                setDraft({ ...draft, subjects: toggle(draft.subjects, subject.slug) })
              }
            />
            {subject.title}
          </label>
        ))}
      </fieldset>

      {draft.subjects.length > 0 && (
        <fieldset className="flex flex-col gap-1">
          <legend className="text-sm font-medium">Thèmes</legend>
          <p className="text-xs text-slate-500">
            Un sujet dont aucun thème n'est coché garde tous ses thèmes.
          </p>
          {draft.subjects.map((slug) => (
            <details key={slug} className="rounded-lg border border-slate-200 px-3 py-2">
              <summary className="cursor-pointer text-sm">
                {published.find((subject) => subject.slug === slug)?.title ?? slug}
              </summary>
              <div className="mt-2 flex flex-col gap-1">
                {themes
                  .filter((theme) => theme.subjectSlug === slug)
                  .map((theme) => (
                    <label key={theme.slug} className="flex items-center gap-2 text-sm">
                      <input
                        type="checkbox"
                        checked={draft.themes.includes(theme.slug)}
                        onChange={() =>
                          setDraft({ ...draft, themes: toggle(draft.themes, theme.slug) })
                        }
                      />
                      {theme.title}
                    </label>
                  ))}
              </div>
            </details>
          ))}
        </fieldset>
      )}

      <fieldset className="flex flex-col gap-1">
        <legend className="text-sm font-medium">Formats de carte</legend>
        <p className="text-xs text-slate-500">Aucun coché : tous les formats.</p>
        {(Object.keys(FORMAT_LABELS) as CardType[]).map((type) => (
          <label key={type} className="flex items-center gap-2">
            <input
              type="checkbox"
              checked={draft.cardTypes.includes(type)}
              onChange={() => setDraft({ ...draft, cardTypes: toggle(draft.cardTypes, type) })}
            />
            {FORMAT_LABELS[type]}
          </label>
        ))}
      </fieldset>

      <label className="flex flex-col gap-1">
        <span className="text-sm font-medium">Objectif quotidien (cartes)</span>
        <input
          type="number"
          min={MIN_DAILY_GOAL}
          max={MAX_DAILY_GOAL}
          required
          // Champ vidé : NaN, affiché vide ; `required` empêche alors l'enregistrement.
          value={Number.isNaN(draft.dailyGoal) ? '' : draft.dailyGoal}
          onChange={(event) => setDraft({ ...draft, dailyGoal: event.target.valueAsNumber })}
          className="w-28 rounded-lg border border-slate-300 px-3 py-2"
        />
      </label>

      <fieldset className="flex flex-col gap-1">
        <legend className="text-sm font-medium">Classement</legend>
        {(Object.keys(VISIBILITY_LABELS) as LeaderboardVisibility[]).map((visibility) => (
          <label key={visibility} className="flex items-center gap-2">
            <input
              type="radio"
              name="leaderboardVisibility"
              checked={draft.leaderboardVisibility === visibility}
              onChange={() => setDraft({ ...draft, leaderboardVisibility: visibility })}
            />
            {VISIBILITY_LABELS[visibility]}
          </label>
        ))}
      </fieldset>

      <div className="flex flex-wrap items-center gap-3">
        <button
          type="submit"
          disabled={update.isPending || themesPending}
          className="rounded-lg bg-indigo-600 px-4 py-2 font-medium text-white hover:bg-indigo-700 disabled:opacity-50"
        >
          Enregistrer les réglages
        </button>
        <button type="button" onClick={onDone} className="text-sm text-slate-600 hover:underline">
          Annuler
        </button>
      </div>
      {update.isError && (
        <p role="alert">
          {update.error instanceof ApiError
            ? update.error.message
            : "Les réglages n'ont pas pu être enregistrés."}
        </p>
      )}
    </form>
  )
}
