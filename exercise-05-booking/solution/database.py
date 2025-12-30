# database.py - Updated with fixes and Airport class
from dataclasses import dataclass
from datetime import datetime, timedelta
from typing import Dict, List, Optional

from temporalio.exceptions import ApplicationError


@dataclass
class Airport:
    code: str
    name: str
    city: str


@dataclass
class TravelBookingRequest:
    confirmation_code: str  # System generated
    customer_name: str
    customer_email: str
    departure_city: str
    destination_city: str
    carriage_class: str
    departure_date: str  # ISO format: "2025-01-15"
    return_date: str
    num_passengers: int
    airline: Optional[str] = ""
    room_type: str = "standard"
    car_type: str = "economy"


# Flight template - flies this route every day
@dataclass
class Flight:
    flight_number: str
    airline: str
    price: float  # Base price for economy
    departure_time: str
    arrival_time: str
    departure_airport: str
    arrival_airport: str


@dataclass
class SeatInstance:
    """A specific seat on a specific flight instance"""
    seat_number: str
    carriage_class: str
    date: str
    is_available: bool


@dataclass
class FlightInstance:
    """An actual flight on a particular day"""
    flight_number: str
    date: str
    seats: List[SeatInstance]
    airplane: str


@dataclass
class FlightReservationRequest:
    confirmation_code: str
    carriage_class: str
    departure_date: str
    departure_city: str
    destination_city: str
    airline: Optional[str] = ""


@dataclass
class CompletedFlightReservation:
    """The actual booked flight reservation"""
    confirmation_code: str
    customer_name: str
    customer_email: str
    flight_number: str
    date: str
    seat_numbers: List[str]
    carriage_class: str
    price: float
    booked_at: str  # timestamp


@dataclass
class Room:
    room_number: str
    room_type: str
    unavailable_dates: List[str]

    def is_available(self, dates: List[str]) -> bool:
        """Check if room is available for all requested dates"""
        return not any(date in self.unavailable_dates for date in dates)

    def book_dates(self, dates: List[str]):
        """Mark dates as unavailable"""
        for date in dates:
            if date not in self.unavailable_dates:
                self.unavailable_dates.append(date)


@dataclass
class Hotel:
    hotel_name: str = ""
    hotel_id: str = ""
    city: str = ""
    airport_code: str = ""
    rooms: List[Room] = None
    rates: Dict[str, float] = None

    def __post_init__(self):
        """Initialize default values if not provided"""
        if self.rooms is None:
            # Default rooms (for backward compatibility)
            self.rooms = [
                Room('101', 'standard', []),
                Room('102', 'standard', []),
                Room('103', 'standard', []),
                Room('201', 'deluxe', []),
                Room('202', 'deluxe', []),
                Room('301', 'suite', []),
            ]

        if self.rates is None:
            self.rates = {
                'standard': 100.00,
                'deluxe': 150.00,
                'suite': 250.00,
            }

    def book_room(self, room_type: str, dates: List[str]) -> str:
        """
        Book first available room of specified type

        Returns:
            room_number of booked room

        Raises:
            ApplicationError if no rooms available
        """
        available_rooms = [r for r in self.rooms
                           if r.room_type == room_type and r.is_available(dates)]

        if not available_rooms:
            raise ApplicationError(f"No available {room_type} rooms for dates {dates}")

        # Book the first available room
        room = available_rooms[0]
        room.book_dates(dates)
        return room.room_number


@dataclass
class HotelReservationRequest:
    confirmation_code: str
    city: str
    hotel_name: str
    room_type: str
    check_in: str
    check_out: str


@dataclass
class CompletedHotelReservation:
    confirmation_code: str
    customer_name: str
    hotel_name: str
    room_number: str
    room_type: str
    check_in: str
    check_out: str
    price: float
    booked_at: str

    def nights(self) -> int:
        dep_date = datetime.fromisoformat(self.check_out)
        ret_date = datetime.fromisoformat(self.check_in)
        return (ret_date - dep_date).days

