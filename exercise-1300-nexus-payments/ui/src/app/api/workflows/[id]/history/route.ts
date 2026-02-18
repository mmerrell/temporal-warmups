import { NextResponse } from 'next/server'

export const dynamic = 'force-dynamic'

interface StepInfo {
  name: string
  status: 'pending' | 'in_progress' | 'completed' | 'failed' | 'waiting'
  startTime: string | null
  endTime: string | null
  detail: string | null
  team: 'payments' | 'compliance'
}

interface ParsedEvent {
  eventId: string
  eventType: string
  eventTime: string
  detail: string
}

export async function GET(
  request: Request,
  { params }: { params: { id: string } }
) {
  const workflowId = params.id

  try {
    const res = await fetch(
      `http://localhost:8233/api/v1/namespaces/default/workflows/${encodeURIComponent(workflowId)}/history?maximumPageSize=200`,
      { headers: { Accept: 'application/json' } }
    )

    if (!res.ok) {
      return NextResponse.json(
        { error: `Temporal returned ${res.status}` },
        { status: 502 }
      )
    }

    const data = await res.json()
    const rawEvents = data.history?.events || []

    const events: ParsedEvent[] = rawEvents.map((e: any) => ({
      eventId: e.eventId || '',
      eventType: e.eventType || '',
      eventTime: e.eventTime || '',
      detail: summarizeEvent(e),
    }))

    const steps = parseSteps(rawEvents)

    return NextResponse.json({ steps, events })
  } catch (e: any) {
    return NextResponse.json(
      { error: 'Cannot connect to Temporal', detail: e.message },
      { status: 503 }
    )
  }
}

function summarizeEvent(e: any): string {
  const type = e.eventType || ''

  if (type.includes('ACTIVITY_TASK_SCHEDULED')) {
    const actType = e.activityTaskScheduledEventAttributes?.activityType?.name
    return actType ? `Activity: ${actType}` : 'Activity scheduled'
  }
  if (type.includes('ACTIVITY_TASK_COMPLETED')) {
    return 'Activity completed'
  }
  if (type.includes('ACTIVITY_TASK_FAILED')) {
    const msg = e.activityTaskFailedEventAttributes?.failure?.message
    return msg ? `Failed: ${msg}` : 'Activity failed'
  }
  if (type.includes('NEXUS_OPERATION_SCHEDULED')) {
    const op = e.nexusOperationScheduledEventAttributes?.operation
    const svc = e.nexusOperationScheduledEventAttributes?.service
    return `Nexus: ${svc || ''}/${op || ''}`
  }
  if (type.includes('NEXUS_OPERATION_STARTED')) {
    return 'Nexus operation started (async)'
  }
  if (type.includes('NEXUS_OPERATION_COMPLETED')) {
    return 'Nexus operation completed'
  }
  if (type.includes('NEXUS_OPERATION_FAILED')) {
    const msg = e.nexusOperationFailedEventAttributes?.failure?.message
    return msg ? `Nexus failed: ${msg}` : 'Nexus operation failed'
  }
  if (type.includes('TIMER_STARTED')) {
    return 'Timer started (awaiting signal)'
  }
  if (type.includes('TIMER_FIRED')) {
    return 'Timer fired (timeout)'
  }
  if (type.includes('TIMER_CANCELED')) {
    return 'Timer canceled'
  }
  if (type.includes('WORKFLOW_EXECUTION_SIGNALED')) {
    const name = e.workflowExecutionSignaledEventAttributes?.signalName
    return name ? `Signal: ${name}` : 'Signal received'
  }
  if (type.includes('WORKFLOW_EXECUTION_STARTED')) {
    return 'Workflow started'
  }
  if (type.includes('WORKFLOW_EXECUTION_COMPLETED')) {
    return 'Workflow completed'
  }
  if (type.includes('WORKFLOW_EXECUTION_FAILED')) {
    return 'Workflow failed'
  }
  if (type.includes('WORKFLOW_TASK')) {
    return '' // Internal, skip in display
  }

  return type.replace('EVENT_TYPE_', '').replace(/_/g, ' ').toLowerCase()
}

