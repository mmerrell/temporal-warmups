import asyncio
import uuid
from workflow import BookingWorkflow
from database import TravelBookingRequest

from temporalio.client import Client

async def main():
    # Test bookings
    bookings = [
        TravelBookingRequest(
            confirmation_code = str(uuid.uuid4()),
            customer_name="Alice Johnson",
            customer_email="alice@example.com",
            departure_city="San Francisco",
            destination_city="New York",
            departure_date="2025-01-15",
            return_date="2025-01-20",
            num_passengers=2,
            carriage_class="Economy Plus"
        ),
        TravelBookingRequest(
            confirmation_code = str(uuid.uuid4()),
            customer_name="Bob Smith",
            customer_email="bob@example.com",
            departure_city="Los Angeles",
            destination_city="Miami",
            departure_date="2025-02-01",
            return_date="2025-02-07",
            num_passengers=1,
            carriage_class="First"
        ),
        TravelBookingRequest(
            confirmation_code = str(uuid.uuid4()),
            customer_name="Charlie Brown",
            customer_email="charlie@example.com",
            departure_city="Seattle",
            destination_city="Boston",
            departure_date="2025-03-10",
            return_date="2025-03-15",
            num_passengers=3,
            carriage_class="Coach"
        ),
    ]

    client = await Client.connect("localhost:7233")

    for booking in bookings:
        await client.execute_workflow(
            BookingWorkflow.run,
            args=[booking],
            id=f"booking-{uuid.uuid4()}",
            task_queue="booking-queue",
        )

if __name__ == "__main__":
    asyncio.run(main())