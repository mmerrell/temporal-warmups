# travel_booking.py - Original messy implementation
# Multi-step booking process that NEEDS proper compensations

import time
import random
from datetime import datetime
from dataclasses import dataclass
from typing import Dict, List

@dataclass
class TravelBooking:
    """Customer's travel booking request"""
    customer_name: str
    customer_email: str
    departure_city: str
    destination_city: str
    departure_date: str  # ISO format: "2025-01-15"
    return_date: str
    num_passengers: int

@dataclass
class FlightReservation:
    confirmation_code: str
    flight_number: str
    airline: str
    price: float
    departure: str
    arrival: str

@dataclass
class HotelReservation:
    confirmation_code: str
    hotel_name: str
    room_type: str
    price: float
    check_in: str
    check_out: str

@dataclass
class CarReservation:
    confirmation_code: str
    rental_company: str
    car_type: str
    price: float
    pickup_date: str
    return_date: str

class TravelBookingSystem:
    def __init__(self):
        self.flight_reservations: Dict[str, FlightReservation] = {}
        self.hotel_reservations: Dict[str, HotelReservation] = {}
        self.car_reservations: Dict[str, CarReservation] = {}
        self.payment_ids: List[str] = []

    def book_travel_package(self, booking: TravelBooking):
        """
        Book a complete travel package: Flight + Hotel + Car

        THE PROBLEM: If any step fails after previous steps succeeded,
        we're left with partial bookings and the customer is charged!

        We need PROPER COMPENSATIONS (rollback/cancellation) not just retries.
        """
        print(f"\n{'=' * 70}")
        print(f"Processing travel booking for {booking.customer_name}")
        print(f"Route: {booking.departure_city} → {booking.destination_city}")
        print(f"Dates: {booking.departure_date} to {booking.return_date}")
        print(f"Passengers: {booking.num_passengers}")
        print(f"{'=' * 70}\n")

        flight_confirmation = None
        hotel_confirmation = None
        car_confirmation = None

        try:
            # ========================================
            # Step 1: Book Flight
            # ========================================
            print("Step 1: Booking flight...")
            time.sleep(0.5)

            # Simulate flight booking API
            if random.random() < 0.15:
                raise Exception("Flight booking service unavailable")

            flight_price = 350.00 * booking.num_passengers
            flight_confirmation = f"FL-{int(time.time())}-{random.randint(1000, 9999)}"

            flight = FlightReservation(
                confirmation_code=flight_confirmation,
                flight_number="AA1234",
                airline="American Airlines",
                price=flight_price,
                departure=booking.departure_date,
                arrival=booking.departure_date  # Same day for simplicity
            )

            self.flight_reservations[flight_confirmation] = flight
            print(f"✓ Flight booked: {flight_confirmation}")
            print(f"  Price: ${flight_price:.2f}")

            # ========================================
            # Step 2: Book Hotel
            # ========================================
            print("\nStep 2: Booking hotel...")
            time.sleep(0.5)

            # Calculate number of nights
            dep_date = datetime.fromisoformat(booking.departure_date)
            ret_date = datetime.fromisoformat(booking.return_date)
            nights = (ret_date - dep_date).days

            # Simulate hotel booking API (higher failure rate!)
            if random.random() < 0.25:
                raise Exception("Hotel booking system error")

            hotel_price = 150.00 * nights
            hotel_confirmation = f"HT-{int(time.time())}-{random.randint(1000, 9999)}"

            hotel = HotelReservation(
                confirmation_code=hotel_confirmation,
                hotel_name="Grand Plaza Hotel",
                room_type="Standard Double",
                price=hotel_price,
                check_in=booking.departure_date,
                check_out=booking.return_date
            )

            self.hotel_reservations[hotel_confirmation] = hotel
            print(f"✓ Hotel booked: {hotel_confirmation}")
            print(f"  {nights} nights at ${hotel_price:.2f}")

            # ========================================
            # Step 3: Book Rental Car
            # ========================================
            print("\nStep 3: Booking rental car...")
            time.sleep(0.5)

            # Simulate car rental API (VERY high failure rate - 40%!)
            if random.random() < 0.40:
                raise Exception("Car rental system temporarily down")

            car_price = 50.00 * nights
            car_confirmation = f"CR-{int(time.time())}-{random.randint(1000, 9999)}"

            car = CarReservation(
                confirmation_code=car_confirmation,
                rental_company="Enterprise",
                car_type="Economy",
                price=car_price,
                pickup_date=booking.departure_date,
                return_date=booking.return_date
            )

            self.car_reservations[car_confirmation] = car
            print(f"✓ Car rental booked: {car_confirmation}")
            print(f"  ${car_price:.2f}")

            # ========================================
            # Step 4: Process Payment
            # ========================================
            print("\nStep 4: Processing payment...")
            time.sleep(0.5)

            total_price = flight_price + hotel_price + car_price

            # Simulate payment processing
            if random.random() < 0.10:
                raise Exception("Payment processing failed")

            payment_id = f"PAY-{int(time.time())}-{random.randint(1000, 9999)}"
            self.payment_ids.append(payment_id)

            print(f"✓ Payment processed: {payment_id}")
            print(f"  Total charged: ${total_price:.2f}")

            # ========================================
            # Step 5: Send confirmation email
            # ========================================
            print("\nStep 5: Sending confirmation email...")
            time.sleep(0.3)

            # Email can fail, but we don't care - booking is complete
            if random.random() < 0.10:
                print("⚠ Warning: Email service failed, but booking is complete")
            else:
                print(f"✓ Confirmation email sent to {booking.customer_email}")

            print(f"\n{'=' * 70}")
            print("✓ TRAVEL PACKAGE BOOKED SUCCESSFULLY!")
            print(f"Flight: {flight_confirmation}")
            print(f"Hotel: {hotel_confirmation}")
            print(f"Car: {car_confirmation}")
            print(f"Payment: {payment_id}")
            print(f"Total: ${total_price:.2f}")
            print(f"{'=' * 70}\n")

            return {
                'success': True,
                'flight_confirmation': flight_confirmation,
                'hotel_confirmation': hotel_confirmation,
                'car_confirmation': car_confirmation,
                'payment_id': payment_id,
                'total_price': total_price
            }

        except Exception as ex:
            # ========================================
            # CRITICAL PROBLEM: INCOMPLETE ROLLBACK!
            # ========================================
            print(f"\n{'=' * 70}")
            print(f"✗ BOOKING FAILED: {ex}")
            print(f"{'=' * 70}")

            # This is the WRONG way to handle it!
            # We just print warnings but don't actually cancel anything!

            if flight_confirmation:
                print(f"⚠ WARNING: Flight {flight_confirmation} was booked but not cancelled!")
                print("  Customer will be charged for unused flight!")

            if hotel_confirmation:
                print(f"⚠ WARNING: Hotel {hotel_confirmation} was booked but not cancelled!")
                print("  Hotel will charge no-show fee!")

            if car_confirmation:
                print(f"⚠ WARNING: Car {car_confirmation} was booked!")

            # We should be cancelling these reservations, but we're not!
            # This is what SAGA pattern / compensations solve!

            print(f"{'=' * 70}\n")

            return {
                'success': False,
                'error': str(ex),
                'partial_bookings': {
                    'flight': flight_confirmation,
                    'hotel': hotel_confirmation,
                    'car': car_confirmation
                }
            }

    def print_system_state(self):
        """Print current state of all reservations"""
        print("\n" + "=" * 70)
        print("SYSTEM STATE (This shows the problem!)")
        print("=" * 70)
        print(f"Flight Reservations: {len(self.flight_reservations)}")
        print(f"Hotel Reservations: {len(self.hotel_reservations)}")
        print(f"Car Reservations: {len(self.car_reservations)}")
        print(f"Payments Processed: {len(self.payment_ids)}")
        print("\nIf these numbers don't match, we have orphaned reservations!")
        print("This is what proper compensations prevent.")
        print("=" * 70 + "\n")


