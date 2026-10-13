import { useState } from 'react'
import type { FormEvent } from 'react'

type Props = {
  appliance: {
    name: string
    powerWatts: number
  }
}

type OptimisationResult = {
  name: string
  originalStartMinute: number
  recommendedStartMinute: number
  durationMinutes: number
  originalMeetsWindow: boolean
  originalCostGbp: number
  recommendedCostGbp: number
  estimatedSavingsGbp: number
}

function toMinutes(time: string): number {
  if (time === '24:00') return 1440

  if (!/^([01]\d|2[0-3]):[0-5]\d$/.test(time)) {
    throw new Error('Enter times in HH:mm format, such as 06:00.')
  }

  const [hours, minutes] = time.split(':').map(Number)
  return hours * 60 + minutes
}

function formatTime(minutes: number): string {
  const hours = Math.floor(minutes / 60)
  const remainder = minutes % 60

  return `${String(hours).padStart(2, '0')}:${String(remainder).padStart(2, '0')}`
}

function money(value: number): string {
  return value.toLocaleString('en-GB', {
    style: 'currency',
    currency: 'GBP',
  })
}

function readRate(value: string): number {
  const rate = Number(value)

  if (value.trim() === '' || !Number.isFinite(rate) || rate < 0) {
    throw new Error('Enter electricity rates of zero or more.')
  }

  return rate
}

