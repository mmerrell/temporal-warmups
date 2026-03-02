'use client'

import { useEffect, useState } from 'react'

interface Workflow {
  workflowId: string
  status: string
  startTime: string
  closeTime?: string
}

type Mode = 'temporal' | 'pre-temporal' | 'loading'

const TRANSACTION_META: Record<string, { amount: number; route: string; description: string }> = {
  'payment-TXN-001': { amount: 250.00, route: 'US -> US', description: 'Monthly rent payment' },
  'payment-TXN-002': { amount: 49999.00, route: 'US -> Cayman Islands', description: 'Investment fund transfer' },
  'payment-TXN-003': { amount: 12.50, route: 'US -> US', description: 'Coffee shop purchase' },
  'payment-TXN-004': { amount: 150000.00, route: 'Russia -> US', description: 'Business consulting payment' },
  'payment-TXN-005': { amount: 9999.00, route: 'US -> US', description: 'Cash deposit' },
}

function getRiskBadge(workflowId: string) {
  if (workflowId.includes('TXN-001') || workflowId.includes('TXN-003')) {
    return <span className="px-2 py-0.5 rounded-full text-xs font-medium bg-green-900/50 text-green-400 border border-green-800">LOW</span>
  }
  if (workflowId.includes('TXN-005')) {
    return <span className="px-2 py-0.5 rounded-full text-xs font-medium bg-yellow-900/50 text-yellow-400 border border-yellow-800">MEDIUM</span>
  }
  if (workflowId.includes('TXN-002')) {
    return <span className="px-2 py-0.5 rounded-full text-xs font-medium bg-orange-900/50 text-orange-400 border border-orange-800">HIGH</span>
  }
  if (workflowId.includes('TXN-004')) {
    return <span className="px-2 py-0.5 rounded-full text-xs font-medium bg-red-900/50 text-red-400 border border-red-800">CRITICAL</span>
  }
  return <span className="px-2 py-0.5 rounded-full text-xs font-medium bg-gray-800 text-gray-400">UNKNOWN</span>
}

function getStatusBadge(status: string) {
  if (status === 'Running') {
    return <span className="px-2 py-0.5 rounded-full text-xs font-medium bg-blue-900/50 text-blue-400 border border-blue-800 animate-pulse">Running</span>
  }
  if (status === 'Completed') {
    return <span className="px-2 py-0.5 rounded-full text-xs font-medium bg-green-900/50 text-green-400 border border-green-800">Completed</span>
  }
  if (status === 'Failed') {
    return <span className="px-2 py-0.5 rounded-full text-xs font-medium bg-red-900/50 text-red-400 border border-red-800">Failed</span>
  }
  return <span className="px-2 py-0.5 rounded-full text-xs font-medium bg-gray-800 text-gray-400">{status}</span>
}

