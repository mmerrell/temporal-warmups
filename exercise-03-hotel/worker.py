import asyncio
import sys
from workflow import HotelReservationWorkflow

from temporalio.worker import Worker
from temporalio.client import Client

from activities import (
    check_room_availability,
    collect_payment,
    assign_room,
    send_email_notification,
    send_sms_notification,
    front_desk_confirmation,
)

async def main():
    try:
        client = await Client.connect("localhost:7233")
        worker = Worker(
            client,
            workflows=[HotelReservationWorkflow],
            task_queue="reservation-queue",
            activities=[
                check_room_availability,
                collect_payment,
                assign_room,
                send_email_notification,
                send_sms_notification,
                front_desk_confirmation,
            ],
        )
        await worker.run()

    except Exception as err:
        print(f"❌ ERROR: {err}", file=sys.stderr, flush=True)
        raise

if __name__ == "__main__":
    asyncio.run(main())