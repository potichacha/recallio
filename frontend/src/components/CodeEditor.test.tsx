import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import CodeEditor from './CodeEditor'

describe('CodeEditor', () => {
  it('shows the code in an editable text box named after the file', () => {
    render(<CodeEditor label="Point.java" value={'public final class Point {}\n'} />)

    const editor = screen.getByRole('textbox', { name: 'Point.java' })
    expect(editor).toHaveTextContent('public final class Point {}')
    expect(editor).toHaveAttribute('contenteditable', 'true')
  })

  it('highlights a Go file as Go', () => {
    render(
      <CodeEditor label="somme.go" value={'package somme\n\nfunc Somme() int { return 0 }\n'} />,
    )

    // `func` est un mot-clé en Go, coloré comme `return` ; en Java, ce serait un nom de type.
    const spans = Array.from(
      screen.getByRole('textbox', { name: 'somme.go' }).querySelectorAll('span'),
    )
    const token = (text: string) => spans.find((span) => span.textContent === text)?.className
    expect(token('func')).toBeDefined()
    expect(token('func')).toBe(token('return'))
  })

  it('becomes read-only without losing its content', () => {
    const { rerender } = render(<CodeEditor label="Point.java" value="class Point {}" />)

    rerender(<CodeEditor label="Point.java" value="class Point {}" readOnly />)

    const editor = screen.getByRole('textbox', { name: 'Point.java' })
    expect(editor).toHaveAttribute('contenteditable', 'false')
    expect(editor).toHaveTextContent('class Point {}')
  })
})
