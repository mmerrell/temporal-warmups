'use client'

import { useEffect, useState } from 'react'
import { useParams } from 'next/navigation'

type Mode = 'temporal' | 'pre-temporal' | 'loading'

interface Step {
  name: string
  status: 'pending' | 'in_progress' | 'completed' | 'failed' | 'waiting' | 'skipped'
  startTime: string | null
  endTime: string | null
  detail: string | null
  team: 'payments' | 'compliance'
}

interface HistoryEvent {
  eventId: string
  eventType: string
  eventTime: string
  detail: string
}

const TRANSACTION_META: Record<string, { amount: number; route: string; description: string }> = {
  'payment-TXN-001': { amount: 250.00, route: 'US -> US', description: 'Monthly rent payment' },
  'payment-TXN-002': { amount: 49999.00, route: 'US -> Cayman Islands', description: 'Investment fund transfer' },
  'payment-TXN-003': { amount: 12.50, route: 'US -> US', description: 'Coffee shop purchase' },
  'payment-TXN-004': { amount: 150000.00, route: 'Russia -> US', description: 'Business consulting payment' },
  'payment-TXN-005': { amount: 9999.00, route: 'US -> US', description: 'Cash deposit' },
}

const PRE_TEMPORAL_CALLOUTS: Record<string, string> = {
  'Validate Payment': 'No retry if validation service is down',
  'Fraud Screening': 'Direct API call — if it fails, the whole payment fails. No retries.',
  'Categorize Transaction': 'Another direct API call — if step 2 succeeded but this fails, the fraud check was wasted',
  'Approval Wait': 'No mechanism to pause and wait for human review!',
  'Execute Payment': 'No compensation if this fails after previous steps succeeded',
}

function stepIcon(status: Step['status']) {
  switch (status) {
    case 'completed':
      return (
        <div className="w-10 h-10 rounded-full bg-green-900/50 border-2 border-green-500 flex items-center justify-center">
          <svg className="w-5 h-5 text-green-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2.5} d="M5 13l4 4L19 7" />
          </svg>
        </div>
      )
    case 'in_progress':
      return (
        <div className="w-10 h-10 rounded-full bg-blue-900/50 border-2 border-blue-500 flex items-center justify-center animate-pulse">
          <div className="w-3 h-3 rounded-full bg-blue-400" />
        </div>
      )
    case 'failed':
      return (
        <div className="w-10 h-10 rounded-full bg-red-900/50 border-2 border-red-500 flex items-center justify-center">
          <svg className="w-5 h-5 text-red-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2.5} d="M6 18L18 6M6 6l12 12" />
          </svg>
        </div>
      )
    case 'waiting':
      return (
        <div className="w-10 h-10 rounded-full bg-yellow-900/50 border-2 border-yellow-500 flex items-center justify-center animate-pulse">
          <svg className="w-5 h-5 text-yellow-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
          </svg>
        </div>
      )
    case 'skipped':
      return (
        <div className="w-10 h-10 rounded-full bg-orange-900/50 border-2 border-orange-500 flex items-center justify-center">
          <svg className="w-5 h-5 text-orange-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-2.5L13.732 4.5c-.77-.833-2.694-.833-3.464 0L3.34 16.5c-.77.833.192 2.5 1.732 2.5z" />
          </svg>
        </div>
      )
    default: // pending
      return (
        <div className="w-10 h-10 rounded-full bg-gray-800 border-2 border-gray-600 flex items-center justify-center">
          <div className="w-3 h-3 rounded-full bg-gray-600" />
        </div>
      )
  }
}

function connectorColor(fromStatus: Step['status'], toStatus: Step['status']) {
  if (fromStatus === 'completed' && toStatus !== 'pending') return 'bg-green-600'
  if (fromStatus === 'completed') return 'bg-gray-700'
  return 'bg-gray-700'
}

function formatDuration(start: string | null, end: string | null): string {
  if (!start) return '-'
  const s = new Date(start).getTime()
  const e = end ? new Date(end).getTime() : Date.now()
  const ms = e - s
  if (ms < 1000) return `${ms}ms`
  if (ms < 60000) return `${(ms / 1000).toFixed(1)}s`
  return `${(ms / 60000).toFixed(1)}m`
}

function teamBadge(team: Step['team'], mode: Mode) {
  if (team === 'compliance') {
    return (
      <span className="text-[10px] uppercase tracking-wider px-1.5 py-0.5 rounded bg-cyan-900/50 text-cyan-400 border border-cyan-800">
        {mode === 'temporal' ? 'Compliance (Nexus)' : 'Compliance (REST)'}
      </span>
    )
  }
  return <span className="text-[10px] uppercase tracking-wider px-1.5 py-0.5 rounded bg-indigo-900/50 text-indigo-400 border border-indigo-800">Payments</span>
}

