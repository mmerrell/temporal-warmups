import asyncio
import sys
from temporalio.worker import Worker
from workflow import EmailVerificationWorkflow

from temporalio.client import Client
from activities import generate_token,send_verification_email

async def main():
    try:
        client = await Client.connect("localhost:7233")
        worker = Worker(
            client,
            workflows=[EmailVerificationWorkflow],
            task_queue="email-verification-tasks",
            activities=[generate_token,send_verification_email]
        )

        await worker.run()

    except Exception as err:
        print(f"❌ ERROR: {err}", file=sys.stderr, flush=True)
        raise

if __name__ == "__main__":
    asyncio.run(main())