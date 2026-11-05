/** Le Tour de test : trois notions de Go, dont une de deux pages. */
export const TEST_TOUR = {
  subjectSlug: 'go',
  subjectTitle: 'Go',
  domains: [
    {
      id: 'bases',
      title: 'Bases du langage',
      chapters: [
        {
          id: 'premiers-pas',
          title: 'Premiers pas',
          notions: [
            { slug: 'hello-world', title: 'Le premier programme', pageCount: 1 },
            { slug: 'packages-imports', title: 'Packages et imports', pageCount: 2 },
          ],
          exercises: [],
        },
        {
          id: 'variables-types',
          title: 'Variables, types et constantes',
          notions: [{ slug: 'valeur-zero', title: 'La valeur zéro', pageCount: 1 }],
          exercises: [
            {
              notion: 'valeur-zero',
              cardId: 'ex1',
              summary: 'Écris `Zero`, qui renvoie la valeur zéro',
            },
          ],
        },
      ],
    },
  ],
}

export const HELLO_CODE =
  'package main\n\nimport "fmt"\n\nfunc main() {\n\tfmt.Println("Bonjour, Go !")\n}\n'

/** Les pages d'une notion, chacune avec le même programme. */
export function tourNotion(slug: string, title: string, ...pageTitles: string[]) {
  return {
    slug,
    title,
    chapterTitle: 'Premiers pas',
    pages: pageTitles.map((pageTitle) => ({
      title: pageTitle,
      text: `Le texte de **${pageTitle}**.`,
      example: [{ path: 'main.go', content: HELLO_CODE }],
      task: null,
    })),
  }
}

/** Les questions d'une notion : un vrai ou faux (la réponse juste est « Faux ») et un code à trous (« main »). */
export function checkpoint(validated = false) {
  return {
    validated,
    questions: [
      {
        cardId: 'q1',
        type: 'TRUE_FALSE',
        prompt: '`main` prend des paramètres.',
        choices: ['Vrai', 'Faux'],
        multipleAnswers: false,
        blankCount: 0,
      },
      {
        cardId: 'q2',
        type: 'FILL_IN',
        prompt: '`package ___`',
        choices: [],
        multipleAnswers: false,
        blankCount: 1,
      },
    ],
  }
}

/** La correction de ces deux questions, justes ou non. */
export function checkResult(correct: boolean) {
  return {
    validated: correct,
    results: [
      {
        cardId: 'q1',
        correct,
        correctChoices: [1],
        blanks: [],
        explanation: 'Non : `main` ne prend rien.',
      },
      {
        cardId: 'q2',
        correct,
        correctChoices: [],
        blanks: [{ expected: 'main', correct }],
        explanation: 'Le package `main`.',
      },
    ],
  }
}
