# Usage example
import asyncio
import uuid
from database import ReservationRequest
from temporalio.client import Client
from workflow import HotelReservationWorkflow

async def main():
    # Test reservations
    reservations = [
        ReservationRequest(
            guest_name = "Alice Johnson",
            guest_email = "alice@example.com",
            guest_mobile = "415-555-1212",
            room_type = 'deluxe',
            check_in = "2024-12-20",
            check_out = "2024-12-23",
            payment_method = "credit_card",
        ),
        ReservationRequest(
            guest_name = 'Bob Smith',
            guest_email = 'bob@example.com',
            guest_mobile = "512-555-1212",
            room_type = 'deluxe',
            check_in = "2025-12-20",
            check_out = "2025-12-23",
            payment_method = 'credit_card',
        ),
        ReservationRequest(
            guest_name = 'Charlie Brown',
            guest_email ='charlie@example.com',
            guest_mobile = "212-555-1212",
            room_type = 'deluxe',
            check_in = "2025-07-12",
            check_out = "2025-07-24",
            payment_method = 'debit_card',
        ),
    ]

    client = await Client.connect("localhost:7233")

    for reservation in reservations:
        await client.execute_workflow(
            HotelReservationWorkflow.run,
            args=[reservation],
            id = f"RES-{uuid.uuid4()}",
            task_queue="reservation-queue",
        )
        await asyncio.sleep(2)

if __name__ == "__main__":
    asyncio.run(main())