@dataclass
class Car:
    license_plate: str
    make: str
    model: str
    unavailable_dates: List[str]  # Dates when car is NOT available

    # Extra metadata (added by loader)
    car_type: str = ""
    location: str = ""
    daily_rate: float = 50.00

    def is_available(self, dates: List[str]) -> bool:
        """Check if car is available for all requested dates"""
        return not any(date in self.unavailable_dates for date in dates)

    def book_dates(self, dates: List[str]):
        """Mark dates as unavailable"""
        for date in dates:
            if date not in self.unavailable_dates:
                self.unavailable_dates.append(date)


@dataclass
class CarReservationRequest:
    confirmation_code: str
    rental_company: str
    car_type: str
    pickup_date: str
    return_date: str


@dataclass
class CompletedCarReservation:
    confirmation_code: str
    customer_name: str
    license_plate: str
    car_type: str
    pickup_date: str
    return_date: str
    price: float
    booked_at: str


@dataclass
class CompletedTravelBooking:
    """Master travel package with payment"""
    confirmation_code: str
    customer_name: str
    customer_email: str
    payment_id: str
    total_price: float
    booked_at: str


### Payment Stuff
@dataclass
class PaymentRequest:
    confirmation_code: str
    price: float


@dataclass
class CompletedPayment:
    id: str
    confirmation_code: str


