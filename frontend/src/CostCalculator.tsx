import { useState } from 'react'
import type { FormEvent } from 'react'

type Props = {
  applianceId: string
  applianceName: string
}

type Calculation = {
  energyKwh: number
  costGbp: number
}

export default function CostCalculator({
  applianceId,
  applianceName,
}: Props) {
  const [hours, setHours] = useState('3')
  const [rate, setRate] = useState('0.25')
  const [result, setResult] = useState<Calculation | null>(null)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')

  async function calculate(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setError('')
    setResult(null)

    const durationHours = Number(hours)
    const rateGbpPerKwh = Number(rate)

    if (
      hours.trim() === '' ||
      rate.trim() === '' ||
      !Number.isFinite(durationHours) ||
      !Number.isFinite(rateGbpPerKwh) ||
      durationHours < 0 ||
      rateGbpPerKwh < 0
    ) {
      setError('Enter valid hours and a rate of zero or more.')
      return
    }

    setBusy(true)

    try {
      const response = await fetch(
        `/api/appliances/${applianceId}/calculate`,
        {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ durationHours, rateGbpPerKwh }),
        },
      )

      const body = await response.json()

      if (!response.ok) {
        throw new Error(
          body.detail ?? body.error ?? 'Could not calculate the cost.',
        )
      }

      setResult(body as Calculation)
    } catch (error) {
      setError(
        error instanceof Error ? error.message : 'Calculation failed.',
      )
    } finally {
      setBusy(false)
    }
  }

  return (
    <div>
      <p className="muted">
        Calculate the cost of running {applianceName}.
        Enter the rate in pounds: 0.25 means 25p per kWh.
      </p>

      <form onSubmit={calculate}>
        <fieldset disabled={busy}>
          <div className="form-grid">
            <div>
              <label htmlFor="usage-hours">Usage time (hours)</label>
              <input
                id="usage-hours"
                type="number"
                min="0"
                step="any"
                required
                value={hours}
                onChange={(event) => {
                  setHours(event.target.value)
                  setResult(null)
                  setError('')
                }}
              />
            </div>

            <div>
              <label htmlFor="electricity-rate">Rate (£ per kWh)</label>
              <input
                id="electricity-rate"
                type="number"
                min="0"
                step="any"
                required
                value={rate}
                onChange={(event) => {
                  setRate(event.target.value)
                  setResult(null)
                  setError('')
                }}
              />
            </div>
          </div>

          <button className="primary-button" type="submit">
            {busy ? 'Calculating…' : 'Calculate cost'}
          </button>
        </fieldset>
      </form>

      {error && <p className="error" role="alert">{error}</p>}

      <div aria-live="polite">
        {result && (
          <div className="results-grid">
            <div className="result-card">
              <p>Energy used</p>
              <strong>
                {result.energyKwh.toLocaleString('en-GB', {
                  maximumFractionDigits: 6,
                })} kWh
              </strong>
            </div>

            <div className="result-card">
              <p>Estimated cost</p>
              <strong>
                {result.costGbp.toLocaleString('en-GB', {
                  style: 'currency',
                  currency: 'GBP',
                })}
              </strong>
            </div>
          </div>
        )}
      </div>

      <p className="calculation-note">
        Assumes constant power throughout the usage period.
        Excludes standing charges.
      </p>
    </div>
  )
}