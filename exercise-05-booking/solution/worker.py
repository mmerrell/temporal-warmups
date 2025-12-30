import asyncio

from temporalio.worker import Worker
from temporalio.client import Client
from workflow import BookingWorkflow
from activities import (
    store_booking_request,
    book_flight,
    book_car,
    book_hotel,
    accept_payment,
    cancel_flight,
    cancel_hotel,
    cancel_car
)

async def main():
    client = await Client.connect("localhost:7233")

    worker = Worker(
        client,
        workflows=[BookingWorkflow],
        task_queue="booking-queue",
        activities=[store_booking_request,book_flight,book_car,book_hotel,accept_payment,cancel_flight,cancel_car,cancel_hotel],
    )

    await worker.run()

if __name__ == "__main__":
    asyncio.run(main())
