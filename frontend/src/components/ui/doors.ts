/**
 * Les trois portes de l'application (ADR 0077), et la couleur de chacune : le violet de la marque pour « Apprendre »,
 * le bleu pour « S'entraîner », le jaune pour « Jouer ». Une recette qui prend la couleur d'une porte reçoit ce nom.
 */
export type Door = 'learn' | 'practice' | 'play'