function statusLabel(status: Step['status']) {
  const styles: Record<string, string> = {
    completed: 'text-green-400',
    in_progress: 'text-blue-400',
    failed: 'text-red-400',
    waiting: 'text-yellow-400',
    skipped: 'text-orange-400',
    pending: 'text-gray-500',
  }
  const labels: Record<string, string> = {
    completed: 'Completed',
    in_progress: 'In Progress',
    failed: 'Failed',
    waiting: 'Waiting',
    skipped: 'Skipped',
    pending: 'Pending',
  }
  return <span className={`text-xs font-medium ${styles[status]}`}>{labels[status]}</span>
}

export default function TransactionDetailPage() {
  const params = useParams()
  const workflowId = params.id as string

  const [steps, setSteps] = useState<Step[]>([])
  const [events, setEvents] = useState<HistoryEvent[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [showEvents, setShowEvents] = useState(false)
  const [mode, setMode] = useState<Mode>('loading')

  const fetchHistory = async () => {
    // Try Temporal first
    try {
      const res = await fetch(`/api/workflows/${encodeURIComponent(workflowId)}/history`)
      if (res.ok) {
        const data = await res.json()
        if (data.steps && data.steps.length > 0) {
          setSteps(data.steps)
          setEvents(data.events || [])
          setMode('temporal')
          setError(null)
          setLoading(false)
          return
        }
      }
    } catch {
      // Temporal unavailable
    }

    // Fall back to pre-temporal state
    try {
      const res = await fetch(`/api/processing/state?id=${encodeURIComponent(workflowId)}`)
      if (res.ok) {
        const data = await res.json()
        setSteps(data.steps || [])
        setEvents([])
        setMode('pre-temporal')
        setError(null)
        setLoading(false)
        return
      }
    } catch {
      // Neither available
    }

    setError('Cannot load transaction data. Is the dashboard or Temporal running?')
    setLoading(false)
  }

  useEffect(() => {
    fetchHistory()
    const interval = setInterval(fetchHistory, 2000)
    return () => clearInterval(interval)
  }, [workflowId])

  const meta = TRANSACTION_META[workflowId]

  return (
    <div className="space-y-8">
      {/* Breadcrumb */}
      <div className="flex items-center gap-2 text-sm text-gray-400">
        <a href="/" className="hover:text-white transition">Transactions</a>
        <span>/</span>
        <span className="text-gray-200">{workflowId}</span>
      </div>

      {/* Mode Banner */}
      {mode === 'pre-temporal' && (
        <div className="bg-orange-900/30 border border-orange-700 rounded-lg px-4 py-3 flex items-center gap-3">
          <div className="w-2.5 h-2.5 rounded-full bg-orange-500 animate-pulse" />
          <div>
            <span className="font-semibold text-orange-300">Pre-Temporal Mode</span>
            <span className="text-orange-400/80 text-sm ml-2">
              Direct REST calls — observe the problems below
            </span>
          </div>
        </div>
      )}
      {mode === 'temporal' && (
        <div className="bg-green-900/30 border border-green-700 rounded-lg px-4 py-3 flex items-center gap-3">
          <div className="w-2.5 h-2.5 rounded-full bg-green-500" />
          <div>
            <span className="font-semibold text-green-300">Temporal Nexus Mode</span>
            <span className="text-green-400/80 text-sm ml-2">
              Durable workflow with full event history
            </span>
          </div>
        </div>
      )}

      {/* Header */}
      <div className="flex items-start justify-between">
        <div>
          <h2 className="text-2xl font-bold mb-1 font-mono">{workflowId}</h2>
          {meta && (
            <p className="text-gray-400 text-sm">
              {meta.description} &middot; ${meta.amount.toLocaleString('en-US', { minimumFractionDigits: 2 })} &middot; {meta.route}
            </p>
          )}
        </div>
        {mode === 'temporal' && (
          <a
            href={`http://localhost:8233/namespaces/default/workflows/${workflowId}`}
            target="_blank"
            className="text-xs text-indigo-400 hover:text-indigo-300 border border-indigo-800 rounded px-3 py-1.5 transition"
          >
            View in Temporal UI
          </a>
        )}
      </div>

      {error && (
        <div className="bg-red-900/30 border border-red-800 rounded-lg p-4 text-red-300 text-sm">
          {error}
        </div>
      )}

      {loading ? (
        <div className="text-gray-500">Loading workflow history...</div>
      ) : (
        <>
          {/* Step Pipeline - Horizontal */}
          <div className="bg-gray-900 border border-gray-800 rounded-lg p-6">
            <h3 className="text-sm font-semibold text-gray-400 uppercase tracking-wider mb-6">
              {mode === 'temporal' ? 'Workflow Pipeline' : 'Processing Pipeline (Pre-Temporal)'}
            </h3>
            <div className="flex items-center justify-between">
              {steps.map((step, i) => (
                <div key={i} className="flex items-center flex-1">
                  <div className="flex flex-col items-center text-center min-w-0">
                    {stepIcon(step.status)}
                    <div className="mt-2 text-xs font-medium text-gray-200 leading-tight">{step.name}</div>
                    <div className="mt-0.5">{statusLabel(step.status)}</div>
                    <div className="mt-1">{teamBadge(step.team, mode)}</div>
                  </div>
                  {i < steps.length - 1 && (
                    <div className={`h-0.5 flex-1 mx-2 mt-[-2rem] ${connectorColor(step.status, steps[i + 1].status)}`} />
                  )}
                </div>
              ))}
            </div>
          </div>

          {/* Step Detail Cards */}
          <div className="grid grid-cols-1 gap-3">
            {steps.map((step, i) => (
              <div
                key={i}
                className={`bg-gray-900 border rounded-lg p-4 ${
                  step.status === 'failed'
                    ? 'border-red-800'
                    : step.status === 'waiting'
                    ? 'border-yellow-800'
                    : step.status === 'in_progress'
                    ? 'border-blue-800'
                    : step.status === 'completed'
                    ? 'border-gray-700'
                    : step.status === 'skipped'
                    ? 'border-orange-800'
                    : 'border-gray-800 opacity-50'
                }`}
              >
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-3">
                    <span className="text-gray-500 text-sm font-mono w-6">#{i + 1}</span>
                    <span className="font-medium text-sm">{step.name}</span>
                    {teamBadge(step.team, mode)}
                  </div>
                  <div className="flex items-center gap-4">
                    {statusLabel(step.status)}
                    <span className="text-xs text-gray-500 font-mono">
                      {step.status !== 'pending' ? formatDuration(step.startTime, step.endTime) : ''}
                    </span>
                  </div>
                </div>
                {step.detail && step.status !== 'pending' && (
                  <div className={`mt-2 text-xs pl-9 ${step.status === 'failed' ? 'text-red-400' : 'text-gray-400'}`}>
                    {step.detail}
                  </div>
                )}
                {/* Pre-temporal callout */}
                {mode === 'pre-temporal' && PRE_TEMPORAL_CALLOUTS[step.name] && (
                  <div className="mt-2 text-xs pl-9 text-orange-400/70 flex items-start gap-1.5">
                    <span className="text-orange-500">⚠</span>
                    <span>{PRE_TEMPORAL_CALLOUTS[step.name]}</span>
                  </div>
                )}
              </div>
            ))}
          </div>

          {/* Raw Event Timeline (Temporal mode only) */}
          {mode === 'temporal' && events.length > 0 && (
            <div className="bg-gray-900 border border-gray-800 rounded-lg">
              <button
                onClick={() => setShowEvents(!showEvents)}
                className="w-full px-4 py-3 flex items-center justify-between text-sm text-gray-400 hover:text-gray-200 transition"
              >
                <span>Raw Temporal Events ({events.filter(e => e.detail).length})</span>
                <svg
                  className={`w-4 h-4 transition-transform ${showEvents ? 'rotate-180' : ''}`}
                  fill="none"
                  viewBox="0 0 24 24"
                  stroke="currentColor"
                >
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 9l-7 7-7-7" />
                </svg>
              </button>
              {showEvents && (
                <div className="border-t border-gray-800 max-h-96 overflow-y-auto">
                  <table className="w-full text-xs">
                    <thead>
                      <tr className="text-left text-gray-500 border-b border-gray-800">
                        <th className="px-4 py-2 w-12">#</th>
                        <th className="px-4 py-2">Event Type</th>
                        <th className="px-4 py-2">Time</th>
                        <th className="px-4 py-2">Detail</th>
                      </tr>
                    </thead>
                    <tbody>
                      {events
                        .filter(e => e.detail)
                        .map(e => (
                          <tr key={e.eventId} className="border-b border-gray-800/50 hover:bg-gray-800/30">
                            <td className="px-4 py-1.5 text-gray-500 font-mono">{e.eventId}</td>
                            <td className="px-4 py-1.5 font-mono text-gray-300">
                              {e.eventType.replace('EVENT_TYPE_', '')}
                            </td>
                            <td className="px-4 py-1.5 text-gray-500">
                              {new Date(e.eventTime).toLocaleTimeString()}
                            </td>
                            <td className="px-4 py-1.5 text-gray-400">{e.detail}</td>
                          </tr>
                        ))}
                    </tbody>
                  </table>
                </div>
              )}
            </div>
          )}

          {/* Pre-temporal: No event history callout */}
          {mode === 'pre-temporal' && (
            <div className="bg-gray-900 border border-orange-900/50 rounded-lg p-4 text-center text-sm text-orange-400/70">
              No event history available in pre-Temporal mode. With Temporal, every step is recorded as a durable event.
            </div>
          )}
        </>
      )}
    </div>
  )
}
