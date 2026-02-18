import { NextResponse } from 'next/server'
import { readFile, writeFile } from 'fs/promises'
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

async function readState(): Promise<StepUpdate[]> {
  try {
    const data = await readFile(STATE_FILE, 'utf-8')
    return JSON.parse(data)
  } catch {
    return []
  }
}

async function writeState(state: StepUpdate[]): Promise<void> {
  await writeFile(STATE_FILE, JSON.stringify(state, null, 2))
}

export async function POST(request: Request) {
  try {
    const body = await request.json()

    // Handle reset action
    if (body.action === 'reset') {
      await writeState([])
      return NextResponse.json({ ok: true, action: 'reset' })
    }

    // Validate required fields
    const { transactionId, step, stepName, status, detail, timestamp } = body
    if (!transactionId || step === undefined || !stepName || !status) {
      return NextResponse.json({ error: 'Missing required fields' }, { status: 400 })
    }

    const state = await readState()
    state.push({
      transactionId,
      step,
      stepName,
      status,
      detail: detail || '',
      timestamp: timestamp || new Date().toISOString(),
    })
    await writeState(state)

    return NextResponse.json({ ok: true })
  } catch (e: any) {
    return NextResponse.json({ error: e.message }, { status: 500 })
  }
}
