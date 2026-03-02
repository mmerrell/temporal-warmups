import { NextRequest, NextResponse } from 'next/server'
import { execSync } from 'child_process'

export async function POST(request: NextRequest) {
  try {
    const body = await request.json()
    const { workflowId, signalName, input } = body

    if (!workflowId || !signalName) {
      return NextResponse.json({ error: 'Missing workflowId or signalName' }, { status: 400 })
    }

    // Use Temporal CLI to send the signal
    const inputJson = JSON.stringify(input)
    const cmd = `temporal workflow signal --workflow-id "${workflowId}" --name "${signalName}" --input '${inputJson}'`

    execSync(cmd, { timeout: 10000 })

    return NextResponse.json({ success: true, workflowId, signalName })
  } catch (e: any) {
    return NextResponse.json(
      { error: 'Failed to send signal', detail: e.message },
      { status: 500 }
    )
  }
}
