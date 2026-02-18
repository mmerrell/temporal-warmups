import { NextResponse } from 'next/server'
import { readFile } from 'fs/promises'
import path from 'path'

export const dynamic = 'force-dynamic'

const STATE_FILE = path.join(process.cwd(), 'processing-state.json')

interface StepUpdate {
  transactionId: string
  step: number
  stepName: string
  status: string
  detail: string
  timestamp: string
}

interface WorkflowSummary {
  workflowId: string
  status: string
  startTime: string
  closeTime: string | null
}

interface StepInfo {
  name: string
  status: 'pending' | 'in_progress' | 'completed' | 'failed' | 'waiting' | 'skipped'
  startTime: string | null
  endTime: string | null
  detail: string | null
  team: 'payments' | 'compliance'
}

const STEP_DEFAULTS: StepInfo[] = [
  { name: 'Validate Payment', status: 'pending', startTime: null, endTime: null, detail: null, team: 'payments' },
  { name: 'Fraud Screening', status: 'pending', startTime: null, endTime: null, detail: null, team: 'compliance' },
  { name: 'Categorize Transaction', status: 'pending', startTime: null, endTime: null, detail: null, team: 'compliance' },
  { name: 'Approval Wait', status: 'pending', startTime: null, endTime: null, detail: null, team: 'payments' },
  { name: 'Execute Payment', status: 'pending', startTime: null, endTime: null, detail: null, team: 'payments' },
]

async function readState(): Promise<StepUpdate[]> {
  try {
    const data = await readFile(STATE_FILE, 'utf-8')
    return JSON.parse(data)
  } catch {
    return []
  }
}

function buildTransactionSteps(updates: StepUpdate[]): StepInfo[] {
  const steps = STEP_DEFAULTS.map(s => ({ ...s }))

  for (const update of updates) {
    // Map step number (1-5) to array index (0-4)
    const idx = update.step - 1
    if (idx < 0 || idx >= 5) continue

    const step = steps[idx]
    step.detail = update.detail || step.detail

    if (update.status === 'in_progress') {
      step.status = 'in_progress'
      if (!step.startTime) step.startTime = update.timestamp
    } else if (update.status === 'completed') {
      step.status = 'completed'
      step.endTime = update.timestamp
      if (!step.startTime) step.startTime = update.timestamp
    } else if (update.status === 'failed') {
      step.status = 'failed'
      step.endTime = update.timestamp
      if (!step.startTime) step.startTime = update.timestamp
    } else if (update.status === 'skipped') {
      step.status = 'completed'
      step.endTime = update.timestamp
      if (!step.startTime) step.startTime = update.timestamp
    }
  }

  return steps
}

function getOverallStatus(steps: StepInfo[]): string {
  if (steps.some(s => s.status === 'failed')) return 'Failed'
  if (steps.every(s => s.status === 'completed')) return 'Completed'
  if (steps.some(s => s.status === 'in_progress')) return 'Running'
  return 'Running'
}

export async function GET(request: Request) {
  try {
    const { searchParams } = new URL(request.url)
    const id = searchParams.get('id')

    const allUpdates = await readState()

    // Group updates by transaction ID
    const grouped: Record<string, StepUpdate[]> = {}
    for (const u of allUpdates) {
      if (!grouped[u.transactionId]) grouped[u.transactionId] = []
      grouped[u.transactionId].push(u)
    }

    // If requesting a specific transaction's detail
    if (id) {
      // id comes as "payment-TXN-001", extract "TXN-001"
      const txnId = id.replace('payment-', '')
      const updates = grouped[txnId] || []
      const steps = buildTransactionSteps(updates)

      // Check for global failure (step=0) updates
      const globalFailure = updates.find(u => u.step === 0 && u.status === 'failed')
      if (globalFailure) {
        // Mark any pending/in_progress steps as failed
        for (const step of steps) {
          if (step.status === 'pending' || step.status === 'in_progress') {
            step.status = 'failed'
            step.detail = globalFailure.detail
            step.endTime = globalFailure.timestamp
          }
        }
      }

      return NextResponse.json({ steps, events: [] })
    }

    // Return workflow list for main page
    const workflows: WorkflowSummary[] = Object.entries(grouped).map(([txnId, updates]) => {
      const steps = buildTransactionSteps(updates)
      const firstTimestamp = updates[0]?.timestamp || new Date().toISOString()
      const lastTimestamp = updates[updates.length - 1]?.timestamp || null
      const status = getOverallStatus(steps)

      return {
        workflowId: `payment-${txnId}`,
        status,
        startTime: firstTimestamp,
        closeTime: status === 'Completed' || status === 'Failed' ? lastTimestamp : null,
      }
    })

    return NextResponse.json({ workflows })
  } catch (e: any) {
    return NextResponse.json({ error: e.message }, { status: 500 })
  }
}