export default function TransactionsPage() {
  const [workflows, setWorkflows] = useState<Workflow[]>([])
  const [loading, setLoading] = useState(true)
  const [mode, setMode] = useState<Mode>('loading')

  const fetchWorkflows = async () => {
    // Try Temporal first
    try {
      const res = await fetch('/api/workflows')
      if (res.ok) {
        const data = await res.json()
        const wfs = data.workflows || []
        if (wfs.length > 0) {
          setWorkflows(wfs)
          setMode('temporal')
          setLoading(false)
          return
        }
      }
    } catch {
      // Temporal unavailable, try pre-temporal
    }

    // Fall back to pre-temporal state
    try {
      const res = await fetch('/api/processing/state')
      if (res.ok) {
        const data = await res.json()
        const wfs = data.workflows || []
        if (wfs.length > 0) {
          setWorkflows(wfs)
          setMode('pre-temporal')
          setLoading(false)
          return
        }
      }
    } catch {
      // Neither source has data
    }

    // No data from either source
    setWorkflows([])
    setMode(mode === 'loading' ? 'pre-temporal' : mode)
    setLoading(false)
  }

  useEffect(() => {
    fetchWorkflows()
    const interval = setInterval(fetchWorkflows, 2000)
    return () => clearInterval(interval)
  }, [])

  const paymentWorkflows = workflows.filter(w => w.workflowId.startsWith('payment-'))
  const fraudWorkflows = workflows.filter(w => w.workflowId.startsWith('fraud-screen-'))

  return (
    <div className="space-y-8">
      {/* Mode Banner */}
      {mode === 'pre-temporal' && (
        <div className="bg-orange-900/30 border border-orange-700 rounded-lg px-4 py-3 flex items-center gap-3">
          <div className="w-2.5 h-2.5 rounded-full bg-orange-500 animate-pulse" />
          <div>
            <span className="font-semibold text-orange-300">Pre-Temporal Mode</span>
            <span className="text-orange-400/80 text-sm ml-2">
              Direct REST calls — no retries, no durability, no human approval
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
              Durable workflows with cross-team Nexus calls, retries, and human-in-the-loop
            </span>
          </div>
        </div>
      )}

      <div>
        <h2 className="text-2xl font-bold mb-1">Transaction Overview</h2>
        <p className="text-gray-400 text-sm">
          {mode === 'temporal'
            ? 'Payment workflows processing via Temporal Nexus'
            : 'Payment processing via direct REST calls (pre-Temporal baseline)'}
        </p>
      </div>

      <div className="grid grid-cols-3 gap-4">
        <div className="bg-gray-900 border border-gray-800 rounded-lg p-5">
          <div className="text-3xl font-bold text-indigo-400">{paymentWorkflows.length}</div>
          <div className="text-gray-400 text-sm mt-1">
            {mode === 'temporal' ? 'Payment Workflows' : 'Transactions'}
          </div>
        </div>
        <div className="bg-gray-900 border border-gray-800 rounded-lg p-5">
          <div className="text-3xl font-bold text-cyan-400">
            {mode === 'temporal' ? fraudWorkflows.length : '—'}
          </div>
          <div className="text-gray-400 text-sm mt-1">
            {mode === 'temporal' ? 'Fraud Screenings (Nexus)' : 'Fraud Screenings (N/A)'}
          </div>
        </div>
        <div className="bg-gray-900 border border-gray-800 rounded-lg p-5">
          <div className="text-3xl font-bold text-green-400">
            {paymentWorkflows.filter(w => w.status === 'Completed').length}
          </div>
          <div className="text-gray-400 text-sm mt-1">Completed</div>
        </div>
      </div>

      <div>
        <h3 className="text-lg font-semibold mb-3">
          {mode === 'temporal' ? 'Payment Workflows' : 'Payment Transactions'}
        </h3>
        {loading ? (
          <div className="text-gray-500">Loading...</div>
        ) : paymentWorkflows.length === 0 ? (
          <div className="bg-gray-900 border border-gray-800 rounded-lg p-8 text-center text-gray-500">
            {mode === 'temporal'
              ? <>No payment workflows found. Run the starter: <code className="text-indigo-400">mvn compile exec:java@starter</code></>
              : <>No transactions yet. Run: <code className="text-orange-400">mvn compile exec:java</code></>}
          </div>
        ) : (
          <div className="bg-gray-900 border border-gray-800 rounded-lg overflow-hidden">
            <table className="w-full">
              <thead>
                <tr className="border-b border-gray-800 text-left text-sm text-gray-400">
                  <th className="px-4 py-3">Transaction</th>
                  <th className="px-4 py-3">Amount</th>
                  <th className="px-4 py-3">Route</th>
                  <th className="px-4 py-3">Description</th>
                  <th className="px-4 py-3">Risk</th>
                  <th className="px-4 py-3">Status</th>
                </tr>
              </thead>
              <tbody>
                {paymentWorkflows.map(w => {
                  const meta = TRANSACTION_META[w.workflowId]
                  return (
                    <tr key={w.workflowId} className="border-b border-gray-800/50 hover:bg-gray-800/30">
                      <td className="px-4 py-3">
                        <a href={`/transaction/${encodeURIComponent(w.workflowId)}`}
                           className="text-indigo-400 hover:text-indigo-300 font-mono text-sm">
                          {w.workflowId}
                        </a>
                      </td>
                      <td className="px-4 py-3 font-mono">
                        {meta ? `$${meta.amount.toLocaleString('en-US', { minimumFractionDigits: 2 })}` : '-'}
                      </td>
                      <td className="px-4 py-3 text-sm text-gray-300">{meta?.route || '-'}</td>
                      <td className="px-4 py-3 text-sm text-gray-400">{meta?.description || '-'}</td>
                      <td className="px-4 py-3">{getRiskBadge(w.workflowId)}</td>
                      <td className="px-4 py-3">{getStatusBadge(w.status)}</td>
                    </tr>
                  )
                })}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {mode === 'temporal' && fraudWorkflows.length > 0 && (
        <div>
          <h3 className="text-lg font-semibold mb-3">Compliance Fraud Screenings (via Nexus)</h3>
          <div className="bg-gray-900 border border-gray-800 rounded-lg overflow-hidden">
            <table className="w-full">
              <thead>
                <tr className="border-b border-gray-800 text-left text-sm text-gray-400">
                  <th className="px-4 py-3">Workflow ID</th>
                  <th className="px-4 py-3">Status</th>
                  <th className="px-4 py-3">Started</th>
                </tr>
              </thead>
              <tbody>
                {fraudWorkflows.map(w => (
                  <tr key={w.workflowId} className="border-b border-gray-800/50 hover:bg-gray-800/30">
                    <td className="px-4 py-3">
                      <a href={`http://localhost:8233/namespaces/default/workflows/${w.workflowId}`}
                         target="_blank"
                         className="text-cyan-400 hover:text-cyan-300 font-mono text-sm">
                        {w.workflowId}
                      </a>
                    </td>
                    <td className="px-4 py-3">{getStatusBadge(w.status)}</td>
                    <td className="px-4 py-3 text-sm text-gray-400">
                      {new Date(w.startTime).toLocaleTimeString()}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* Pre-temporal problems callout */}
      {mode === 'pre-temporal' && paymentWorkflows.length > 0 && (
        <div className="bg-orange-950/30 border border-orange-900/50 rounded-lg p-5 space-y-3">
          <h3 className="text-orange-300 font-semibold">Problems with this approach</h3>
          <div className="grid grid-cols-2 gap-2 text-sm text-orange-400/80">
            <div className="flex items-start gap-2">
              <span className="text-red-400 mt-0.5">✗</span>
              <span>No retries — API failure = lost transaction</span>
            </div>
            <div className="flex items-start gap-2">
              <span className="text-red-400 mt-0.5">✗</span>
              <span>No durability — crash = lost state</span>
            </div>
            <div className="flex items-start gap-2">
              <span className="text-red-400 mt-0.5">✗</span>
              <span>No human approval — high-risk auto-processed</span>
            </div>
            <div className="flex items-start gap-2">
              <span className="text-red-400 mt-0.5">✗</span>
              <span>Tight coupling — Compliance outage blocks ALL payments</span>
            </div>
            <div className="flex items-start gap-2">
              <span className="text-red-400 mt-0.5">✗</span>
              <span>No cross-team visibility or audit trail</span>
            </div>
            <div className="flex items-start gap-2">
              <span className="text-green-400 mt-0.5">→</span>
              <span className="text-green-400/80">Solution: Temporal Nexus!</span>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