class BookingDatabase:
    """Central database for all travel bookings"""

    def __init__(self):
        # Inventory
        self.airports: List[Airport] = []
        self.flights: List[Flight] = []
        self.flight_instances: List[FlightInstance] = []
        self.hotels: List[Hotel] = []
        self.cars: List[Car] = []

        # Booking requests (original customer requests)
        self.booking_requests: Dict[str, TravelBookingRequest] = {}

        # Completed reservations (stored by confirmation_code)
        self.flight_reservations: Dict[str, CompletedFlightReservation] = {}
        self.hotel_reservations: Dict[str, CompletedHotelReservation] = {}
        self.car_reservations: Dict[str, CompletedCarReservation] = {}

        # Master travel bookings (with payment_id)
        self.completed_bookings: Dict[str, CompletedTravelBooking] = {}

    def add_booking_request(self, confirmation_code: str, request: TravelBookingRequest):
        """Store the original booking request"""
        self.booking_requests[confirmation_code] = request

    def get_booking_request(self, confirmation_code: str) -> TravelBookingRequest:
        """Retrieve the original booking request"""
        return self.booking_requests[confirmation_code]

    def find_flight(self, flight_number: str, date: str) -> Optional[FlightInstance]:
        """Find a specific flight instance"""
        for fi in self.flight_instances:
            if fi.flight_number == flight_number and fi.date == date:
                return fi
        return None

    def find_available_flight(
            self,
            departure_city: str,
            destination_city: str,
            date: str,
            carriage_class: str
    ) -> Optional[tuple[Flight, FlightInstance, SeatInstance]]:
        """
        Find an available flight matching criteria

        Returns:
            Tuple of (Flight template, FlightInstance, available SeatInstance)
            or None if no matching flight found
        """
        # Map cities to airport codes
        city_to_airport = {
            airport.city: airport.code
            for airport in self.airports
        }

        departure_airport = city_to_airport.get(departure_city)
        arrival_airport = city_to_airport.get(destination_city)

        if not departure_airport or not arrival_airport:
            return None  # City not found

        # Find flight templates matching route
        matching_flights = [
            f for f in self.flights
            if f.departure_airport == departure_airport
               and f.arrival_airport == arrival_airport
        ]

        if not matching_flights:
            return None  # No flights on this route

        # Check each matching flight for availability on the date
        for flight in matching_flights:
            instance = self.get_flight_instance(flight.flight_number, date)
            if not instance:
                continue

            # Find available seat in requested class
            seat = self.find_available_seat(flight.flight_number, date, carriage_class)
            if seat:
                return flight, instance, seat

        return None

    def find_available_hotel_room(
            self,
            city: str,
            room_type: str,
            check_in: str,
            check_out: str
    ) -> Optional[tuple[Hotel, Room]]:
        """
        Find available hotel room matching criteria

        Returns:
            Tuple of (Hotel, Room) or None if not found
        """
        # Find hotels in the city
        matching_hotels = [h for h in self.hotels if h.city == city]

        if not matching_hotels:
            return None

        # Generate list of dates needed
        start = datetime.fromisoformat(check_in)
        end = datetime.fromisoformat(check_out)
        dates = []
        current = start
        while current < end:
            dates.append(current.strftime('%Y-%m-%d'))
            current += timedelta(days=1)

        # Check each hotel for available room
        for hotel in matching_hotels:
            for room in hotel.rooms:
                if room.room_type == room_type and room.is_available(dates):
                    return hotel, room

        return None

    def find_available_car(
            self,
            city: str,
            car_type: str,
            pickup_date: str,
            return_date: str
    ) -> Optional[Car]:
        """
        Find available rental car matching criteria

        Returns:
            Car or None if not found
        """
        # Map city to airport code
        city_to_airport = {airport.city: airport.code for airport in self.airports}
        location = city_to_airport.get(city)

        if not location:
            return None

        # Generate list of dates needed
        start = datetime.fromisoformat(pickup_date)
        end = datetime.fromisoformat(return_date)
        dates = []
        current = start
        while current <= end:  # Include return date
            dates.append(current.strftime('%Y-%m-%d'))
            current += timedelta(days=1)

        # Find available car at location
        for car in self.cars:
            if (car.location == location and
                    car.car_type == car_type and
                    car.is_available(dates)):
                return car

        return None

    def get_available_seats(self, flight_number: str, date: str, carriage_class: str) -> List[SeatInstance]:
        """Get all available seats in a specific class"""
        instance = self.get_flight_instance(flight_number, date)
        if not instance:
            return []

        return [s for s in instance.seats
                if s.carriage_class == carriage_class and s.is_available]

    def find_hotel_by_city(self, city: str) -> Optional[Hotel]:
        """Find first hotel in a city"""
        for hotel in self.hotels:
            if hotel.city == city:
                return hotel
        return None

    def get_flight_instance(self, flight_number: str, date: str) -> Optional[FlightInstance]:
        """Get specific flight instance by number and date"""
        return next(
            (fi for fi in self.flight_instances
             if fi.flight_number == flight_number and fi.date == date),
            None
        )

    def find_available_seat(
            self,
            flight_number: str,
            date: str,
            carriage_class: str
    ) -> Optional[SeatInstance]:
        """
        Find first available seat in specified class

        Returns:
            SeatInstance if available seat found, None otherwise
        """
        instance = self.get_flight_instance(flight_number, date)
        if not instance:
            return None

        # Find first available seat in the requested class
        for seat in instance.seats:
            if seat.carriage_class == carriage_class and seat.is_available:
                return seat

        return None

    def book_seat(
            self,
            flight_number: str,
            date: str,
            seat_number: str
    ) -> bool:
        """
        Mark a specific seat as booked

        Returns:
            True if seat was successfully booked, False if not found or already booked
        """
        instance = self.get_flight_instance(flight_number, date)
        if not instance:
            return False

        for seat in instance.seats:
            if seat.seat_number == seat_number:
                if not seat.is_available:
                    return False  # Already booked
                seat.is_available = False
                return True

        return False  # Seat not found

    def release_seat(
            self,
            flight_number: str,
            date: str,
            seat_number: str
    ) -> bool:
        """
        Release a booked seat (for cancellations/compensations)

        Returns:
            True if seat was successfully released, False if not found
        """
        instance = self.get_flight_instance(flight_number, date)
        if not instance:
            return False

        for seat in instance.seats:
            if seat.seat_number == seat_number:
                seat.is_available = True
                return True

        return False  # Seat not found

    def get_available_seat_count(
            self,
            flight_number: str,
            date: str,
            carriage_class: str
    ) -> int:
        """
        Count available seats in specified class

        Useful for checking availability before booking
        """
        instance = self.get_flight_instance(flight_number, date)
        if not instance:
            return 0

        return sum(
            1 for seat in instance.seats
            if seat.carriage_class == carriage_class and seat.is_available
        )


_global_db = None


def get_booking_database() -> BookingDatabase:
    """Get or create the global booking database singleton"""
    global _global_db
    if _global_db is None:
        from seed_data_loader import SeedDataLoader
        _global_db = SeedDataLoader.initialize_database(
            start_date="2025-01-20",
            num_days=60
        )
        print("✓ Booking database initialized")
    return _global_db