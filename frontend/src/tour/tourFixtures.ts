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
        },
        {
          id: 'variables-types',
          title: 'Variables, types et constantes',
          notions: [{ slug: 'valeur-zero', title: 'La valeur zéro', pageCount: 1 }],
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
