/**
 * Les retours sensoriels d'une réponse (ADR 0050) : une vibration et un son court, chacun coupé par défaut. Beaucoup
 * révisent au bureau ou dans les transports : c'est à l'utilisateur de les allumer, dans son profil.
 */
export type FeedbackSettings = { sound: boolean; vibration: boolean }

const STORAGE_KEY = 'recallio.feedback'
const OFF: FeedbackSettings = { sound: false, vibration: false }

/** Les notes jouées : deux notes qui montent quand on savait, une note grave sinon. */
const TONES = { recalled: [523, 784], forgotten: [196] }
const TONE_SECONDS = 0.18
const TONE_GAP_SECONDS = 0.09
const VOLUME = 0.08

export function loadFeedbackSettings(): FeedbackSettings {
  try {
    const stored: unknown = JSON.parse(localStorage.getItem(STORAGE_KEY) ?? 'null')
    if (typeof stored !== 'object' || stored === null) return OFF
    return {
      sound: 'sound' in stored && stored.sound === true,
      vibration: 'vibration' in stored && stored.vibration === true,
    }
  } catch {
    return OFF
  }
}

export function saveFeedbackSettings(settings: FeedbackSettings) {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(settings))
  } catch {
    // Stockage indisponible : les retours restent coupés.
  }
}

let audio: AudioContext | undefined

/** Le retour d'une réponse, selon les réglages de l'utilisateur. */
export function answerFeedback(recalled: boolean) {
  const { sound, vibration } = loadFeedbackSettings()
  if (vibration) navigator.vibrate?.(recalled ? 30 : [40, 60, 40])
  if (sound) playTones(recalled ? TONES.recalled : TONES.forgotten)
}

function playTones(frequencies: number[]) {
  try {
    audio ??= new AudioContext()
    const context = audio
    frequencies.forEach((frequency, index) => {
      const start = context.currentTime + index * TONE_GAP_SECONDS
      const oscillator = context.createOscillator()
      const gain = context.createGain()
      oscillator.frequency.value = frequency
      gain.gain.setValueAtTime(VOLUME, start)
      gain.gain.exponentialRampToValueAtTime(0.0001, start + TONE_SECONDS)
      oscillator.connect(gain).connect(context.destination)
      oscillator.start(start)
      oscillator.stop(start + TONE_SECONDS)
    })
  } catch {
    // Pas de son possible ici : la réponse reste affichée, c'est l'essentiel.
  }
}
