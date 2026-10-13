import { useState } from 'react'
import type { FormEvent, InputHTMLAttributes } from 'react'

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

type Preview = {
  start: number
  duration: number
  earliest: number
  latest: number
  changeover: number
  firstRate: number
  secondRate: number
}

function toMinutes(time: string): number {
  if (time === '24:00') return 1440
  if (!/^([01]\d|2[0-3]):[0-5]\d$/.test(time)) {
    throw new Error('Enter times in HH:mm format, such as 06:00.')
  }
  const [hours, minutes] = time.split(':').map(Number)
  return hours * 60 + minutes
}

function clock(minutes: number): string {
  if (minutes === 1440) return '24:00'
  const time = minutes % 1440
  const value =
    `${String(Math.floor(time / 60)).padStart(2, '0')}:` +
    String(time % 60).padStart(2, '0')

  return minutes > 1440 ? `${value} next day` : value
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

function Field({
  label,
  id,
  ...props
}: InputHTMLAttributes<HTMLInputElement> & { label: string; id: string }) {
  return (
    <div className="field">
      <label htmlFor={id}>{label}</label>
      <input id={id} {...props} />
    </div>
  )
}

function TimelineTrack({
  label,
  start,
  duration,
  variant,
  empty,
}: {
  label: string
  start?: number
  duration?: number
  variant: string
  empty?: string
}) {
  const hasRun = start !== undefined && duration !== undefined
  const firstLength = hasRun ? Math.min(duration, 1440 - start) : 0
  const overflow = hasRun ? Math.max(0, start + duration - 1440) : 0

  return (
    <div className="timeline-row">
      <div className="timeline-row-heading">
        <span>{label}</span>
        <span className="mono">
          {hasRun ? `${clock(start)} — ${clock(start + duration)}` : 'Not calculated'}
        </span>
      </div>

      <div
        className="timeline-track"
        role="img"
        aria-label={
          hasRun
            ? `${label}: ${clock(start)} to ${clock(start + duration)}`
            : `${label}: not calculated`
        }
      >
        {hasRun ? (
          <>
            <span
              className={`timeline-bar ${variant}`}
              style={{
                left: `${(start / 1440) * 100}%`,
                width: `${(firstLength / 1440) * 100}%`,
              }}
            />
            {overflow > 0 && (
              <span
                className={`timeline-bar ${variant}`}
                style={{ left: 0, width: `${(overflow / 1440) * 100}%` }}
              />
            )}
          </>
        ) : (
          <span className="track-placeholder">{empty}</span>
        )}
      </div>
    </div>
  )
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

  function readInputs(): Preview {
    const start = toMinutes(originalStart)
    const earliest = toMinutes(earliestStart)
    const latest = toMinutes(latestFinish)
    const split = toMinutes(changeover)
    const minutes = Number(duration)

    if (start >= 1440 || earliest >= 1440 || split <= 0 || split >= 1440) {
      throw new Error(
        'Start times must be before midnight. Set the tariff changeover between 00:01 and 23:59.',
      )
    }

    if (!Number.isInteger(minutes) || minutes < 1 || minutes > 1440) {
      throw new Error('Duration must be a whole number from 1 to 1440 minutes.')
    }

    if (latest <= earliest) {
      throw new Error('The finish deadline must be later than the earliest start.')
    }

    return {
      start,
      duration: minutes,
      earliest,
      latest,
      changeover: split,
      firstRate: readRate(firstRate),
      secondRate: readRate(secondRate),
    }
  }

  let preview: Preview | null = null
  try {
    preview = readInputs()
  } catch {
    // Incomplete inputs are normal while typing.
  }

  async function optimise(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setError('')
    setResult(null)

    try {
      const input = readInputs()

      if (input.duration > input.latest - input.earliest) {
        throw new Error('The appliance cannot finish within this window.')
      }

      setBusy(true)

      const response = await fetch('/api/energy/optimise', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          schedule: {
            name: appliance.name,
            powerWatts: appliance.powerWatts,
            startMinute: input.start,
            durationMinutes: input.duration,
            tariffs: [
              {
                startMinute: 0,
                endMinute: input.changeover,
                rateGbpPerKwh: input.firstRate,
              },
              {
                startMinute: input.changeover,
                endMinute: 1440,
                rateGbpPerKwh: input.secondRate,
              },
            ],
          },
          earliestStartMinute: input.earliest,
          latestFinishMinute: input.latest,
        }),
      })

      const body = await response.json()
      if (!response.ok) {
        throw new Error(body.detail ?? body.error ?? 'Could not optimise this schedule.')
      }
      setResult(body as OptimisationResult)
    } catch (error) {
      setError(error instanceof Error ? error.message : 'Optimisation failed.')
    } finally {
      setBusy(false)
    }
  }

  const maximumRate = preview
    ? Math.max(preview.firstRate, preview.secondRate, 0.01)
    : 1
  const splitX = preview ? (preview.changeover / 1440) * 1000 : 0
  const firstY = preview ? 130 - (preview.firstRate / maximumRate) * 100 : 130
  const secondY = preview ? 130 - (preview.secondRate / maximumRate) * 100 : 130
  const line = `M0 ${firstY} H${splitX} V${secondY} H1000`
  const area = `${line} V150 H0 Z`

  return (
    <div className="planner-layout">
      <form
        className="surface planner-controls"
        onSubmit={optimise}
        onChange={() => {
          setResult(null)
          setError('')
        }}
      >
        <div className="controls-heading">
          <span className="eyebrow">PLAN A RUN</span>
          <h2>{appliance.name}</h2>
          <span className="muted">
            {appliance.powerWatts.toLocaleString('en-GB')} watts
          </span>
        </div>

        <fieldset disabled={busy}>
          <legend><span>01</span> Your routine</legend>
          <div className="form-grid">
            <Field
              label="Usual start"
              id="original-start"
              type="time"
              required
              value={originalStart}
              onChange={(event) => setOriginalStart(event.target.value)}
            />
            <Field
              label="Run time · min"
              id="run-duration"
              type="number"
              min="1"
              max="1440"
              step="1"
              required
              value={duration}
              onChange={(event) => setDuration(event.target.value)}
            />
            <Field
              label="Start no earlier"
              id="earliest-start"
              type="time"
              required
              value={earliestStart}
              onChange={(event) => setEarliestStart(event.target.value)}
            />
            <Field
              label="Finish by"
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
          <p id="window-help" className="field-help">
            Same-day window. Use 24:00 for the end of the day.
          </p>
        </fieldset>

        <fieldset disabled={busy}>
          <legend><span>02</span> Your electricity rates</legend>
          <Field
            label="Rate changes at"
            id="tariff-changeover"
            type="time"
            min="00:01"
            max="23:59"
            required
            value={changeover}
            onChange={(event) => setChangeover(event.target.value)}
          />
          <div className="form-grid">
            <Field
              label="Before · £/kWh"
              id="first-rate"
              type="number"
              min="0"
              step="any"
              required
              value={firstRate}
              onChange={(event) => setFirstRate(event.target.value)}
            />
            <Field
              label="After · £/kWh"
              id="second-rate"
              type="number"
              min="0"
              step="any"
              required
              value={secondRate}
              onChange={(event) => setSecondRate(event.target.value)}
            />
          </div>
          <p className="field-help">
            Editable example rates, not live prices. £0.25 = 25p.
          </p>
          <button className="primary-button" type="submit">
            {busy ? 'Comparing…' : 'Find a better time'}
            <span aria-hidden="true">↗</span>
          </button>
        </fieldset>

        {error && <p className="error" role="alert">{error}</p>}
      </form>

      <div className="planner-visuals">
        <section className="surface day-panel" aria-labelledby="day-heading">
          <div className="section-heading">
            <div>
              <p className="eyebrow">THE BIG PICTURE</p>
              <h2 id="day-heading">Your day in energy.</h2>
            </div>
            <span className="tag">24-hour view</span>
          </div>

          {!preview ? (
            <div className="chart-empty">
              Complete the time and rate fields to preview your day.
            </div>
          ) : (
            <>
              <div className="chart-heading">
                <span>Electricity rate</span>
                <span className="mono">£ / kWh</span>
              </div>

              <div className="tariff-legend">
                <span>
                  <i className="legend-dot charcoal" />
                  00:00–{clock(preview.changeover)}
                  <strong>{money(preview.firstRate)}</strong>
                </span>
                <span>
                  <i className="legend-dot sage" />
                  {clock(preview.changeover)}–24:00
                  <strong>{money(preview.secondRate)}</strong>
                </span>
              </div>

              <svg
                className="tariff-chart"
                viewBox="0 0 1000 160"
                preserveAspectRatio="none"
                role="img"
                aria-label={
                  `Electricity rate is ${money(preview.firstRate)} per kWh ` +
                  `until ${clock(preview.changeover)}, then ` +
                  `${money(preview.secondRate)} until midnight.`
                }
              >
                {[30, 80, 130].map((y) => (
                  <line
                    key={y}
                    x1="0"
                    y1={y}
                    x2="1000"
                    y2={y}
                    stroke="#deded5"
                    strokeDasharray="3 5"
                    vectorEffect="non-scaling-stroke"
                  />
                ))}
                <path d={area} fill="#dce8bd" opacity="0.65" />
                <path
                  d={line}
                  fill="none"
                  stroke="#333d2e"
                  strokeWidth="2"
                  vectorEffect="non-scaling-stroke"
                />
                <line
                  x1={splitX}
                  y1="8"
                  x2={splitX}
                  y2="150"
                  stroke="#929c80"
                  strokeDasharray="4 5"
                  vectorEffect="non-scaling-stroke"
                />
              </svg>

              <div className="time-axis" aria-hidden="true">
                <span>00:00</span><span>06:00</span><span>12:00</span>
                <span>18:00</span><span>24:00</span>
              </div>

              <div className="schedule-tracks">
                <TimelineTrack
                  label="Allowed window"
                  start={preview.earliest}
                  duration={preview.latest - preview.earliest}
                  variant="window-bar"
                />
                <TimelineTrack
                  label="Original run"
                  start={preview.start}
                  duration={preview.duration}
                  variant="original-bar"
                />
                <TimelineTrack
                  label="Recommended run"
                  start={result?.recommendedStartMinute}
                  duration={result?.durationMinutes}
                  variant="recommended-bar"
                  empty="Your recommendation will appear here"
                />
              </div>

              {preview.start + preview.duration > 1440 && (
                <p className="field-help">
                  The original run crosses midnight; its continuation is shown
                  at the left of the timeline.
                </p>
              )}
            </>
          )}
        </section>

        <section className="recommendation-panel" aria-live="polite" aria-busy={busy}>
          {result ? (
            <>
              <div className="recommendation-top">
                <div>
                  <p className="eyebrow">YOUR RECOMMENDED START</p>
                  <p className="recommended-time">
                    {clock(result.recommendedStartMinute)}
                  </p>
                  <p className="recommendation-caption">
                    Finish at {clock(
                      result.recommendedStartMinute + result.durationMinutes,
                    )} · {result.durationMinutes} minute run
                  </p>
                </div>
                <span className="result-arrow" aria-hidden="true">↗</span>
              </div>

              <div className="cost-comparison">
                <div>
                  <span>Original cost</span>
                  <strong>{money(result.originalCostGbp)}</strong>
                </div>
                <div>
                  <span>Recommended cost</span>
                  <strong>{money(result.recommendedCostGbp)}</strong>
                </div>
                <div className="saving-cell">
                  <span>
                    {result.estimatedSavingsGbp >= 0 ? 'Saving per run' : 'Extra per run'}
                  </span>
                  <strong>{money(Math.abs(result.estimatedSavingsGbp))}</strong>
                </div>
              </div>

              {!result.originalMeetsWindow && (
                <p className="window-warning">
                  Your original run is outside the allowed window. The
                  recommendation meets your restrictions; it may cost more.
                </p>
              )}
            </>
          ) : (
            <div className="recommendation-empty">
              <span className="result-arrow" aria-hidden="true">↗</span>
              <div>
                <p className="eyebrow">A SMALL CHANGE IN ROUTINE</p>
                <h2>{busy ? 'Finding your next move…' : 'Find your better time.'}</h2>
                <p>
                  Set your rates and running window, then compare the cost
                  of switching on at a different time.
                </p>
              </div>
            </div>
          )}
        </section>

        <p className="method-note">
          <span>HOW IT WORKS</span>
          Compares starts every 15 minutes from your earliest start, plus your
          original start when it fits. Uses constant power and excludes standing
          charges. The cheapest minute between these candidates may differ.
        </p>
      </div>
    </div>
  )
}