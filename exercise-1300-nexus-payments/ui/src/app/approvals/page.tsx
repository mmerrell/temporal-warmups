'use client'

import { useEffect, useState } from 'react'

interface Workflow {
  workflowId: string
  status: string
  startTime: string
}

const RISK_INFO: Record<string, { amount: number; route: string; risk: string; reason: string }> = {
  'payment-TXN-002': { amount: 49999, route: 'US -> Cayman Islands', risk: 'HIGH', reason: 'Tax haven destination, large transfer' },
  'payment-TXN-004': { amount: 150000, route: 'Russia -> US', risk: 'CRITICAL', reason: 'OFAC sanctioned country, large amount' },
  'payment-TXN-005': { amount: 9999, route: 'US -> US', risk: 'MEDIUM', reason: 'Just under $10k CTR threshold (structuring?)' },
}

export default function ApprovalsPage() {
  const [workflows, setWorkflows] = useState<Workflow[]>([])
  const [sending, setSending] = useState<string | null>(null)
  const [results, setResults] = useState<Record<string, string>>({})

  const fetchWorkflows = async () => {
    try {
      const res = await fetch('/api/workflows')
      if (!res.ok) return
      const data = await res.json()
      setWorkflows(data.workflows || [])
    } catch {}
  }

  useEffect(() => {
    fetchWorkflows()
    const interval = setInterval(fetchWorkflows, 3000)
    return () => clearInterval(interval)
  }, [])

  const pendingApprovals = workflows
    .filter(w => w.status === 'Running' && RISK_INFO[w.workflowId])

  const sendSignal = async (workflowId: string, approved: boolean) => {
    setSending(workflowId)
    try {
      const res = await fetch('/api/signal', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          workflowId,
          signalName: 'approveTransaction',
          input: {
            approved,
            reviewerName: 'Dashboard User',
            reason: approved ? 'Approved via dashboard' : 'Rejected via dashboard',
          },
        }),
      })
      if (res.ok) {
        setResults(prev => ({ ...prev, [workflowId]: approved ? 'Approved' : 'Rejected' }))
      } else {
        const errText = await res.text()
        setResults(prev => ({ ...prev, [workflowId]: 'Error: ' + errText }))
      }
    } catch (e) {
      setResults(prev => ({ ...prev, [workflowId]: 'Failed to send signal' }))
    } finally {
      setSending(null)
    }
  }

  return (
    <div className="space-y-8">
      <div>
        <h2 className="text-2xl font-bold mb-1">Approval Queue</h2>
        <p className="text-gray-400 text-sm">Review and approve/reject high-risk transactions (sends Temporal Signals)</p>
      </div>

      {pendingApprovals.length === 0 ? (
        <div className="bg-gray-900 border border-gray-800 rounded-lg p-12 text-center">
          <div className="text-gray-500 text-lg mb-2">No pending approvals</div>
          <div className="text-gray-600 text-sm">
            High-risk transactions (TXN-002, TXN-004, TXN-005) will appear here when running.
          </div>
        </div>
      ) : (
        <div className="grid gap-4">
          {pendingApprovals.map(w => {
            const info = RISK_INFO[w.workflowId]!
            const result = results[w.workflowId]
            const isProcessing = sending === w.workflowId

            return (
              <div key={w.workflowId} className="bg-gray-900 border border-gray-800 rounded-lg p-6">
                <div className="flex items-start justify-between">
                  <div className="space-y-2">
                    <div className="flex items-center gap-3">
                      <span className="font-mono text-indigo-400 font-semibold">{w.workflowId}</span>
                      <span className={`px-2 py-0.5 rounded-full text-xs font-medium border ${
                        info.risk === 'CRITICAL' ? 'bg-red-900/50 text-red-400 border-red-800' :
                        info.risk === 'HIGH' ? 'bg-orange-900/50 text-orange-400 border-orange-800' :
                        'bg-yellow-900/50 text-yellow-400 border-yellow-800'
                      }`}>
                        {info.risk}
                      </span>
                    </div>
                    <div className="text-sm text-gray-300">
                      <span className="font-mono">${info.amount.toLocaleString('en-US', { minimumFractionDigits: 2 })}</span>
                      <span className="text-gray-600 mx-2">|</span>
                      <span>{info.route}</span>
                    </div>
                    <div className="text-sm text-gray-500">{info.reason}</div>
                  </div>

                  <div className="flex gap-2">
                    {result ? (
                      <span className={`px-4 py-2 rounded text-sm font-medium ${
                        result.startsWith('Approved') ? 'bg-green-900/50 text-green-400' :
                        result.startsWith('Rejected') ? 'bg-red-900/50 text-red-400' :
                        'bg-gray-800 text-gray-400'
                      }`}>
                        {result}
                      </span>
                    ) : (
                      <>
                        <button
                          onClick={() => sendSignal(w.workflowId, true)}
                          disabled={isProcessing}
                          className="px-4 py-2 bg-green-700 hover:bg-green-600 disabled:opacity-50 rounded text-sm font-medium transition"
                        >
                          {isProcessing ? 'Sending...' : 'Approve'}
                        </button>
                        <button
                          onClick={() => sendSignal(w.workflowId, false)}
                          disabled={isProcessing}
                          className="px-4 py-2 bg-red-700 hover:bg-red-600 disabled:opacity-50 rounded text-sm font-medium transition"
                        >
                          Reject
                        </button>
                      </>
                    )}
                  </div>
                </div>
              </div>
            )
          })}
        </div>
      )}

      <div className="bg-gray-900/50 border border-gray-800 rounded-lg p-4 text-sm text-gray-500">
        <p className="font-medium text-gray-400 mb-1">How it works</p>
        <p>Clicking Approve/Reject sends a Temporal Signal to the running payment workflow. This is the same as running:</p>
        <code className="block mt-2 text-xs text-indigo-400 bg-gray-900 p-2 rounded">
          temporal workflow signal --workflow-id payment-TXN-002 --name approveTransaction --input &#39;&#123;&quot;approved&quot;:true,...&#125;&#39;
        </code>
      </div>
    </div>
  )
}
