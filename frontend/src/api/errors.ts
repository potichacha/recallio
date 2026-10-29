import type { components } from './schema'

type ProblemDetail = components['schemas']['ProblemDetail']

/** Erreur d'API portant le code stable du Problem Details (RFC 9457) renvoyé par le backend. */
export class ApiError extends Error {
  readonly status: number
  readonly code: string

  constructor(status: number, code: string, message: string) {
    super(message)
    this.status = status
    this.code = code
  }

  static from(response: Response, error: unknown): ApiError {
    const problem = error as Partial<ProblemDetail> | undefined
    return new ApiError(
      response.status,
      problem?.code ?? 'unknown',
      problem?.detail ?? `Erreur ${response.status}`,
    )
  }
}
