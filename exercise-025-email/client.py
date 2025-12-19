# Usage
import asyncio
import uuid

from temporalio.client import Client
from workflow import EmailVerificationWorkflow

async def main():
    client = await Client.connect("localhost:7233")

    emails = [
        "alice@example.com",
        "bob@example.com",
        "charlie@example.com"
    ]

    for email in emails:
        result = await client.execute_workflow(
            EmailVerificationWorkflow.run,
            args=[email],
            id=f"email-email_{email}-{uuid.uuid4()}",
            task_queue="email-verification-tasks",
        )
        await asyncio.sleep(1)
        print(f"✓ {email}: {result}")

if __name__ == "__main__":
    asyncio.run(main())