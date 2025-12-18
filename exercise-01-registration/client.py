import asyncio
import uuid

from temporalio.client import Client

from workflow import User, RegistrationWorkflow

async def main():
    client = await Client.connect("localhost:7233")
    # Try registering a few users
    users = [
        User(0,"alice@example.com","alice","secure123"),
        User(0,"bob@example.com","bob","password456"),
        User(0,"alice2@example.com","alice","another_password")
    ]

    for user in users:
        workflow_id = f"registration-{user.email}-{uuid.uuid4()}"
        handle = await client.start_workflow(
            RegistrationWorkflow.run,
            args=[user],
            id=workflow_id,
            task_queue="user-registration-tasks"
        )

        print(f"Started workflow: {workflow_id}")
        result = await handle.result()
        print(f"✓ Registration result for {user.username}: {result}")

# Usage example
if __name__ == "__main__":
    asyncio.run(main())