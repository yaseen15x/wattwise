import { useEffect, useState } from 'react'
import type { FormEvent } from 'react'
import CostCalculator from './CostCalculator'
import ScheduleOptimiser from './ScheduleOptimiser'
import './App.css'

type Appliance = {
  id: string
  name: string
  powerWatts: number
  createdAt: string
}

type View = 'planner' | 'calculator' | 'appliances'

const views: { id: View; label: string; number: string }[] = [
  { id: 'planner', label: 'Energy planner', number: '01' },
  { id: 'calculator', label: 'Cost calculator', number: '02' },
  { id: 'appliances', label: 'Appliances', number: '03' },
]

const headings: Record<View, { title: string; description: string }> = {
  planner: {
    title: 'Same energy. Better timing.',
    description:
      'Explore how changing your routine could change your electricity bill.',
  },
  calculator: {
    title: 'Know what it costs.',
    description:
      'Turn watts and running time into a clearer picture of your energy use.',
  },
  appliances: {
    title: 'A little inventory. A clearer picture.',
    description:
      'Save your appliances once, then use them across your calculations.',
  },
}

async function checkResponse(response: Response): Promise<void> {
  if (response.ok) return

  const body = await response.json().catch(() => null)
  throw new Error(
    body?.detail ?? body?.error ?? `Request failed (${response.status})`,
  )
}

