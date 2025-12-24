import random
import uuid
from datetime import datetime

from temporalio.exceptions import ApplicationError

from seed_data_loader import SeedDataLoader
from temporalio import activity
from database import (
    get_booking_database,
    FlightReservationRequest,
    HotelReservationRequest,
    CarReservationRequest,
    TravelBookingRequest,
    CompletedFlightReservation,
    CompletedHotelReservation,
    CompletedCarReservation,
    CompletedPayment,
)

@activity.defn
async def book_flight(request: FlightReservationRequest) -> CompletedFlightReservation:
    db = get_booking_database()
    booking_request = db.get_booking_request(request.confirmation_code)

    # Search for available flight
    result = db.find_available_flight(
        departure_city=request.departure_city,
        destination_city=request.destination_city,
        date=request.departure_date,
        carriage_class=request.carriage_class
    )

    if not result:
        raise ApplicationError(f"No available flights from {request.departure_city} to {request.destination_city}")

    flight, instance, seat = result

    # Book the seat
    seat_success = db.book_seat(flight.flight_number, request.departure_date, seat.seat_number)
    if not seat_success:
        raise ApplicationError(f"Unable to book seats on flight {flight.flight_number} from {request.departure_city} to {request.destination_city}")

    # Get flight template for pricing
    price = flight.price if flight else 350.00

    # Simulate payment
    payment_id = f"PAY-{uuid.uuid4().hex[:8]}"


    # Create completed reservation
    completed_reservation = CompletedFlightReservation(
        confirmation_code=request.confirmation_code,
        customer_name=booking_request.customer_name,
        customer_email=booking_request.customer_email,
        flight_number=flight.flight_number,
        date=request.departure_date,
        seat_numbers=[seat.seat_number],
        carriage_class=request.carriage_class,
        price=price,
        payment_id=payment_id,
        booked_at=datetime.now().isoformat(),
        status="confirmed"
    )

    # Store it
    db.flight_reservations[request.confirmation_code] = completed_reservation
    activity.logger.info(f"✓ Flight booked: {seat.seat_number} on {flight.flight_number}")
    return completed_reservation

@activity.defn
async def book_hotel(hotel: HotelReservationRequest) -> CompletedHotelReservation:
    db = get_booking_database()

    # Calculate number of nights
    out_date = datetime.fromisoformat(hotel.check_out)
    in_date = datetime.fromisoformat(hotel.check_in)
    nights = (out_date - in_date).days
    hotel_price = 150.00 * nights

    if random.random() < 0.25:
        raise Exception("Hotel booking service unavailable")

    return CompletedHotelReservation(
        confirmation_code=hotel.confirmation_code,
    )

@activity.defn
async def book_car(car: CarReservationRequest) -> CompletedCarReservation:
    db = get_booking_database()

    car_price = 50.00 * nights

    if random.random() < 0.40:
        raise Exception("Car booking service unavailable")

    return CompletedCarReservation()

async def accept_payment(booking: TravelBookingRequest) -> CompletedPayment:
    db = get_booking_database()

    # Simulate payment processing
    if random.random() < 0.10:
        raise Exception("Payment processing failed")

    payment_id = f"PAY-{random.randint(1000, 9999)}"

    if random.random() < 0.15:
        raise Exception("Payment service unavailable")

    payment = uuid.uuid4().hex[:8]
    return CompletedPayment(
        id = payment_id,
        confirmation_code = booking.confirmation_code,
    )
