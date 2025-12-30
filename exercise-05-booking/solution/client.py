import asyncio
import uuid
from datetime import datetime, timedelta
from workflow import BookingWorkflow
from database import TravelBookingRequest

from temporalio.client import Client


async def main():
    # Database is seeded from 2025-01-20 for 60 days (through ~2025-03-20)
    base_date = datetime(2025, 1, 20)

    # Test bookings - 50 total with mix of valid and intentionally failing
    bookings = []

    # ==========================================
    # VALID BOOKINGS (30 total)
    # These should succeed OR fail randomly due to simulated service issues
    # ==========================================

    valid_routes = [
        ("San Francisco", "New York"),
        ("Los Angeles", "Miami"),
        ("Seattle", "Atlanta"),
        ("Boston", "Los Angeles"),
        ("Chicago", "Denver"),
        ("San Francisco", "Chicago"),
        ("Dallas", "Las Vegas"),
        ("Phoenix", "Seattle"),
        ("New York", "San Francisco"),
        ("Atlanta", "Miami"),
        ("Denver", "Boston"),
        ("Las Vegas", "Dallas"),
    ]

    carriage_classes = ["economy", "business", "first"]

    # Generate 30 valid bookings spread across the date range
    for i in range(30):
        route = valid_routes[i % len(valid_routes)]
        carriage = carriage_classes[i % len(carriage_classes)]

        # Spread bookings across the 60-day range
        days_offset = (i * 2) % 55  # Keep within range with some buffer

        bookings.append(TravelBookingRequest(
            confirmation_code=str(uuid.uuid4()),
            customer_name=f"Customer-{i + 1:03d}",
            customer_email=f"customer{i + 1}@example.com",
            departure_city=route[0],
            destination_city=route[1],
            departure_date=(base_date + timedelta(days=days_offset)).strftime('%Y-%m-%d'),
            return_date=(base_date + timedelta(days=days_offset + 4 + (i % 3))).strftime('%Y-%m-%d'),
            num_passengers=(i % 3) + 1,
            carriage_class=carriage
        ))

    # ==========================================
    # INTENTIONAL FAILURES (13 total)
    # These test different failure points in the saga
    # ==========================================

    # ========================================
    # FLIGHT FAILURES (3 total)
    # These fail at step 1 - nothing to compensate
    # ========================================

    # Flight failure #1: Date before seeded range
    bookings.append(TravelBookingRequest(
        confirmation_code=str(uuid.uuid4()),
        customer_name="EarlyDate-Fail",
        customer_email="early@example.com",
        departure_city="San Francisco",
        destination_city="New York",
        departure_date=(base_date - timedelta(days=5)).strftime('%Y-%m-%d'),  # Before range!
        return_date=(base_date - timedelta(days=2)).strftime('%Y-%m-%d'),
        num_passengers=1,
        carriage_class="economy"
    ))

    # Flight failure #2: Date after seeded range
    bookings.append(TravelBookingRequest(
        confirmation_code=str(uuid.uuid4()),
        customer_name="LateDate-Fail",
        customer_email="late@example.com",
        departure_city="Los Angeles",
        destination_city="Miami",
        departure_date=(base_date + timedelta(days=65)).strftime('%Y-%m-%d'),  # After range!
        return_date=(base_date + timedelta(days=70)).strftime('%Y-%m-%d'),
        num_passengers=1,
        carriage_class="business"
    ))

    # Flight failure #3: Invalid route (no direct flight)
    bookings.append(TravelBookingRequest(
        confirmation_code=str(uuid.uuid4()),
        customer_name="BadRoute-Fail",
        customer_email="badroute@example.com",
        departure_city="San Francisco",
        destination_city="Miami",  # No direct SFO->MIA flight
        departure_date=(base_date + timedelta(days=10)).strftime('%Y-%m-%d'),
        return_date=(base_date + timedelta(days=15)).strftime('%Y-%m-%d'),
        num_passengers=1,
        carriage_class="economy"
    ))

    # ========================================
    # HOTEL FAILURES (5 total)
    # Flight succeeds → Hotel fails → Compensate: cancel_flight
    # ========================================

    invalid_room_types = [
        ("Presidential-Suite", "presidential"),  # Doesn't exist (only standard/deluxe/suite)
        ("Penthouse-Fail", "penthouse"),  # Doesn't exist
        ("Oceanview-Fail", "oceanview"),  # Doesn't exist
        ("ExecutiveSuite-Fail", "executive"),  # Doesn't exist
        ("DeluxeKing-Fail", "deluxe-king"),  # Close but wrong format
    ]

    for i, (name, room_type) in enumerate(invalid_room_types):
        bookings.append(TravelBookingRequest(
            confirmation_code=str(uuid.uuid4()),
            customer_name=name,
            customer_email=f"hotel-fail-{i + 1}@example.com",
            departure_city="San Francisco",
            destination_city="New York",
            departure_date=(base_date + timedelta(days=5 + i)).strftime('%Y-%m-%d'),
            return_date=(base_date + timedelta(days=10 + i)).strftime('%Y-%m-%d'),
            num_passengers=1,
            carriage_class="economy",
            room_type=room_type
        ))

    # Note: These will fail in workflow.py when it passes the invalid room_type
    # We need to update workflow.py to use a variable room_type based on the booking

    # ========================================
    # CAR FAILURES (5 total)
    # Flight+Hotel succeed → Car fails → Compensate: cancel_hotel, cancel_flight
    # ========================================

    invalid_car_types = [
        ("Limousine-Fail", "limousine"),  # Doesn't exist (only economy/compact/midsize/suv/luxury)
        ("SportsCar-Fail", "sports-car"),  # Doesn't exist
        ("Convertible-Fail", "convertible"),  # Doesn't exist
        ("LuxurySUV-Fail", "luxury-suv"),  # Doesn't exist
        ("Electric-Fail", "electric"),  # Doesn't exist
    ]

    for i, (name, car_type) in enumerate(invalid_car_types):
        bookings.append(TravelBookingRequest(
            confirmation_code=str(uuid.uuid4()),
            customer_name=name,
            customer_email=f"car-fail-{i + 1}@example.com",
            departure_city="Los Angeles",
            destination_city="Miami",
            departure_date=(base_date + timedelta(days=15 + i)).strftime('%Y-%m-%d'),
            return_date=(base_date + timedelta(days=20 + i)).strftime('%Y-%m-%d'),
            num_passengers=1,
            carriage_class="economy",
            car_type = car_type
        ))

    # ========================================
    # PAYMENT FAILURE TEST (1 booking)
    # Flight+Hotel+Car succeed → Payment fails → Full compensation chain
    # ========================================
    bookings.append(TravelBookingRequest(
        confirmation_code="FORCE-PAYMENT-FAIL-" + str(uuid.uuid4())[:8],
        customer_name="PaymentFail-Test",
        customer_email="payment-fail@example.com",
        departure_city="San Francisco",
        destination_city="New York",
        departure_date=(base_date + timedelta(days=30)).strftime('%Y-%m-%d'),
        return_date=(base_date + timedelta(days=35)).strftime('%Y-%m-%d'),
        num_passengers=1,
        carriage_class="economy",
        room_type="standard",
        car_type="economy"
    ))

    # Note: These will fail in workflow.py when it passes the invalid car_type
    # We need to update workflow.py to use a variable car_type based on the booking

    # ==========================================
    # RUN THE BOOKINGS
    # ==========================================

    client = await Client.connect("localhost:7233")

    print(f"\n{'=' * 70}")
    print(f"HIGH-VOLUME BOOKING TEST")
    print(f"{'=' * 70}")
    print(f"Total bookings: {len(bookings)}")
    print(f"  - Valid bookings: 30 (will succeed OR fail randomly)")
    print(f"  - Flight failures: 3 (early date, late date, bad route)")
    print(f"  - Hotel failures: 5 (invalid room types → compensate flight)")
    print(f"  - Car failures: 5 (invalid car types → compensate hotel+flight)")
    print(f"\nExpected random failures on valid bookings:")
    print(f"  - Hotel service: ~25% failure rate")
    print(f"  - Car rental: ~40% failure rate")
    print(f"  - Payment: ~10% failure rate")
    print(f"\nSeeded date range: 2025-01-20 to ~2025-03-20")
    print(f"{'=' * 70}\n")

    success_count = 0
    failure_count = 0

    for i, booking in enumerate(bookings, 1):
        workflow_id = f"booking-{booking.customer_name.replace(' ', '-')}-{booking.confirmation_code[:8]}"
        print(
            f"[{i:2d}/{len(bookings)}] {booking.customer_name:20s} | {booking.departure_city:15s} → {booking.destination_city:15s} | {booking.departure_date}",
            end=" ")

        try:
            await client.execute_workflow(
                BookingWorkflow.run,
                args=[booking],
                id=workflow_id,
                task_queue="booking-queue",
            )
            print(" ✅ SUCCESS")
            success_count += 1
        except Exception as e:
            print(f" ❌ FAILED")
            failure_count += 1

        # Small pause for readability
        if i % 10 == 0:
            await asyncio.sleep(0.5)

    print(f"\n{'=' * 70}")
    print(f"RESULTS:")
    print(f"  ✅ Successful bookings: {success_count}")
    print(f"  ❌ Failed bookings: {failure_count}")
    print(f"\n💡 Check Temporal UI to see compensation workflows!")
    print(f"{'=' * 70}\n")


if __name__ == "__main__":
    asyncio.run(main())