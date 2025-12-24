# seed_data_loader.py
# Utility to load seed data from JSON into database objects

import json
from datetime import datetime, timedelta
from typing import List
from database import (
    Flight, FlightInstance, SeatInstance,
    Hotel, Room, Car, Airport,
    BookingDatabase
)


class SeedDataLoader:

    @staticmethod
    def load_airports(filepath: str = "travel-seed-data.json") -> List[Airport]:
        """Load airports from JSON"""
        with open(filepath, 'r') as f:
            data = json.load(f)

        airports = []
        for a in data['airports']:
            airports.append(Airport(
                code=a['code'],
                name=a['name'],
                city=a['city']
            ))
        return airports

    @staticmethod
    def load_flights(filepath: str = "travel-seed-data.json") -> List[Flight]:
        """Load flight templates from JSON"""
        with open(filepath, 'r') as f:
            data = json.load(f)

        flights = []
        for f_data in data['flights']:
            # For each carriage class with a price, create price entry
            # This matches your Flight.price structure
            flight = Flight(
                flight_number=f_data['flight_number'],
                airline=f_data['airline'],
                price=f_data['base_prices'].get('economy', 0),  # Default to economy
                departure_time=f_data['departure_time'],
                arrival_time=f_data['arrival_time'],
                departure_airport=f_data['departure_airport'],
                arrival_airport=f_data['arrival_airport']
            )
            flights.append(flight)
        return flights

    @staticmethod
    def generate_flight_instances(
            flights: List[Flight],
            start_date: str,
            num_days: int = 30,
            filepath: str = "travel-seed-data.json"
    ) -> List[FlightInstance]:
        """
        Generate FlightInstance objects for each flight for the next N days

        Args:
            flights: List of Flight templates
            start_date: Starting date in ISO format "2025-01-15"
            num_days: Number of days to generate instances for
            filepath: Path to seed data for airplane configs
        """
        with open(filepath, 'r') as f:
            data = json.load(f)

        seat_configs = data['airplane_seat_configs']
        flight_data_map = {f['flight_number']: f for f in data['flights']}

        instances = []
        base_date = datetime.fromisoformat(start_date)

        for flight in flights:
            flight_info = flight_data_map.get(flight.flight_number)
            if not flight_info:
                continue

            airplane = flight_info['airplane']
            seat_config = seat_configs.get(airplane, {})

            # Generate instance for each day
            for day in range(num_days):
                instance_date = (base_date + timedelta(days=day)).strftime('%Y-%m-%d')

                # Generate seats for this instance
                seats = []
                seat_number = 1

                for carriage_class, count in seat_config.items():
                    for _ in range(count):
                        seat = SeatInstance(
                            seat_number=f"{seat_number}{carriage_class[0].upper()}",
                            carriage_class=carriage_class,
                            date=instance_date,
                            is_available=True
                        )
                        seats.append(seat)
                        seat_number += 1

                instance = FlightInstance(
                    flight_number=flight.flight_number,
                    date=instance_date,
                    seats=seats,
                    airplane=airplane
                )
                instances.append(instance)

        return instances

    @staticmethod
    def load_hotels(filepath: str = "travel-seed-data.json") -> List[Hotel]:
        """Load hotels from JSON"""
        with open(filepath, 'r') as f:
            data = json.load(f)

        hotels = []
        for h_data in data['hotels']:
            # Create Room objects
            rooms = []
            for r in h_data['rooms']:
                room = Room(
                    room_number=r['room_number'],
                    room_type=r['room_type'],
                    unavailable_dates=[]  # Start with all dates available
                )
                rooms.append(room)

            # Create Hotel (note: your Hotel.__init__ creates default rooms)
            # We'll need to fix this - hotels should accept rooms in constructor
            hotel = Hotel()
            hotel.hotel_name = h_data['hotel_name']
            hotel.rooms = rooms
            hotel.rates = h_data['rates']

            # Add extra metadata
            hotel.hotel_id = h_data['hotel_id']
            hotel.city = h_data['city']
            hotel.airport_code = h_data['airport_code']

            hotels.append(hotel)

        return hotels

    @staticmethod
    def load_cars(filepath: str = "travel-seed-data.json") -> List[Car]:
        """Load car inventory from JSON"""
        with open(filepath, 'r') as f:
            data = json.load(f)

        cars = []
        car_types = {ct['type_name']: ct for ct in data['car_types']}

        for c_data in data['car_inventory']:
            car_type_info = car_types.get(c_data['car_type'], {})

            car = Car(
                license_plate=c_data['license_plate'],
                make=car_type_info.get('make', 'Unknown'),
                model=car_type_info.get('model', 'Unknown'),
                unavailable_dates=[]
            )
            # Add extra metadata
            car.car_type = c_data['car_type']
            car.location = c_data['location']
            car.daily_rate = car_type_info.get('daily_rate', 50.00)

            cars.append(car)

        return cars

    @staticmethod
    def initialize_database(
            start_date: str = "2025-01-15",
            num_days: int = 30,
            filepath: str = "travel-seed-data.json"
    ) -> BookingDatabase:
        """
        Initialize a complete BookingDatabase with seed data

        Args:
            start_date: Starting date for flight instances
            num_days: Number of days to generate flight instances
            filepath: Path to JSON seed data

        Returns:
            Fully populated BookingDatabase
        """
        db = BookingDatabase()

        # Load all data
        db.airports = SeedDataLoader.load_airports(filepath)
        db.flights = SeedDataLoader.load_flights(filepath)
        db.flight_instances = SeedDataLoader.generate_flight_instances(
            db.flights, start_date, num_days, filepath
        )
        db.hotels = SeedDataLoader.load_hotels(filepath)
        db.cars = SeedDataLoader.load_cars(filepath)

        print(f"Database initialized with:")
        print(f"  {len(db.airports)} airports")
        print(f"  {len(db.flights)} flight routes")
        print(f"  {len(db.flight_instances)} flight instances")
        print(f"  {len(db.hotels)} hotels")
        print(f"  {sum(len(h.rooms) for h in db.hotels)} hotel rooms")
        print(f"  {len(db.cars)} rental cars")

        return db


# Usage example
if __name__ == "__main__":
    # Initialize database with seed data
    db = SeedDataLoader.initialize_database(
        start_date="2025-01-15",
        num_days=30
    )

    # Example queries
    print("\nSample data:")
    print(f"\nFirst flight: {db.flights[0]}")
    print(f"\nFirst hotel: {db.hotels[0].hotel_name} with {len(db.hotels[0].rooms)} rooms")
    print(f"\nFirst car: {db.cars[0]}")
    print(f"\nSample flight instance: {db.flight_instances[0].flight_number} on {db.flight_instances[0].date}")