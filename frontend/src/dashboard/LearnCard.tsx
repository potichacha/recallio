import { ArrowRight } from '@phosphor-icons/react'
import { Link } from 'react-router'
import type { ThemeStats } from '../api/stats'
import { useTour, useTourProgress } from '../api/tour'
import { LoadingState } from '../components/states/LoadingState'
import { button } from '../components/ui/button'
import { SegmentedProgress } from '../components/ui/SegmentedProgress'
import { eyebrow } from '../components/ui/typography'
import { courseLearnStep, tourLearnStep, type LearnStep } from './learnStep'

/** Au-delà, les segments seraient trop fins pour se lire : il reste le compte, écrit. */
const MAX_SEGMENTS = 12

const DONE_LABELS: Record<LearnStep['unit'], string> = {
  lesson: 'Leçons faites',
  chapter: 'Chapitres terminés',
}

type Props = {
  subject: string
  title: string
  hasTour: boolean
  /** Les chapitres du sujet et leurs cartes vues : ce qui reste à montrer d'un sujet sans Tour. */
  themes: ThemeStats[]
  /** Vrai tant qu'aucune carte n'a été révisée : aucune progression à zéro n'est affichée. */
  newcomer: boolean
}

/**
 * La carte dominante de l'accueil (ADR 0077) : le sujet en cours, le chapitre où l'on en est, ses leçons faites, et
 * « Continuer ». Elle mène là où « Reprendre le Tour » mène pour ce sujet, ou à son cours s'il n'a pas de Tour.
 */
export function LearnCard(props: Props) {
  return props.hasTour ? <TourLearnCard {...props} /> : <CourseLearnCard {...props} />
}

function CourseLearnCard({ subject, title, themes, newcomer }: Props) {
  return (
    <LearnCardView
      subject={subject}
      title={title}
      step={courseLearnStep(subject, themes)}
      newcomer={newcomer}
    />
  )
}

/** Le Tour du sujet et ce qu'on y a fait ; s'ils ne se chargent pas, la carte se rabat sur le cours du sujet. */
function TourLearnCard(props: Props) {
  const { subject, title, newcomer } = props
  const tour = useTour(subject)
  const progress = useTourProgress(subject, true)

  if (tour.isError || progress.isError) return <CourseLearnCard {...props} />
  if (!tour.data || !progress.data) {
    return <LoadingState label="Chargement de ta leçon…" shape="card" />
  }
  const step = tourLearnStep(subject, tour.data, progress.data)
  if (!step) return <CourseLearnCard {...props} />
  return <LearnCardView subject={subject} title={title} step={step} newcomer={newcomer} />
}

/** Le dessin de la carte : trois cartes en éventail sur un disque, qui dépassent en haut à droite. */
function Illustration() {
  return (
    <svg
      aria-hidden="true"
      viewBox="0 0 150 150"
      className="absolute -top-3.5 -right-4.5 size-[9.375rem] md:-top-7.5 md:-right-7.5 md:size-60"
    >
      <circle cx="90" cy="60" r="70" className="fill-hero-shade" />
      <rect
        x="52"
        y="22"
        width="56"
        height="72"
        rx="12"
        transform="rotate(-14 80 58)"
        className="fill-brand-line"
      />
      <rect
        x="62"
        y="24"
        width="56"
        height="72"
        rx="12"
        transform="rotate(-2 90 60)"
        className="fill-white"
      />
      <rect
        x="74"
        y="28"
        width="56"
        height="72"
        rx="12"
        transform="rotate(11 102 64)"
        className="fill-sun"
      />
      <path
        d="M90 56h26M90 68h16"
        strokeWidth="5"
        strokeLinecap="round"
        transform="rotate(11 102 64)"
        className="fill-none stroke-on-sun"
      />
    </svg>
  )
}

function LearnCardView({
  subject,
  title,
  step,
  newcomer,
}: {
  subject: string
  title: string
  /** Absente pour un sujet qui n'a encore ni Tour ni carte : la carte mène à son cours, sans rien compter. */
  step: LearnStep | undefined
  newcomer: boolean
}) {
  const showProgress = step !== undefined && (!newcomer || step.done > 0)
  const count = step ? `${step.done} sur ${step.total}` : ''
  const label = step ? `${DONE_LABELS[step.unit]} : ${count}` : ''
  const segmented = step !== undefined && step.total <= MAX_SEGMENTS
  return (
    <section
      aria-labelledby="learn-title"
      className="relative flex flex-col gap-3.5 overflow-hidden rounded-3xl bg-hero px-5 pt-5.5 pb-5 text-on-hero shadow-hero md:gap-4.5 md:rounded-[2rem] md:p-7.5 xl:flex-[2_1_0%]"
    >
      <Illustration />
      <div className="relative">
        <p className={`${eyebrow()} text-on-hero-muted`}>Apprendre · {title}</p>
        <h2
          id="learn-title"
          className="mt-1.5 max-w-52 text-[1.75rem] leading-[1.1] font-black tracking-[-0.025rem] md:mt-2 md:max-w-80 md:text-[2.5rem] md:leading-[1.05] md:tracking-[-0.05rem]"
        >
          {step?.chapter || title}
        </h2>
        {step?.next && (
          <p className="mt-2.5 hidden max-w-xs font-bold text-on-hero-muted md:block">
            Prochaine leçon : {step.next}.
          </p>
        )}
      </div>
      {showProgress && (
        <div className="relative flex items-center gap-3 md:max-w-[22.5rem] md:gap-3.5">
          {segmented && (
            <SegmentedProgress total={step.total} filled={step.done} label={label} tone="hero" />
          )}
          {/* Avec les segments, leur nom dit déjà le compte ; sans eux, cette ligne le dit seule. */}
          <span className="text-sm font-black md:text-[0.9375rem]" aria-hidden={segmented}>
            {segmented ? count : label}
          </span>
        </div>
      )}
      <Link
        to={step?.to ?? `/subjects/${encodeURIComponent(subject)}/course`}
        className={`relative ${button({ variant: 'sun', size: 'lg' })} focus-visible:outline-on-hero md:self-start md:px-8`}
      >
        {step === undefined || step.started ? 'Continuer' : 'Commencer'}
        <ArrowRight weight="bold" aria-hidden className="size-5" />
      </Link>
    </section>
  )
}