export default function ScheduleOptimiser({ appliance }: Props) {
  const [originalStart, setOriginalStart] = useState('06:00')
  const [duration, setDuration] = useState('60')
  const [earliestStart, setEarliestStart] = useState('06:00')
  const [latestFinish, setLatestFinish] = useState('10:00')
  const [changeover, setChangeover] = useState('07:00')
  const [firstRate, setFirstRate] = useState('0.30')
  const [secondRate, setSecondRate] = useState('0.10')

  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const [result, setResult] = useState<OptimisationResult | null>(null)

  async function optimise(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setError('')
    setResult(null)

    try {
      const startMinute = toMinutes(originalStart)
      const earliestStartMinute = toMinutes(earliestStart)
      const latestFinishMinute = toMinutes(latestFinish)
      const changeoverMinute = toMinutes(changeover)
      const durationMinutes = Number(duration)

      if (
        startMinute >= 1440 ||
        earliestStartMinute >= 1440 ||
        changeoverMinute <= 0 ||
        changeoverMinute >= 1440
      ) {
        throw new Error(
          'Start times must be before midnight. The tariff changeover must be between 00:01 and 23:59.',
        )
      }

      if (
        !Number.isInteger(durationMinutes) ||
        durationMinutes < 1 ||
        durationMinutes > 1440
      ) {
        throw new Error('Duration must be a whole number from 1 to 1440 minutes.')
      }

      if (latestFinishMinute <= earliestStartMinute) {
        throw new Error(
          'The finish deadline must be later than the earliest start on the same day.',
        )
      }

      if (durationMinutes > latestFinishMinute - earliestStartMinute) {
        throw new Error('The appliance cannot finish within this window.')
      }

      const tariffs = [
        {
          startMinute: 0,
          endMinute: changeoverMinute,
          rateGbpPerKwh: readRate(firstRate),
        },
        {
          startMinute: changeoverMinute,
          endMinute: 1440,
          rateGbpPerKwh: readRate(secondRate),
        },
      ]

      setBusy(true)

      const response = await fetch('/api/energy/optimise', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          schedule: {
            name: appliance.name,
            powerWatts: appliance.powerWatts,
            startMinute,
            durationMinutes,
            tariffs,
          },
          earliestStartMinute,
          latestFinishMinute,
        }),
      })

      const body = await response.json()

      if (!response.ok) {
        throw new Error(
          body.detail ?? body.error ?? 'Could not optimise this schedule.',
        )
      }

      setResult(body as OptimisationResult)
    } catch (error) {
      setError(
        error instanceof Error ? error.message : 'Optimisation failed.',
      )
    } finally {
      setBusy(false)
    }
  }

  return (
    <section className="panel optimiser-panel" aria-labelledby="optimiser-heading">
      <h2 id="optimiser-heading">Find a cheaper running time</h2>
      <p className="muted">
        Plan when to run {appliance.name} ({appliance.powerWatts} W).
        The example rates below are editable, not live electricity prices.
      </p>

      <form
        onSubmit={optimise}
        onChange={() => {
          setResult(null)
          setError('')
        }}
      >
        <fieldset disabled={busy}>
          <legend>Appliance schedule</legend>

          <div className="form-grid">
            <div>
              <label htmlFor="original-start">Original start</label>
              <input
                id="original-start"
                type="time"
                required
                value={originalStart}
                onChange={(event) => setOriginalStart(event.target.value)}
              />
            </div>

            <div>
              <label htmlFor="run-duration">Duration (minutes)</label>
              <input
                id="run-duration"
                type="number"
                min="1"
                max="1440"
                step="1"
                required
                value={duration}
                onChange={(event) => setDuration(event.target.value)}
              />
            </div>

            <div>
              <label htmlFor="earliest-start">Earliest allowed start</label>
              <input
                id="earliest-start"
                type="time"
                required
                value={earliestStart}
                onChange={(event) => setEarliestStart(event.target.value)}
              />
            </div>

            <div>
              <label htmlFor="latest-finish">Finish by (HH:mm)</label>
              <input
                id="latest-finish"
                type="text"
                placeholder="10:00"
                maxLength={5}
                required
                aria-describedby="window-help"
                value={latestFinish}
                onChange={(event) => setLatestFinish(event.target.value)}
              />
            </div>
          </div>

          <p id="window-help" className="muted">
            Use a window within one day. Enter 24:00 for midnight at
            the end of that day.
          </p>
        </fieldset>

        <fieldset disabled={busy}>
          <legend>Electricity tariffs</legend>

          <label htmlFor="tariff-changeover">Tariff changeover time</label>
          <input
            id="tariff-changeover"
            type="time"
            min="00:01"
            max="23:59"
            required
            value={changeover}
            onChange={(event) => setChangeover(event.target.value)}
          />

          <div className="form-grid">
            <div>
              <label htmlFor="first-rate">
                00:00–{changeover || 'changeover'} (£/kWh)
              </label>
              <input
                id="first-rate"
                type="number"
                min="0"
                step="any"
                required
                value={firstRate}
                onChange={(event) => setFirstRate(event.target.value)}
              />
            </div>

            <div>
              <label htmlFor="second-rate">
                {changeover || 'Changeover'}–24:00 (£/kWh)
              </label>
              <input
                id="second-rate"
                type="number"
                min="0"
                step="any"
                required
                value={secondRate}
                onChange={(event) => setSecondRate(event.target.value)}
              />
            </div>
          </div>

          <button className="primary-button" type="submit">
            {busy ? 'Comparing schedules…' : 'Find recommended start'}
          </button>
        </fieldset>
      </form>

      {error && <p className="error" role="alert">{error}</p>}

      <div aria-live="polite">
        {result && (
          <>
            {!result.originalMeetsWindow && (
              <p className="window-warning">
                Your original schedule falls outside the allowed window.
                The recommendation meets your restrictions, but may cost more.
              </p>
            )}

            <div className="results-grid">
              <div className="result-card">
                <p>Original start</p>
                <strong>{formatTime(result.originalStartMinute)}</strong>
                <p className="schedule-cost">
                  {money(result.originalCostGbp)}
                </p>
              </div>

              <div className="result-card">
                <p>Recommended start</p>
                <strong>{formatTime(result.recommendedStartMinute)}</strong>
                <p className="schedule-cost">
                  Finish at{' '}
                  {formatTime(
                    result.recommendedStartMinute + result.durationMinutes,
                  )}
                  {' · '}
                  {money(result.recommendedCostGbp)}
                </p>
              </div>
            </div>

            <p className="savings-summary">
              {result.estimatedSavingsGbp >= 0
                ? `Estimated saving: ${money(result.estimatedSavingsGbp)} per run`
                : `Additional cost: ${money(Math.abs(result.estimatedSavingsGbp))} per run`}
            </p>
          </>
        )}
      </div>

      <p className="calculation-note">
        Compares starts every 15 minutes from your earliest allowed start,
        plus the original start if it fits. Assumes constant power and
        excludes standing charges.
      </p>
    </section>
  )
}