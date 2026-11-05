import {
  Books,
  CalendarCheck,
  CheckCircle,
  Compass,
  Fire,
  Globe,
  MapTrifold,
  Plant,
  Stack,
  Target,
  Trophy,
  type Icon,
} from '@phosphor-icons/react'
import type { Achievement, Quest } from '../api/quests'

const count = (value: number, one: string, many: string) => `${value} ${value > 1 ? many : one}`

/** Ce que demande chaque quête (ADR 0040), avec son objectif, et une précision quand le libellé ne suffit pas. */
export const QUEST_LABELS: Record<Quest, { title: (target: number) => string; hint?: string }> = {
  DAILY_REVIEWS: { title: (target) => `Réviser ${count(target, 'carte', 'cartes')}` },
  DAILY_VERIFIED: {
    title: (target) => `Réussir ${count(target, 'question corrigée', 'questions corrigées')}`,
    hint: 'QCM, vrai ou faux, code à compléter ou exercice : le serveur vérifie la réponse.',
  },
  DAILY_CODE: {
    title: (target) => `Réviser ${count(target, 'carte de code', 'cartes de code')}`,
    hint: 'Qu’affiche ce code, bug à corriger, code à compléter ou exercice.',
  },
  DAILY_TOUR_PAGES: { title: (target) => `Lire ${count(target, 'page', 'pages')} du Tour` },
  DAILY_TOUR_NOTION: {
    title: (target) =>
      target > 1 ? `Valider ${target} notions du Tour` : 'Valider une notion du Tour',
  },
  WEEKLY_DAYS: { title: (target) => `Réviser ${count(target, 'jour', 'jours')} dans la semaine` },
  WEEKLY_EXERCISES: {
    title: (target) => `Réussir ${count(target, 'exercice de code', 'exercices de code')}`,
    hint: 'Un exercice réussi passe tous ses tests.',
  },
  WEEKLY_FORMATS: {
    title: (target) => `Réviser ${count(target, 'format', 'formats')} de cartes différents`,
  },
  WEEKLY_TOUR: { title: (target) => `Valider ${count(target, 'notion', 'notions')} du Tour` },
}

/** Les succès (ADR 0040) : un nom, ce qui les débloque, et une icône (ADR 0049). */
export const ACHIEVEMENT_LABELS: Record<
  Achievement,
  { title: string; description: string; icon: Icon }
> = {
  FIRST_REVIEW: { title: 'Premier pas', description: 'Réviser une première carte.', icon: Plant },
  HUNDRED_REVIEWS: { title: 'Centurion', description: 'Réviser 100 cartes.', icon: Stack },
  THOUSAND_REVIEWS: { title: 'Mille cartes', description: 'Réviser 1 000 cartes.', icon: Books },
  STREAK_7: { title: 'Une semaine', description: 'Tenir une série de 7 jours.', icon: Fire },
  STREAK_30: { title: 'Un mois', description: 'Tenir une série de 30 jours.', icon: CalendarCheck },
  FIRST_EXERCISE: {
    title: 'Tous les tests passent',
    description: 'Réussir un premier exercice de code, jugé par ses tests.',
    icon: CheckCircle,
  },
  POLYGLOT: { title: 'Polyglotte', description: 'Réviser dans trois sujets.', icon: Globe },
  FIRST_TOUR_NOTION: {
    title: 'En route',
    description: 'Valider une première notion du Tour.',
    icon: Compass,
  },
  TOUR_EXPLORER: {
    title: 'Explorateur',
    description: 'Valider dix notions du Tour.',
    icon: MapTrifold,
  },
  FIRST_QUEST: { title: 'Première quête', description: 'Terminer une quête.', icon: Target },
  WEEKLY_QUEST: {
    title: 'Quête de la semaine',
    description: 'Terminer une quête de la semaine.',
    icon: Trophy,
  },
}
