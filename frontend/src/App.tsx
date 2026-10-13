import { useEffect, useState } from 'react'
import type { FormEvent } from 'react'
import './App.css'

type Appliance = {
  id: string
  name: string
  powerWatts: number
  createdAt: string
}

async function checkResponse(response: Response): Promise<void> {
  if (response.ok) return

  const body = await response.json().catch(() => null)

  throw new Error(
    body?.detail ?? body?.error ?? `Request failed (${response.status})`,
  )
}

function App() {
  const [appliances, setAppliances] = useState<Appliance[]>([])
  const [name, setName] = useState('')
  const [power, setPower] = useState('')
  const [loading, setLoading] = useState(true)
  const [busy, setBusy] = useState(false)
  const [loadError, setLoadError] = useState('')
  const [actionError, setActionError] = useState('')
  const [notice, setNotice] = useState('')

  useEffect(() => {
    const controller = new AbortController()

    async function loadAppliances() {
      try {
        const response = await fetch('/api/appliances', {
          signal: controller.signal,
        })

        await checkResponse(response)
        const data: Appliance[] = await response.json()

        if (!controller.signal.aborted) {
          setAppliances(data)
        }
      } catch (error) {
        if (!controller.signal.aborted) {
          setLoadError(
            error instanceof Error
              ? error.message
              : 'Could not load appliances.',
          )
        }
      } finally {
        if (!controller.signal.aborted) {
          setLoading(false)
        }
      }
    }

    void loadAppliances()

    return () => controller.abort()
  }, [])

  async function addAppliance(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setActionError('')
    setNotice('')

    if (!name.trim()) {
      setActionError('Enter an appliance name.')
      return
    }

    const watts = Number(power)

    if (power.trim() === '' || !Number.isFinite(watts) || watts < 0) {
      setActionError('Enter a valid power rating of zero or more.')
      return
    }

    setBusy(true)

    try {
      const response = await fetch('/api/appliances', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          name: name.trim(),
          powerWatts: watts,
        }),
      })

      await checkResponse(response)
      const saved: Appliance = await response.json()

      setAppliances((current) => [...current, saved])
      setName('')
      setPower('')
      setNotice(`${saved.name} added.`)
    } catch (error) {
      setActionError(
        error instanceof Error ? error.message : 'Could not add appliance.',
      )
    } finally {
      setBusy(false)
    }
  }

  async function deleteAppliance(appliance: Appliance) {
    if (!window.confirm(`Delete "${appliance.name}"?`)) return

    setBusy(true)
    setActionError('')
    setNotice('')

    try {
      const response = await fetch(`/api/appliances/${appliance.id}`, {
        method: 'DELETE',
      })

      await checkResponse(response)

      setAppliances((current) =>
        current.filter((item) => item.id !== appliance.id),
      )
      setNotice(`${appliance.name} deleted.`)
    } catch (error) {
      setActionError(
        error instanceof Error ? error.message : 'Could not delete appliance.',
      )
    } finally {
      setBusy(false)
    }
  }

  const sortedAppliances = [...appliances].sort((a, b) =>
    a.name.localeCompare(b.name),
  )

  const disabled = loading || busy || Boolean(loadError)

  return (
    <main className="dashboard">
      <header className="page-header">
        <p className="eyebrow">HOME ENERGY SIMULATOR</p>
        <h1>WattWise<span>.</span></h1>
        <p>Understand what your appliances use and what they cost.</p>
      </header>

      <div className="dashboard-grid">
        <section className="panel" aria-labelledby="add-heading">
          <h2 id="add-heading">Add an appliance</h2>
          <p className="muted">
            Enter the power rating from its label or specifications.
          </p>

          <form onSubmit={addAppliance}>
            <fieldset disabled={disabled}>
              <label htmlFor="appliance-name">Appliance name</label>
              <input
                id="appliance-name"
                value={name}
                onChange={(event) => setName(event.target.value)}
                placeholder="e.g. Washing machine"
                maxLength={100}
                required
              />

              <label htmlFor="appliance-power">Power rating (watts)</label>
              <input
                id="appliance-power"
                type="number"
                value={power}
                onChange={(event) => setPower(event.target.value)}
                placeholder="e.g. 2000"
                min="0"
                max="999999999.999"
                step="0.001"
                required
              />

              <button className="primary-button" type="submit">
                {busy ? 'Saving changes…' : 'Add appliance'}
              </button>
            </fieldset>
          </form>
        </section>

        <section className="panel" aria-labelledby="saved-heading">
          <div className="section-heading">
            <h2 id="saved-heading">Your appliances</h2>
            {!loading && !loadError && (
              <span className="count">{appliances.length} saved</span>
            )}
          </div>

          {loading && <p role="status">Loading appliances…</p>}

          {loadError && (
            <div role="alert">
              <p className="error">{loadError}</p>
              <p className="muted">
                Check that your backend and database are running.
              </p>
              <button onClick={() => window.location.reload()}>
                Try again
              </button>
            </div>
          )}

          {!loading && !loadError && appliances.length === 0 && (
            <p className="empty-state">
              No appliances yet. Add your first one using the form.
            </p>
          )}

          <ul className="appliance-list">
            {sortedAppliances.map((appliance) => (
              <li key={appliance.id} className="appliance-row">
                <div>
                  <h3>{appliance.name}</h3>
                  <p className="muted">
                    {appliance.powerWatts.toLocaleString('en-GB', {
                      maximumFractionDigits: 3,
                    })}{' '}
                    W
                  </p>
                </div>

                <button
                  className="delete-button"
                  disabled={disabled}
                  onClick={() => void deleteAppliance(appliance)}
                  aria-label={`Delete ${appliance.name}`}
                >
                  Delete
                </button>
              </li>
            ))}
          </ul>
        </section>
      </div>

      {actionError && <p className="error" role="alert">{actionError}</p>}
      <p className="notice" role="status">{notice}</p>
    </main>
  )
}

export default App