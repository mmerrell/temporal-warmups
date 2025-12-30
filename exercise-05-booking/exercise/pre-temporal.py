# pre-temporal.py - Travel booking WITHOUT compensations
# This demonstrates the problem that SAGA pattern solves!

import random
import time
from datetime import datetime
from models import (
    TravelBooking,
    FlightReservation,
    HotelReservation,
    CarReservation,
    Payment,
)

# "Database" - just in-memory dictionaries
flight_reservations = {}
hotel_reservations = {}
car_reservations = {}
payments = {}


def book_flight(booking: TravelBooking, reservation_id: str) -> FlightReservation:
    """
    Book a flight for the customer

    Simulates occasional failures (10% of the time)
    """
    print(f"\n📍 Booking flight for {booking.customer_name}...")
    print(f"   Route: {booking.departure_city} → {booking.destination_city}")
    print(f"   Date: {booking.departure_date}")

    # Simulate processing time
    time.sleep(0.5)

    # Simulate occasional failures
    if random.random() < 0.10:
        raise Exception("❌ Flight booking service unavailable")

    # Calculate price
    base_price = 350.00
    price = base_price * booking.num_passengers

    # Create reservation
    flight_number = f"AA{random.randint(100, 999)}"
    reservation = FlightReservation(
        reservation_id=reservation_id,
        customer_name=booking.customer_name,
        flight_number=flight_number,
        departure_city=booking.departure_city,
        destination_city=booking.destination_city,
        date=booking.departure_date,
        price=price,
    )

    # Store it
    flight_reservations[reservation_id] = reservation

    print(f"   ✅ Flight {flight_number} booked (${price:.2f})")
    return reservation


def book_hotel(booking: TravelBooking, reservation_id: str) -> HotelReservation:
    """
    Book a hotel for the customer

    Simulates occasional failures (20% of the time)
    """
    print(f"\n📍 Booking hotel in {booking.destination_city}...")

    # Simulate processing time
    time.sleep(0.5)

    # Simulate occasional failures
    if random.random() < 0.20:
        raise Exception("❌ Hotel booking service unavailable")

    # Calculate price (per night)
    from datetime import datetime
    check_in = datetime.fromisoformat(booking.departure_date)
    check_out = datetime.fromisoformat(booking.return_date)
    nights = (check_out - check_in).days

    rate_per_night = 150.00
    price = rate_per_night * nights

    # Create reservation
    hotel_name = "Grand Plaza Hotel"
    reservation = HotelReservation(
        reservation_id=reservation_id,
        customer_name=booking.customer_name,
        hotel_name=hotel_name,
        city=booking.destination_city,
        check_in=booking.departure_date,
        check_out=booking.return_date,
        price=price,
    )

    # Store it
    hotel_reservations[reservation_id] = reservation

    print(f"   ✅ Hotel booked: {hotel_name} ({nights} nights, ${price:.2f})")
    return reservation


def book_car(booking: TravelBooking, reservation_id: str) -> CarReservation:
    """
    Book a rental car for the customer

    Simulates FREQUENT failures (40% of the time) - this is where things often break!
    """
    print(f"\n📍 Booking rental car in {booking.destination_city}...")

    # Simulate processing time
    time.sleep(0.5)

    # Simulate FREQUENT failures - this is the problem step!
    if random.random() < 0.40:
        raise Exception("❌ Car rental service unavailable")

    # Calculate price
    from datetime import datetime
    pickup = datetime.fromisoformat(booking.departure_date)
    return_date = datetime.fromisoformat(booking.return_date)
    days = (return_date - pickup).days

    rate_per_day = 50.00
    price = rate_per_day * days

    # Create reservation
    car_type = "Economy"
    reservation = CarReservation(
        reservation_id=reservation_id,
        customer_name=booking.customer_name,
        car_type=car_type,
        city=booking.destination_city,
        pickup_date=booking.departure_date,
        return_date=booking.return_date,
        price=price,
    )

    # Store it
    car_reservations[reservation_id] = reservation

    print(f"   ✅ Car rental booked: {car_type} ({days} days, ${price:.2f})")
    return reservation


def process_payment(reservation_id: str, total_amount: float) -> Payment:
    """
    Process payment for the booking

    Simulates occasional failures (15% of the time)
    """
    print(f"\n📍 Processing payment of ${total_amount:.2f}...")

    # Simulate processing time
    time.sleep(0.5)

    # Simulate occasional failures
    if random.random() < 0.15:
        raise Exception("❌ Payment processing failed")

    # Create payment record
    payment_id = f"PAY-{random.randint(1000, 9999)}"
    payment = Payment(
        payment_id=payment_id,
        reservation_id=reservation_id,
        amount=total_amount,
    )

    # Store it
    payments[reservation_id] = payment

    print(f"   ✅ Payment processed: {payment_id}")
    return payment


def send_confirmation_email(booking: TravelBooking, reservation_id: str):
    """
    Send confirmation email to customer

    This can fail, but we don't care - booking is already complete!
    """
    print(f"\n📍 Sending confirmation email to {booking.customer_email}...")

    # Simulate processing time
    time.sleep(0.3)

    # Email can fail 10% of the time - but this is OK!
    if random.random() < 0.10:
        print(f"   ⚠️  Email failed (but booking is still valid)")
        return

    print(f"   ✅ Email sent")


