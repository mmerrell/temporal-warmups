# models.py - Simple data models for travel booking
from dataclasses import dataclass
from typing import Optional

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
    """Completed flight reservation"""
    reservation_id: str
    customer_name: str
    flight_number: str
    departure_city: str
    destination_city: str
    date: str
    price: float


@dataclass
class HotelReservation:
    """Completed hotel reservation"""
    reservation_id: str
    customer_name: str
    hotel_name: str
    city: str
    check_in: str
    check_out: str
    price: float


@dataclass
class CarReservation:
    """Completed car rental"""
    reservation_id: str
    customer_name: str
    car_type: str
    city: str
    pickup_date: str
    return_date: str
    price: float


@dataclass
class Payment:
    """Payment record"""
    payment_id: str
    reservation_id: str
    amount: float