function App() {
  const [view, setView] = useState<View>('planner')
  const [appliances, setAppliances] = useState<Appliance[]>([])
  const [selectedId, setSelectedId] = useState('')
  const [name, setName] = useState('')
  const [power, setPower] = useState('')
  const [editingId, setEditingId] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)
  const [busy, setBusy] = useState(false)
  const [loadError, setLoadError] = useState('')
  const [actionError, setActionError] = useState('')
  const [notice, setNotice] = useState('')

  useEffect(() => {
    document.title = 'WattWise — Home energy planner'
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
          setSelectedId(data[0]?.id ?? '')
        }
      } catch (error) {
        if (!controller.signal.aborted) {
          setLoadError(
            error instanceof Error ? error.message : 'Could not load appliances.',
          )
        }
      } finally {
        if (!controller.signal.aborted) setLoading(false)
      }
    }

    void loadAppliances()
    return () => controller.abort()
  }, [])

  useEffect(() => {
    if (view === 'appliances' && editingId) {
      document.getElementById('appliance-name')?.focus()
    }
  }, [view, editingId])

  function resetForm() {
    setEditingId(null)
    setName('')
    setPower('')
    setActionError('')
  }

  function startEditing(appliance: Appliance) {
    setView('appliances')
    setEditingId(appliance.id)
    setName(appliance.name)
    setPower(String(appliance.powerWatts))
    setActionError('')
    setNotice('')
  }

  async function saveAppliance(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setActionError('')
    setNotice('')

    const watts = Number(power)

    if (!name.trim()) {
      setActionError('Enter an appliance name.')
      return
    }

    if (power.trim() === '' || !Number.isFinite(watts) || watts < 0) {
      setActionError('Enter a valid power rating of zero or more.')
      return
    }

    setBusy(true)

    try {
      const response = await fetch(
        editingId ? `/api/appliances/${editingId}` : '/api/appliances',
        {
          method: editingId ? 'PUT' : 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            name: name.trim(),
            powerWatts: watts,
          }),
        },
      )

      await checkResponse(response)
      const saved: Appliance = await response.json()

      setAppliances((current) =>
        editingId
          ? current.map((item) => item.id === saved.id ? saved : item)
          : [...current, saved],
      )
      setSelectedId(saved.id)
      setNotice(`${saved.name} ${editingId ? 'updated' : 'added'}.`)
      resetForm()
    } catch (error) {
      setActionError(
        error instanceof Error ? error.message : 'Could not save appliance.',
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

      const remaining = appliances.filter((item) => item.id !== appliance.id)
      setAppliances(remaining)

      if (selectedId === appliance.id) {
        setSelectedId(remaining[0]?.id ?? '')
      }
      if (editingId === appliance.id) resetForm()

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
  const selected = appliances.find((item) => item.id === selectedId)
  const disabled = loading || busy || Boolean(loadError)
  const componentKey = selected
    ? `${selected.id}:${selected.name}:${selected.powerWatts}`
    : ''

  return (
    <div className="app-shell">
      <a className="skip-link" href="#main-content">Skip to content</a>

      <header className="topbar">
        <a className="brand" href="#" aria-label="WattWise home">
          <span className="brand-symbol" aria-hidden="true">
            <svg viewBox="0 0 24 24" fill="none">
              <path
                d="M3 6L7 18L12 7L17 18L21 6"
                stroke="currentColor"
                strokeWidth="2.5"
                strokeLinecap="round"
                strokeLinejoin="round"
              />
            </svg>
          </span>
          WattWise<span className="brand-period">.</span>
        </a>

        <div className="topbar-caption">Small shifts. Smarter energy.</div>
        <span className="workspace-badge">HOME ENERGY LAB</span>
      </header>

      <div className="app-layout">
        <aside className="sidebar" aria-label="Workspace navigation">
          <p className="sidebar-label">WORKSPACE</p>

          <nav className="main-nav" aria-label="Main navigation">
            {views.map((item) => (
              <button
                key={item.id}
                className={`nav-item ${view === item.id ? 'active' : ''}`}
                aria-current={view === item.id ? 'page' : undefined}
                disabled={busy}
                onClick={() => setView(item.id)}
              >
                <span className="nav-number">{item.number}</span>
                {item.label}
                <span className="nav-arrow" aria-hidden="true">↗</span>
              </button>
            ))}
          </nav>

          <div className="sidebar-section-heading">
            <p className="sidebar-label">YOUR APPLIANCES</p>
            {!loading && !loadError && (
              <span className="small-count">{appliances.length}</span>
            )}
          </div>

          {loading && <p className="sidebar-message">Loading appliances…</p>}

          <div className="device-list">
            {sortedAppliances.map((appliance) => (
              <button
                key={appliance.id}
                className={`device-button ${selectedId === appliance.id ? 'selected' : ''}`}
                aria-pressed={selectedId === appliance.id}
                disabled={disabled}
                onClick={() => setSelectedId(appliance.id)}
              >
                <span className="device-icon" aria-hidden="true">↯</span>
                <span className="device-copy">
                  <strong>{appliance.name}</strong>
                  <small>
                    {appliance.powerWatts.toLocaleString('en-GB')} W
                  </small>
                </span>
                <span className="selection-dot" aria-hidden="true" />
              </button>
            ))}
          </div>

          <button
            className="add-device-button"
            disabled={disabled}
            onClick={() => {
              resetForm()
              setView('appliances')
            }}
          >
            <span aria-hidden="true">＋</span> Add an appliance
          </button>

          <div className="sidebar-note">
            <span className="note-mark" aria-hidden="true">↗</span>
            <p>A better time to switch on.</p>
            <small>
              Explore your own rates and routines. A simulation, built around you.
            </small>
          </div>
        </aside>

        <main id="main-content" className="workspace" tabIndex={-1}>
          <div className="page-heading">
            <div>
              <p className="eyebrow">YOUR HOME, IN PERSPECTIVE</p>
              <h1>{headings[view].title}</h1>
              <p className="page-description">{headings[view].description}</p>
            </div>
            <span className="chapter-number" aria-hidden="true">
              {views.find((item) => item.id === view)?.number}
            </span>
          </div>

          {notice && <p className="notice" role="status">{notice}</p>}

          {loadError ? (
            <div className="empty-panel" role="alert">
              <h2>We couldn’t load your appliances.</h2>
              <p>{loadError}</p>
              <p>Check that the Java backend and database are running.</p>
              <button onClick={() => window.location.reload()}>Try again</button>
            </div>
          ) : loading ? (
            <div className="empty-panel" role="status">
              <span className="eyebrow">GETTING READY</span>
              <h2>Loading your workspace…</h2>
            </div>
          ) : (
            <>
              {view === 'appliances' && (
                <div className="inventory-layout">
                  <section className="surface inventory-form">
                    <p className="eyebrow">APPLIANCE DETAILS</p>
                    <h2>{editingId ? 'Make an adjustment.' : 'Add to your home.'}</h2>
                    <p className="muted">
                      Use the power rating on the appliance label.
                    </p>

                    <form onSubmit={saveAppliance}>
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

                        <label htmlFor="appliance-power">Power rating · watts</label>
                        <input
                          id="appliance-power"
                          type="number"
                          value={power}
                          onChange={(event) => setPower(event.target.value)}
                          placeholder="2000"
                          min="0"
                          max="999999999.999"
                          step="0.001"
                          required
                        />

                        <button className="primary-button" type="submit">
                          {busy ? 'Saving…' : editingId ? 'Save changes' : 'Add appliance'}
                          <span aria-hidden="true">↗</span>
                        </button>

                        {editingId && (
                          <button
                            className="cancel-button"
                            type="button"
                            onClick={resetForm}
                          >
                            Cancel editing
                          </button>
                        )}
                      </fieldset>
                    </form>

                    {actionError && <p className="error" role="alert">{actionError}</p>}
                  </section>

                  <section className="surface inventory-list">
                    <div className="section-heading">
                      <h2>Your collection</h2>
                      <span className="tag">{appliances.length} saved</span>
                    </div>

                    {appliances.length === 0 && (
                      <p className="empty-copy">
                        Start with one appliance you use regularly.
                      </p>
                    )}

                    <ul className="appliance-list">
                      {sortedAppliances.map((appliance) => (
                        <li key={appliance.id} className="appliance-row">
                          <div>
                            <h3>{appliance.name}</h3>
                            <p className="muted">
                              {appliance.powerWatts.toLocaleString('en-GB')} W
                            </p>
                          </div>
                          <div className="row-actions">
                            <button
                              disabled={disabled}
                              onClick={() => startEditing(appliance)}
                              aria-label={`Edit ${appliance.name}`}
                            >
                              Edit
                            </button>
                            <button
                              className="delete-button"
                              disabled={disabled}
                              onClick={() => void deleteAppliance(appliance)}
                              aria-label={`Delete ${appliance.name}`}
                            >
                              Delete
                            </button>
                          </div>
                        </li>
                      ))}
                    </ul>
                  </section>
                </div>
              )}

              {view !== 'appliances' && !selected && (
                <div className="empty-panel">
                  <p className="eyebrow">LET’S START WITH ONE</p>
                  <h2>Your home’s energy story starts here.</h2>
                  <p>Add an appliance to explore its costs and running schedule.</p>
                  <button
                    className="primary-button"
                    onClick={() => setView('appliances')}
                  >
                    Add your first appliance <span aria-hidden="true">↗</span>
                  </button>
                </div>
              )}

              {view === 'planner' && selected && (
                <ScheduleOptimiser key={componentKey} appliance={selected} />
              )}

              {view === 'calculator' && selected && (
                <section className="surface calculator-surface">
                  <div className="section-heading">
                    <div>
                      <p className="eyebrow">THE EVERYDAY NUMBERS</p>
                      <h2>{selected.name}</h2>
                    </div>
                    <span className="tag">
                      {selected.powerWatts.toLocaleString('en-GB')} W
                    </span>
                  </div>
                  <CostCalculator
                    key={componentKey}
                    applianceId={selected.id}
                    applianceName={selected.name}
                  />
                </section>
              )}
            </>
          )}

          <footer className="workspace-footer">
            <span>WATTWISE / HOME ENERGY SIMULATOR</span>
            <span>Plan thoughtfully. Switch on wisely.</span>
          </footer>
        </main>
      </div>
    </div>
  )
}

export default App