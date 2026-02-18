import { NextResponse } from 'next/server'

export const dynamic = 'force-dynamic'

export async function GET() {
  try {
    // Query Temporal's HTTP API for workflow list
    const res = await fetch('http://localhost:8233/api/v1/namespaces/default/workflows?query=', {
      headers: { 'Accept': 'application/json' },
    })

    if (!res.ok) {
      return NextResponse.json({ error: 'Failed to fetch from Temporal' }, { status: 502 })
    }

    const data = await res.json()

    const workflows = (data.executions || []).map((exec: any) => ({
      workflowId: exec.execution?.workflowId || '',
      runId: exec.execution?.runId || '',
      status: mapStatus(exec.status),
      startTime: exec.startTime || '',
      closeTime: exec.closeTime || null,
      taskQueue: exec.taskQueue || '',
    }))

    return NextResponse.json({ workflows })
  } catch (e: any) {
    return NextResponse.json(
      { error: 'Cannot connect to Temporal at localhost:8233', detail: e.message },
      { status: 503 }
    )
  }
}

function mapStatus(status: string): string {
  const map: Record<string, string> = {
    'WORKFLOW_EXECUTION_STATUS_RUNNING': 'Running',
    'WORKFLOW_EXECUTION_STATUS_COMPLETED': 'Completed',
    'WORKFLOW_EXECUTION_STATUS_FAILED': 'Failed',
    'WORKFLOW_EXECUTION_STATUS_CANCELED': 'Canceled',
    'WORKFLOW_EXECUTION_STATUS_TERMINATED': 'Terminated',
    'WORKFLOW_EXECUTION_STATUS_CONTINUED_AS_NEW': 'ContinuedAsNew',
    'WORKFLOW_EXECUTION_STATUS_TIMED_OUT': 'TimedOut',
  }
  return map[status] || status || 'Unknown'
}