function parseSteps(events: any[]): StepInfo[] {
  const steps: StepInfo[] = [
    { name: 'Validate Payment', status: 'pending', startTime: null, endTime: null, detail: null, team: 'payments' },
    { name: 'Categorize Transaction', status: 'pending', startTime: null, endTime: null, detail: null, team: 'compliance' },
    { name: 'Fraud Screening', status: 'pending', startTime: null, endTime: null, detail: null, team: 'compliance' },
    { name: 'Approval Wait', status: 'pending', startTime: null, endTime: null, detail: null, team: 'payments' },
    { name: 'Execute Payment', status: 'pending', startTime: null, endTime: null, detail: null, team: 'payments' },
  ]

  // Track activities and nexus operations in order
  let activityCount = 0
  let nexusCount = 0
  let hasTimer = false
  let hasSignal = false
  let timerFired = false

  // Maps to track scheduled -> completed/failed for activities
  const activityScheduled: Map<number, { index: number; time: string; name: string }> = new Map()
  const nexusScheduled: Map<number, { index: number; time: string }> = new Map()

  for (const event of events) {
    const type = event.eventType || ''
    const time = event.eventTime || ''

    // Activity tracking
    if (type.includes('ACTIVITY_TASK_SCHEDULED')) {
      activityCount++
      const scheduledId = parseInt(event.eventId)
      const actName = event.activityTaskScheduledEventAttributes?.activityType?.name || ''

      if (activityCount === 1) {
        // First activity = Validate Payment
        steps[0].status = 'in_progress'
        steps[0].startTime = time
        steps[0].detail = actName
        activityScheduled.set(scheduledId, { index: 0, time, name: actName })
      } else {
        // Later activities = Execute Payment (after signal/timer)
        steps[4].status = 'in_progress'
        steps[4].startTime = time
        steps[4].detail = actName
        activityScheduled.set(scheduledId, { index: 4, time, name: actName })
      }
    }

    if (type.includes('ACTIVITY_TASK_COMPLETED')) {
      const scheduledId = event.activityTaskCompletedEventAttributes?.scheduledEventId
      const entry = findScheduledActivity(activityScheduled, scheduledId)
      if (entry !== undefined) {
        steps[entry].status = 'completed'
        steps[entry].endTime = time
      }
    }

    if (type.includes('ACTIVITY_TASK_FAILED')) {
      const scheduledId = event.activityTaskFailedEventAttributes?.scheduledEventId
      const entry = findScheduledActivity(activityScheduled, scheduledId)
      if (entry !== undefined) {
        steps[entry].status = 'failed'
        steps[entry].endTime = time
        const msg = event.activityTaskFailedEventAttributes?.failure?.message
        if (msg) steps[entry].detail = msg
      }
    }

    // Nexus operation tracking
    if (type.includes('NEXUS_OPERATION_SCHEDULED')) {
      nexusCount++
      const scheduledId = parseInt(event.eventId)
      const op = event.nexusOperationScheduledEventAttributes?.operation || ''

      if (nexusCount === 1) {
        // First nexus = Categorize (sync)
        steps[1].status = 'in_progress'
        steps[1].startTime = time
        steps[1].detail = `Nexus: ${op}`
        nexusScheduled.set(scheduledId, { index: 1, time })
      } else {
        // Second nexus = Fraud Screening (async)
        steps[2].status = 'in_progress'
        steps[2].startTime = time
        steps[2].detail = `Nexus: ${op}`
        nexusScheduled.set(scheduledId, { index: 2, time })
      }
    }

    if (type.includes('NEXUS_OPERATION_STARTED') && !type.includes('SCHEDULED')) {
      // Async operation has started on the handler side
      const scheduledId = event.nexusOperationStartedEventAttributes?.scheduledEventId
      const entry = findScheduledNexus(nexusScheduled, scheduledId)
      if (entry !== undefined) {
        steps[entry].detail = (steps[entry].detail || '') + ' (async started)'
      }
    }

    if (type.includes('NEXUS_OPERATION_COMPLETED')) {
      const scheduledId = event.nexusOperationCompletedEventAttributes?.scheduledEventId
      const entry = findScheduledNexus(nexusScheduled, scheduledId)
      if (entry !== undefined) {
        steps[entry].status = 'completed'
        steps[entry].endTime = time
      }
    }

    if (type.includes('NEXUS_OPERATION_FAILED')) {
      const scheduledId = event.nexusOperationFailedEventAttributes?.scheduledEventId
      const entry = findScheduledNexus(nexusScheduled, scheduledId)
      if (entry !== undefined) {
        steps[entry].status = 'failed'
        steps[entry].endTime = time
      }
    }

    // Timer = Approval Wait
    if (type.includes('TIMER_STARTED')) {
      hasTimer = true
      steps[3].status = 'waiting'
      steps[3].startTime = time
      steps[3].detail = 'Awaiting approval signal'
    }

    if (type.includes('TIMER_FIRED')) {
      timerFired = true
      if (!hasSignal) {
        steps[3].status = 'completed'
        steps[3].endTime = time
        steps[3].detail = 'Timed out (auto-rejected)'
      }
    }

    if (type.includes('TIMER_CANCELED')) {
      // Timer canceled because signal arrived
      steps[3].endTime = time
    }

    // Signal = approval received
    if (type.includes('WORKFLOW_EXECUTION_SIGNALED')) {
      hasSignal = true
      steps[3].status = 'completed'
      steps[3].endTime = time
      const signalName = event.workflowExecutionSignaledEventAttributes?.signalName || ''
      steps[3].detail = `Signal received: ${signalName}`
    }

    // Workflow completion/failure
    if (type.includes('WORKFLOW_EXECUTION_COMPLETED')) {
      // If we never saw a timer, step 4 = approval was skipped (low risk)
      if (!hasTimer) {
        steps[3].status = 'completed'
        steps[3].detail = 'Skipped (low risk)'
      }
    }

    if (type.includes('WORKFLOW_EXECUTION_FAILED')) {
      // Mark remaining in-progress steps as failed
      for (const step of steps) {
        if (step.status === 'in_progress' || step.status === 'waiting') {
          step.status = 'failed'
          step.endTime = time
        }
      }
    }
  }

  return steps
}

function findScheduledActivity(
  map: Map<number, { index: number; time: string; name: string }>,
  scheduledId: any
): number | undefined {
  if (!scheduledId) return undefined
  const id = typeof scheduledId === 'string' ? parseInt(scheduledId) : scheduledId
  return map.get(id)?.index
}

function findScheduledNexus(
  map: Map<number, { index: number; time: string }>,
  scheduledId: any
): number | undefined {
  if (!scheduledId) return undefined
  const id = typeof scheduledId === 'string' ? parseInt(scheduledId) : scheduledId
  return map.get(id)?.index
}
