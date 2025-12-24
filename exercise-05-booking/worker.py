import asyncio

from temporalio.worker import Worker
from temporalio.client import Client
from workflow import BookingWorkflow
from activities import (
    book_flight,
    book_car,
    book_hotel,
    accept_payment,
)

async def main():
    client = await Client.connect("http://localhost:7233")

    worker = Worker(
        client,
        workflows=[BookingWorkflow],
        task_queue="booking-queue",
        activities=[book_flight,book_car,book_hotel,accept_payment],
    )

    await worker.run()

if __name__ == "__main__":
    asyncio.run(main())
