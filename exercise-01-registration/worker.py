import asyncio
import sys

from temporalio.client import Client
from temporalio.worker import Worker
from activities import send_welcome_email, send_verification_email, create_user_record

from workflow import RegistrationWorkflow


async def main():
    client = await Client.connect("localhost:7233")

    try:
        worker = Worker(
            client,
            task_queue="user-registration-tasks",
            workflows=[RegistrationWorkflow],
            activities=[send_welcome_email,send_verification_email,create_user_record],
        )
    except Exception as err:
        print(f"❌ ERROR: {err}", file=sys.stderr, flush=True)
        raise

    await worker.run()

if __name__ == "__main__":
    asyncio.run(main())