# ========================================
# THE PROBLEM: NO COMPENSATIONS!
# ========================================

def book_travel_package(booking: TravelBooking) -> str:
    """
    Book a complete travel package: flight + hotel + car

    THE PROBLEM: When something fails partway through,
    we leave orphaned reservations in the system!

    This is what SAGA pattern / compensations solve.
    """
    reservation_id = f"RES-{int(time.time())}-{random.randint(100, 999)}"

    print("\n" + "=" * 70)
    print(f"BOOKING TRAVEL PACKAGE")
    print(f"Customer: {booking.customer_name}")
    print(f"Reservation ID: {reservation_id}")
    print("=" * 70)

    flight_result = None
    hotel_result = None
    car_result = None
    payment_result = None

    try:
        # Step 1: Book flight
        flight_result = book_flight(booking, reservation_id)

        # Step 2: Book hotel
        hotel_result = book_hotel(booking, reservation_id)

        # Step 3: Book car (this often fails!)
        car_result = book_car(booking, reservation_id)

        # Step 4: Process payment
        total = flight_result.price + hotel_result.price + car_result.price
        payment_result = process_payment(reservation_id, total)

        # Step 5: Send email (can fail, but we don't care)
        send_confirmation_email(booking, reservation_id)

        # Success!
        print("\n" + "=" * 70)
        print("✅ TRAVEL PACKAGE BOOKED SUCCESSFULLY!")
        print(f"Reservation ID: {reservation_id}")
        print(f"Total: ${total:.2f}")
        print("=" * 70 + "\n")

        return reservation_id

    except Exception as e:
        # ========================================
        # THIS IS THE PROBLEM!!!
        # ========================================
        # When booking fails, we just log errors
        # but we DON'T cancel the reservations!
        #
        # This leaves "orphaned" reservations:
        # - Customer gets charged for flight they can't use
        # - Hotel holds the room (charges no-show fee)
        # - System is left in inconsistent state
        # ========================================

        print("\n" + "=" * 70)
        print(f"❌ BOOKING FAILED: {e}")
        print("=" * 70)

        # Just log what succeeded (but don't cancel anything!)
        if flight_result:
            print(f"⚠️  WARNING: Flight {flight_result.flight_number} was booked but NOT cancelled!")
            print("   Customer will be charged for unused flight!")

        if hotel_result:
            print(f"⚠️  WARNING: Hotel {hotel_result.hotel_name} was booked but NOT cancelled!")
            print("   Hotel will charge no-show fee!")

        if car_result:
            print(f"⚠️  WARNING: Car rental was booked but NOT cancelled!")

        print("\n💡 This is what SAGA pattern / compensations solve!")
        print("   We need to CANCEL these reservations when booking fails!")
        print("=" * 70 + "\n")

        raise


def print_system_state():
    """Show the current state of all reservations"""
    print("\n" + "=" * 70)
    print("SYSTEM STATE (showing the problem)")
    print("=" * 70)
    print(f"Flight Reservations: {len(flight_reservations)}")
    print(f"Hotel Reservations: {len(hotel_reservations)}")
    print(f"Car Reservations: {len(car_reservations)}")
    print(f"Payments Processed: {len(payments)}")
    print("\n⚠️  If these numbers don't match, we have orphaned reservations!")
    print("This is what proper compensations prevent.")
    print("=" * 70 + "\n")


# ========================================
# DEMO: Run this to see the problem!
# ========================================

if __name__ == "__main__":
    print("🎯 DEMONSTRATING THE SAGA PROBLEM")
    print("(Run this a few times - car booking fails 40% of the time)\n")

    bookings = [
        TravelBooking(
            customer_name="Alice Johnson",
            customer_email="alice@example.com",
            departure_city="San Francisco",
            destination_city="New York",
            departure_date="2025-01-15",
            return_date="2025-01-20",
            num_passengers=2,
        ),
        TravelBooking(
            customer_name="Bob Smith",
            customer_email="bob@example.com",
            departure_city="Los Angeles",
            destination_city="Miami",
            departure_date="2025-02-01",
            return_date="2025-02-07",
            num_passengers=1,
        ),
        TravelBooking(
            customer_name="Charlie Brown",
            customer_email="charlie@example.com",
            departure_city="Seattle",
            destination_city="Boston",
            departure_date="2025-03-10",
            return_date="2025-03-15",
            num_passengers=3,
        ),
    ]

    # Try to book each travel package
    for booking in bookings:
        try:
            book_travel_package(booking)
        except Exception:
            pass  # Already logged in the function

        time.sleep(1)  # Pause between bookings

    # Show the problem!
    print_system_state()

    print("\n" + "=" * 70)
    print("THE PROBLEM:")
    print("When bookings fail partway through, we have:")
    print("• Orphaned flight reservations (customer charged but no trip)")
    print("• Orphaned hotel reservations (hotel charges no-show fee)")
    print("• Orphaned car reservations (rental company holds the car)")
    print("\nTHE SOLUTION:")
    print("Implement SAGA pattern with proper compensating actions:")
    print("• cancel_flight()")
    print("• cancel_hotel()")
    print("• cancel_car()")
    print("• refund_payment()")
    print("=" * 70 + "\n")