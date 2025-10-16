# GET all bookings
curl http://localhost:8080/api/bookings

# GET one booking
curl http://localhost:8080/api/bookings/1

# POST - Create a new booking
curl -X POST http://localhost:8080/api/bookings \
  -H "Content-Type: application/json" \
  -d '{
    "customerName": "Jane Doe",
    "email": "jane@example.com",
    "phone": "555-1234",
    "reservationDate": "2025-10-15",
    "reservationTime": "12:00",
    "numberOfGuests": 4
  }'

# PUT - Update booking
curl -X PUT http://localhost:8080/api/bookings/1 \
  -H "Content-Type: application/json" \
  -d '{
    "customerName": "Jane Smith",
    "email": "jane.smith@example.com",
    "phone": "555-5678",
    "reservationDate": "2025-10-16",
    "reservationTime": "14:00",
    "numberOfGuests": 6
  }'

# DELETE booking
curl -X DELETE http://localhost:8080/api/bookings/1