# Usage example
if __name__ == "__main__":
    system = TravelBookingSystem()

    # Test bookings
    bookings = [
        TravelBooking(
            customer_name="Alice Johnson",
            customer_email="alice@example.com",
            departure_city="San Francisco",
            destination_city="New York",
            departure_date="2025-01-15",
            return_date="2025-01-20",
            num_passengers=2
        ),
        TravelBooking(
            customer_name="Bob Smith",
            customer_email="bob@example.com",
            departure_city="Los Angeles",
            destination_city="Miami",
            departure_date="2025-02-01",
            return_date="2025-02-07",
            num_passengers=1
        ),
        TravelBooking(
            customer_name="Charlie Brown",
            customer_email="charlie@example.com",
            departure_city="Seattle",
            destination_city="Boston",
            departure_date="2025-03-10",
            return_date="2025-03-15",
            num_passengers=3
        ),
    ]

    # Process bookings
    for booking_record in bookings:
        try:
            result = system.book_travel_package(booking_record)
            time.sleep(2)  # Pause between bookings
        except Exception as e:
            print(f"Unexpected error: {e}")

        time.sleep(1)

    # Show the problem
    system.print_system_state()

    print("\n" + "=" * 70)
    print("THE PROBLEM:")
    print("When bookings fail partway through, we have:")
    print("- Orphaned flight reservations (customer charged but no trip)")
    print("- Orphaned hotel reservations (hotel charges no-show fee)")
    print("- Orphaned car reservations (rental company holds the car)")
    print("\nTHE SOLUTION:")
    print("Implement SAGA pattern with proper compensating actions:")
    print("- cancel_flight_reservation()")
    print("- cancel_hotel_reservation()")
    print("- cancel_car_reservation()")
    print("- refund_payment()")
    print("=" * 70 + "\n")