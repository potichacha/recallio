import { render, screen, within } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { DayBars } from './DayBars'
import { HorizontalBars } from './HorizontalBars'

const POINTS = [
  { label: '23 nov.', value: 0 },
  { label: '24 nov.', value: 1250 },
  { label: '25 nov.', value: 5 },
]

/** La hauteur d'une barre, telle que le SVG la porte. */
const heights = (chart: HTMLElement) =>
  Array.from(chart.querySelectorAll('rect'), (bar) => Number(bar.getAttribute('height')))

describe('DayBars', () => {
  it('names the drawing and describes it for a screen reader', () => {
    render(
      <DayBars
        title="Révisions, par jour"
        description="Trois jours."
        unit="Révisions"
        points={POINTS}
      />,
    )

    const chart = screen.getByRole('img', { name: 'Révisions, par jour' })
    expect(chart).toHaveAccessibleDescription('Trois jours.')
  })

  it('draws one bar per day, the tallest for the highest value, none for a day at zero', () => {
    render(
      <DayBars title="Révisions" description="Trois jours." unit="Révisions" points={POINTS} />,
    )

    const [empty, tallest, small] = heights(screen.getByRole('img'))
    expect(empty).toBe(0)
    expect(tallest).toBeGreaterThan(100)
    // Une valeur non nulle reste visible à côté d'une valeur bien plus grande.
    expect(small).toBeGreaterThan(0)
    expect(small).toBeLessThan(tallest)
  })

  it('writes the scale and gives the same values in a table', () => {
    render(
      <DayBars title="Révisions" description="Trois jours." unit="Révisions" points={POINTS} />,
    )

    // Le repère du haut porte la plus grande valeur.
    expect(screen.getByRole('img').querySelector('text')).toHaveTextContent(/^1\s250$/)
    expect(screen.getByText('Voir les chiffres')).toBeInTheDocument()
    const table = screen.getByRole('table', { name: 'Révisions', hidden: true })
    expect(
      within(table)
        .getAllByRole('columnheader', { hidden: true })
        .map((header) => header.textContent),
    ).toEqual(['Jour', 'Révisions'])
    const rows = within(table).getAllByRole('row', { hidden: true }).slice(1)
    expect(rows.map((row) => row.textContent?.replace(/\s/g, ' '))).toEqual([
      '23 nov.0',
      '24 nov.1 250',
      '25 nov.5',
    ])
  })

  it('divides nothing by zero when every day is at zero', () => {
    const quiet = POINTS.map((point) => ({ ...point, value: 0 }))
    render(<DayBars title="Révisions" description="Rien." unit="Révisions" points={quiet} />)

    const chart = screen.getByRole('img')
    expect(heights(chart)).toEqual([0, 0, 0])
    expect(chart.querySelector('text')).toHaveTextContent('1')
    expect(chart.innerHTML).not.toMatch(/NaN|Infinity/)
  })

  it('draws an empty chart without failing when there is no day', () => {
    render(<DayBars title="Révisions" description="Rien." unit="Révisions" points={[]} />)

    expect(screen.getByRole('img').querySelectorAll('rect')).toHaveLength(0)
  })
})

describe('HorizontalBars', () => {
  const ROWS = [
    { key: 'a', label: 'Révision', value: 100, text: '4 comptes · 100 %' },
    { key: 'b', label: 'Rappels', hint: 'à ce jour', value: 25, text: '1 compte · 25 %' },
    { key: 'c', label: 'Examen blanc', value: 0, text: '0 compte · 0 %' },
  ]

  /** La longueur de la barre de chaque ligne ; une ligne sans barre n'a que sa piste. */
  const widths = (list: HTMLElement) =>
    within(list)
      .getAllByRole('listitem')
      .map((item) => item.querySelectorAll('rect')[1]?.getAttribute('width') ?? null)

  it('names the chart and writes every value next to its bar', () => {
    render(<HorizontalBars title="Part des comptes actifs" rows={ROWS} max={100} />)

    const items = within(
      screen.getByRole('list', { name: 'Part des comptes actifs' }),
    ).getAllByRole('listitem')
    expect(items.map((item) => item.textContent)).toEqual([
      'Révision4 comptes · 100 %',
      'Rappels · à ce jour1 compte · 25 %',
      'Examen blanc0 compte · 0 %',
    ])
  })

  it('draws each bar to scale, hidden from screen readers, and none for a value at zero', () => {
    render(<HorizontalBars title="Part des comptes actifs" rows={ROWS} max={100} />)

    const list = screen.getByRole('list')
    expect(widths(list)).toEqual(['100%', '25%', null])
    for (const drawing of Array.from(list.querySelectorAll('svg'))) {
      expect(drawing).toHaveAttribute('aria-hidden', 'true')
    }
  })

  it('scales to the largest row when no maximum is given', () => {
    const rows = [
      { key: 'a', label: 'Bases', value: 4, text: '4 comptes' },
      { key: 'b', label: 'Objet', value: 1, text: '1 compte' },
    ]
    render(<HorizontalBars title="Comptes par chapitre" rows={rows} />)

    expect(widths(screen.getByRole('list'))).toEqual(['100%', '25%'])
  })

  it('divides nothing by zero when every row is at zero', () => {
    const rows = ROWS.map((row) => ({ ...row, value: 0 }))
    render(<HorizontalBars title="Comptes par chapitre" rows={rows} />)

    const list = screen.getByRole('list')
    expect(widths(list)).toEqual([null, null, null])
    expect(list.innerHTML).not.toMatch(/NaN|Infinity/)
  })
})
