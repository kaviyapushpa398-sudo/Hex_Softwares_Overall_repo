import java.util.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.stream.*;

// ============================================================
//  TRAVEL BOOKING SYSTEM — Single-File Java Application
//  Covers: Flights · Hotels · Transportation
// ============================================================

public class TravelBookingSystem {

    // ──────────────────────────────────────────────────────────
    //  ENUMS
    // ──────────────────────────────────────────────────────────

    enum SeatClass    { ECONOMY, BUSINESS, FIRST_CLASS }
    enum RoomType     { SINGLE, DOUBLE, SUITE, DELUXE }
    enum VehicleType  { CAR, BUS, TRAIN, TAXI }
    enum BookingStatus{ CONFIRMED, PENDING, CANCELLED }
    enum PaymentMethod{ CREDIT_CARD, DEBIT_CARD, NET_BANKING, UPI, WALLET }

    // ──────────────────────────────────────────────────────────
    //  USER
    // ──────────────────────────────────────────────────────────

    static class User {
        private static int counter = 1000;
        String userId, name, email, phone, password;
        List<Booking> bookingHistory = new ArrayList<>();
        double walletBalance;

        User(String name, String email, String phone, String password) {
            this.userId  = "USR" + (++counter);
            this.name    = name;
            this.email   = email;
            this.phone   = phone;
            this.password = password;
            this.walletBalance = 5000.0;          // welcome credits
        }

        void displayProfile() {
            System.out.println("\n╔══════════════════════════════╗");
            System.out.println("║        USER PROFILE          ║");
            System.out.println("╚══════════════════════════════╝");
            System.out.printf("  ID      : %s%n", userId);
            System.out.printf("  Name    : %s%n", name);
            System.out.printf("  Email   : %s%n", email);
            System.out.printf("  Phone   : %s%n", phone);
            System.out.printf("  Wallet  : ₹%.2f%n", walletBalance);
            System.out.printf("  Bookings: %d%n", bookingHistory.size());
        }
    }

    // ──────────────────────────────────────────────────────────
    //  ABSTRACT SERVICE
    // ──────────────────────────────────────────────────────────

    static abstract class TravelService {
        String serviceId, name, description;
        double basePrice;
        boolean available;

        TravelService(String serviceId, String name, String description, double basePrice) {
            this.serviceId   = serviceId;
            this.name        = name;
            this.description = description;
            this.basePrice   = basePrice;
            this.available   = true;
        }

        abstract void displayDetails();
        abstract double calculatePrice(int quantity, String option);
    }

    // ──────────────────────────────────────────────────────────
    //  FLIGHT SERVICE
    // ──────────────────────────────────────────────────────────

    static class Flight extends TravelService {
        String airline, origin, destination, flightNumber;
        LocalDateTime departure, arrival;
        int totalSeats, availableSeats;
        Map<SeatClass, Integer> classSeats = new LinkedHashMap<>();
        Map<SeatClass, Double>  classPrices = new LinkedHashMap<>();

        Flight(String flightNumber, String airline,
               String origin, String destination,
               LocalDateTime departure, LocalDateTime arrival,
               int totalSeats, double basePrice) {
            super("FL" + flightNumber, airline + " " + flightNumber,
                  origin + " → " + destination, basePrice);
            this.flightNumber  = flightNumber;
            this.airline       = airline;
            this.origin        = origin;
            this.destination   = destination;
            this.departure     = departure;
            this.arrival       = arrival;
            this.totalSeats    = totalSeats;
            this.availableSeats = totalSeats;

            // seat distribution
            classSeats.put(SeatClass.ECONOMY,     (int)(totalSeats * 0.70));
            classSeats.put(SeatClass.BUSINESS,    (int)(totalSeats * 0.20));
            classSeats.put(SeatClass.FIRST_CLASS, (int)(totalSeats * 0.10));

            // pricing multipliers
            classPrices.put(SeatClass.ECONOMY,     basePrice);
            classPrices.put(SeatClass.BUSINESS,    basePrice * 2.5);
            classPrices.put(SeatClass.FIRST_CLASS, basePrice * 5.0);
        }

        Duration getDuration() { return Duration.between(departure, arrival); }

        @Override
        public void displayDetails() {
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");
            Duration dur = getDuration();
            System.out.println("\n┌─────────────────────────────────────────────────┐");
            System.out.printf("│ ✈  %-45s│%n", airline + " — " + flightNumber);
            System.out.println("├─────────────────────────────────────────────────┤");
            System.out.printf("│  Route     : %-35s│%n", origin + " → " + destination);
            System.out.printf("│  Departure : %-35s│%n", departure.format(fmt));
            System.out.printf("│  Arrival   : %-35s│%n", arrival.format(fmt));
            System.out.printf("│  Duration  : %dh %dm%-26s│%n",
                    dur.toHours(), dur.toMinutesPart(), "");
            System.out.printf("│  Seats     : %-35s│%n", availableSeats + " / " + totalSeats + " available");
            System.out.println("├──────────────┬──────────────┬──────────────────┤");
            System.out.println("│  Class       │  Seats Left  │  Price (₹)       │");
            System.out.println("├──────────────┼──────────────┼──────────────────┤");
            for (SeatClass sc : SeatClass.values()) {
                System.out.printf("│  %-12s│  %-12d│  %-16.2f│%n",
                        sc, classSeats.getOrDefault(sc, 0), classPrices.get(sc));
            }
            System.out.println("└──────────────┴──────────────┴──────────────────┘");
        }

        @Override
        public double calculatePrice(int passengers, String seatClass) {
            SeatClass sc = SeatClass.valueOf(seatClass.toUpperCase());
            return classPrices.get(sc) * passengers;
        }

        boolean bookSeats(SeatClass sc, int count) {
            int seats = classSeats.getOrDefault(sc, 0);
            if (seats < count) return false;
            classSeats.put(sc, seats - count);
            availableSeats -= count;
            return true;
        }
    }

    // ──────────────────────────────────────────────────────────
    //  HOTEL SERVICE
    // ──────────────────────────────────────────────────────────

    static class Hotel extends TravelService {
        String location, address;
        int starRating, totalRooms, availableRooms;
        List<String> amenities;
        Map<RoomType, Integer> roomInventory = new LinkedHashMap<>();
        Map<RoomType, Double>  roomPrices    = new LinkedHashMap<>();

        Hotel(String hotelId, String name, String location, String address,
              int starRating, int totalRooms, double basePrice, List<String> amenities) {
            super(hotelId, name, location + " — " + name, basePrice);
            this.location     = location;
            this.address      = address;
            this.starRating   = starRating;
            this.totalRooms   = totalRooms;
            this.availableRooms = totalRooms;
            this.amenities    = amenities;

            roomInventory.put(RoomType.SINGLE, (int)(totalRooms * 0.40));
            roomInventory.put(RoomType.DOUBLE, (int)(totalRooms * 0.35));
            roomInventory.put(RoomType.DELUXE, (int)(totalRooms * 0.15));
            roomInventory.put(RoomType.SUITE,  (int)(totalRooms * 0.10));

            roomPrices.put(RoomType.SINGLE, basePrice);
            roomPrices.put(RoomType.DOUBLE, basePrice * 1.5);
            roomPrices.put(RoomType.DELUXE, basePrice * 2.0);
            roomPrices.put(RoomType.SUITE,  basePrice * 3.5);
        }

        String stars() { return "★".repeat(starRating) + "☆".repeat(5 - starRating); }

        @Override
        public void displayDetails() {
            System.out.println("\n┌─────────────────────────────────────────────────┐");
            System.out.printf("│ 🏨  %-44s│%n", name);
            System.out.println("├─────────────────────────────────────────────────┤");
            System.out.printf("│  Rating   : %-36s│%n", stars());
            System.out.printf("│  Location : %-36s│%n", location);
            System.out.printf("│  Address  : %-36s│%n", address);
            System.out.printf("│  Rooms    : %-36s│%n", availableRooms + " / " + totalRooms + " available");
            System.out.printf("│  Amenities: %-36s│%n", String.join(", ", amenities));
            System.out.println("├──────────────┬──────────────┬──────────────────┤");
            System.out.println("│  Room Type   │  Available   │  Price/Night (₹) │");
            System.out.println("├──────────────┼──────────────┼──────────────────┤");
            for (RoomType rt : RoomType.values()) {
                System.out.printf("│  %-12s│  %-12d│  %-16.2f│%n",
                        rt, roomInventory.getOrDefault(rt, 0), roomPrices.get(rt));
            }
            System.out.println("└──────────────┴──────────────┴──────────────────┘");
        }

        @Override
        public double calculatePrice(int nights, String roomType) {
            RoomType rt = RoomType.valueOf(roomType.toUpperCase());
            return roomPrices.get(rt) * nights;
        }

        boolean bookRoom(RoomType rt) {
            int rooms = roomInventory.getOrDefault(rt, 0);
            if (rooms <= 0) return false;
            roomInventory.put(rt, rooms - 1);
            availableRooms--;
            return true;
        }
    }

    // ──────────────────────────────────────────────────────────
    //  TRANSPORTATION SERVICE
    // ──────────────────────────────────────────────────────────

    static class Transportation extends TravelService {
        String provider, pickupLocation, dropLocation;
        VehicleType vehicleType;
        int capacity, availableVehicles;
        double pricePerKm;
        double distanceKm;

        Transportation(String transId, String provider, VehicleType vehicleType,
                       String pickup, String drop, double distanceKm,
                       double pricePerKm, int capacity, int vehicles) {
            super(transId, provider + " (" + vehicleType + ")",
                  pickup + " → " + drop, pricePerKm * distanceKm);
            this.provider          = provider;
            this.vehicleType       = vehicleType;
            this.pickupLocation    = pickup;
            this.dropLocation      = drop;
            this.distanceKm        = distanceKm;
            this.pricePerKm        = pricePerKm;
            this.capacity          = capacity;
            this.availableVehicles = vehicles;
        }

        String vehicleIcon() {
            return switch (vehicleType) {
                case CAR   -> "🚗";
                case BUS   -> "🚌";
                case TRAIN -> "🚂";
                case TAXI  -> "🚕";
            };
        }

        @Override
        public void displayDetails() {
            System.out.println("\n┌─────────────────────────────────────────────────┐");
            System.out.printf("│ %s  %-43s│%n", vehicleIcon(), provider + " — " + vehicleType);
            System.out.println("├─────────────────────────────────────────────────┤");
            System.out.printf("│  Pickup   : %-36s│%n", pickupLocation);
            System.out.printf("│  Drop     : %-36s│%n", dropLocation);
            System.out.printf("│  Distance : %-36s│%n", distanceKm + " km");
            System.out.printf("│  Rate     : ₹%-35.2f│%n", pricePerKm);
            System.out.printf("│  Capacity : %-36s│%n", capacity + " persons");
            System.out.printf("│  Available: %-36s│%n", availableVehicles + " vehicles");
            System.out.printf("│  Total    : ₹%-35.2f│%n", basePrice);
            System.out.println("└─────────────────────────────────────────────────┘");
        }

        @Override
        public double calculatePrice(int passengers, String option) { return basePrice; }

        boolean book() {
            if (availableVehicles <= 0) return false;
            availableVehicles--;
            return true;
        }
    }

    // ──────────────────────────────────────────────────────────
    //  BOOKING
    // ──────────────────────────────────────────────────────────

    static class Booking {
        private static int counter = 100000;
        String bookingId;
        User user;
        TravelService service;
        BookingStatus status;
        double totalAmount;
        LocalDateTime bookingDate;
        Map<String, Object> details = new LinkedHashMap<>();
        PaymentMethod paymentMethod;

        Booking(User user, TravelService service, double amount,
                PaymentMethod pm, Map<String, Object> details) {
            this.bookingId    = "BK" + (++counter);
            this.user         = user;
            this.service      = service;
            this.totalAmount  = amount;
            this.paymentMethod= pm;
            this.status       = BookingStatus.CONFIRMED;
            this.bookingDate  = LocalDateTime.now();
            this.details      = details;
        }

        void printConfirmation() {
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm:ss");
            System.out.println("\n╔══════════════════════════════════════════════════╗");
            System.out.println("║         ✅  BOOKING CONFIRMED                    ║");
            System.out.println("╠══════════════════════════════════════════════════╣");
            System.out.printf("║  Booking ID   : %-32s║%n", bookingId);
            System.out.printf("║  Passenger    : %-32s║%n", user.name);
            System.out.printf("║  Service      : %-32s║%n", service.name);
            System.out.printf("║  Amount Paid  : ₹%-31.2f║%n", totalAmount);
            System.out.printf("║  Payment Via  : %-32s║%n", paymentMethod);
            System.out.printf("║  Status       : %-32s║%n", status);
            System.out.printf("║  Booked On    : %-32s║%n", bookingDate.format(fmt));
            System.out.println("╠══════════════════════════════════════════════════╣");
            System.out.println("║  DETAILS                                         ║");
            details.forEach((k, v) ->
                System.out.printf("║   %-15s: %-29s║%n", k, v));
            System.out.println("╚══════════════════════════════════════════════════╝");
        }

        void printSummary() {
            System.out.printf("  [%s] %-30s ₹%-10.2f %s%n",
                    bookingId, service.name, totalAmount, status);
        }
    }

    // ──────────────────────────────────────────────────────────
    //  PAYMENT PROCESSOR
    // ──────────────────────────────────────────────────────────

    static class PaymentProcessor {

        static boolean processPayment(User user, double amount, PaymentMethod method) {
            System.out.println("\n  ⏳ Processing payment via " + method + "...");
            simulateDelay(800);

            if (method == PaymentMethod.WALLET) {
                if (user.walletBalance < amount) {
                    System.out.println("  ❌ Insufficient wallet balance!");
                    return false;
                }
                user.walletBalance -= amount;
                System.out.printf("  ✅ ₹%.2f deducted from wallet. Balance: ₹%.2f%n",
                        amount, user.walletBalance);
                return true;
            }
            // Simulate external gateway (always succeeds in demo)
            System.out.printf("  ✅ Payment of ₹%.2f successful.%n", amount);
            return true;
        }

        static void simulateDelay(long ms) {
            try { Thread.sleep(ms); } catch (InterruptedException ignored) {}
        }
    }

    // ──────────────────────────────────────────────────────────
    //  DATA STORE  (in-memory)
    // ──────────────────────────────────────────────────────────

    static class DataStore {
        List<User>           users           = new ArrayList<>();
        List<Flight>         flights         = new ArrayList<>();
        List<Hotel>          hotels          = new ArrayList<>();
        List<Transportation> transportations = new ArrayList<>();
        List<Booking>        allBookings     = new ArrayList<>();
        User currentUser = null;

        void seedData() {
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

            // ── Flights ──────────────────────────────────────────
            flights.add(new Flight("AI101", "Air India",
                    "Delhi", "Mumbai",
                    LocalDateTime.parse("2025-06-15 06:00", dtf),
                    LocalDateTime.parse("2025-06-15 08:10", dtf),
                    200, 4500));
            flights.add(new Flight("6E202", "IndiGo",
                    "Mumbai", "Bangalore",
                    LocalDateTime.parse("2025-06-15 10:30", dtf),
                    LocalDateTime.parse("2025-06-15 12:00", dtf),
                    180, 3200));
            flights.add(new Flight("UK303", "Vistara",
                    "Delhi", "Chennai",
                    LocalDateTime.parse("2025-06-16 07:00", dtf),
                    LocalDateTime.parse("2025-06-16 09:45", dtf),
                    160, 5500));
            flights.add(new Flight("SG404", "SpiceJet",
                    "Kolkata", "Hyderabad",
                    LocalDateTime.parse("2025-06-17 14:00", dtf),
                    LocalDateTime.parse("2025-06-17 16:30", dtf),
                    150, 3800));
            flights.add(new Flight("IX505", "Air Asia",
                    "Bangalore", "Goa",
                    LocalDateTime.parse("2025-06-18 09:00", dtf),
                    LocalDateTime.parse("2025-06-18 10:15", dtf),
                    120, 2800));

            // ── Hotels ───────────────────────────────────────────
            hotels.add(new Hotel("H001", "The Grand Palace",
                    "Mumbai", "Nariman Point, Mumbai — 400021",
                    5, 100, 8000,
                    Arrays.asList("Pool", "Spa", "Gym", "WiFi", "Restaurant")));
            hotels.add(new Hotel("H002", "Comfort Inn",
                    "Delhi", "Connaught Place, New Delhi — 110001",
                    3, 80, 3500,
                    Arrays.asList("WiFi", "Breakfast", "Parking")));
            hotels.add(new Hotel("H003", "Sea View Resort",
                    "Goa", "Calangute Beach, Goa — 403516",
                    4, 60, 6000,
                    Arrays.asList("Beach Access", "Pool", "Bar", "WiFi")));
            hotels.add(new Hotel("H004", "Silicon Stay",
                    "Bangalore", "MG Road, Bangalore — 560001",
                    4, 120, 5000,
                    Arrays.asList("WiFi", "Gym", "Conference Room", "Restaurant")));
            hotels.add(new Hotel("H005", "Heritage Haveli",
                    "Jaipur", "MI Road, Jaipur — 302001",
                    5, 40, 10000,
                    Arrays.asList("Pool", "Spa", "Heritage Tour", "Restaurant", "WiFi")));

            // ── Transportation ───────────────────────────────────
            transportations.add(new Transportation("T001", "OlaCabs",
                    VehicleType.CAR, "Mumbai Airport", "Nariman Point",
                    30, 18, 4, 50));
            transportations.add(new Transportation("T002", "RedBus",
                    VehicleType.BUS, "Delhi ISBT", "Agra Bus Stand",
                    200, 3.5, 45, 10));
            transportations.add(new Transportation("T003", "IRCTC Rail",
                    VehicleType.TRAIN, "Delhi Junction", "Mumbai CST",
                    1380, 1.2, 72, 5));
            transportations.add(new Transportation("T004", "Rapido Taxi",
                    VehicleType.TAXI, "Bangalore Airport", "MG Road",
                    40, 15, 4, 80));
            transportations.add(new Transportation("T005", "KSRTC Bus",
                    VehicleType.BUS, "Bangalore Majestic", "Goa Panaji",
                    580, 2.8, 45, 8));

            // ── Demo user ────────────────────────────────────────
            users.add(new User("Arjun Sharma", "arjun@email.com", "9876543210", "pass123"));
        }
    }

    // ──────────────────────────────────────────────────────────
    //  BOOKING ENGINE
    // ──────────────────────────────────────────────────────────

    static class BookingEngine {
        DataStore store;
        Scanner   sc;

        BookingEngine(DataStore store, Scanner sc) {
            this.store = store;
            this.sc    = sc;
        }

        // ── FLIGHT BOOKING ────────────────────────────────────
        void bookFlight() {
            System.out.println("\n══════════════════ SEARCH FLIGHTS ══════════════════");
            System.out.print("  From (city): "); String from = sc.nextLine().trim();
            System.out.print("  To   (city): "); String to   = sc.nextLine().trim();

            List<Flight> results = store.flights.stream()
                    .filter(f -> f.origin.equalsIgnoreCase(from) &&
                                 f.destination.equalsIgnoreCase(to) &&
                                 f.availableSeats > 0)
                    .collect(Collectors.toList());

            if (results.isEmpty()) {
                System.out.println("  ⚠  No flights found for this route."); return;
            }
            System.out.println("\n  Found " + results.size() + " flight(s):");
            for (int i = 0; i < results.size(); i++) {
                System.out.println("\n  Option " + (i + 1));
                results.get(i).displayDetails();
            }

            System.out.print("\n  Select flight (1-" + results.size() + "): ");
            int choice = readInt(1, results.size()) - 1;
            Flight flight = results.get(choice);

            System.out.println("\n  Seat Classes: 1.ECONOMY  2.BUSINESS  3.FIRST_CLASS");
            System.out.print("  Choose class (1-3): ");
            int ci = readInt(1, 3);
            SeatClass sc2 = SeatClass.values()[ci - 1];

            System.out.print("  Number of passengers: ");
            int pax = readInt(1, 9);

            if (flight.classSeats.getOrDefault(sc2, 0) < pax) {
                System.out.println("  ❌ Not enough seats in " + sc2); return;
            }

            double price = flight.calculatePrice(pax, sc2.name());
            System.out.printf("\n  Total Fare: ₹%.2f%n", price);

            PaymentMethod pm = selectPayment();
            if (!PaymentProcessor.processPayment(store.currentUser, price, pm)) return;

            if (!flight.bookSeats(sc2, pax)) { System.out.println("  ❌ Booking failed."); return; }

            Map<String, Object> det = new LinkedHashMap<>();
            det.put("Flight No",   flight.flightNumber);
            det.put("Route",       flight.origin + " → " + flight.destination);
            det.put("Departure",   flight.departure.toString());
            det.put("Class",       sc2);
            det.put("Passengers",  pax);

            Booking b = new Booking(store.currentUser, flight, price, pm, det);
            store.currentUser.bookingHistory.add(b);
            store.allBookings.add(b);
            b.printConfirmation();
        }

        // ── HOTEL BOOKING ─────────────────────────────────────
        void bookHotel() {
            System.out.println("\n══════════════════ SEARCH HOTELS ═══════════════════");
            System.out.print("  Destination city: ");
            String city = sc.nextLine().trim();

            List<Hotel> results = store.hotels.stream()
                    .filter(h -> h.location.equalsIgnoreCase(city) && h.availableRooms > 0)
                    .collect(Collectors.toList());

            if (results.isEmpty()) {
                System.out.println("  ⚠  No hotels found in " + city); return;
            }
            System.out.println("\n  Found " + results.size() + " hotel(s):");
            for (int i = 0; i < results.size(); i++) {
                System.out.println("\n  Option " + (i + 1));
                results.get(i).displayDetails();
            }

            System.out.print("\n  Select hotel (1-" + results.size() + "): ");
            int choice = readInt(1, results.size()) - 1;
            Hotel hotel = results.get(choice);

            System.out.println("\n  Room Types: 1.SINGLE  2.DOUBLE  3.DELUXE  4.SUITE");
            System.out.print("  Choose room type (1-4): ");
            int ri = readInt(1, 4);
            RoomType rt = RoomType.values()[ri - 1];

            if (hotel.roomInventory.getOrDefault(rt, 0) <= 0) {
                System.out.println("  ❌ No " + rt + " rooms available."); return;
            }

            System.out.print("  Number of nights: ");
            int nights = readInt(1, 60);
            System.out.print("  Check-in date (dd-MM-yyyy): ");
            String checkIn = sc.nextLine().trim();

            double price = hotel.calculatePrice(nights, rt.name());
            System.out.printf("\n  Total Cost: ₹%.2f (%d nights × ₹%.2f)%n",
                    price, nights, hotel.roomPrices.get(rt));

            PaymentMethod pm = selectPayment();
            if (!PaymentProcessor.processPayment(store.currentUser, price, pm)) return;

            hotel.bookRoom(rt);

            Map<String, Object> det = new LinkedHashMap<>();
            det.put("Hotel",    hotel.name);
            det.put("City",     hotel.location);
            det.put("Room",     rt);
            det.put("Check-In", checkIn);
            det.put("Nights",   nights);

            Booking b = new Booking(store.currentUser, hotel, price, pm, det);
            store.currentUser.bookingHistory.add(b);
            store.allBookings.add(b);
            b.printConfirmation();
        }

        // ── TRANSPORT BOOKING ─────────────────────────────────
        void bookTransport() {
            System.out.println("\n══════════════════ SEARCH TRANSPORT ════════════════");
            System.out.println("  Vehicle Types: 1.CAR  2.BUS  3.TRAIN  4.TAXI");
            System.out.print("  Filter by type (0=all, 1-4): ");
            int tf = readInt(0, 4);

            List<Transportation> results;
            if (tf == 0) {
                results = store.transportations.stream()
                        .filter(t -> t.availableVehicles > 0)
                        .collect(Collectors.toList());
            } else {
                VehicleType vt = VehicleType.values()[tf - 1];
                results = store.transportations.stream()
                        .filter(t -> t.vehicleType == vt && t.availableVehicles > 0)
                        .collect(Collectors.toList());
            }

            if (results.isEmpty()) {
                System.out.println("  ⚠  No transport options found."); return;
            }
            System.out.println("\n  Found " + results.size() + " option(s):");
            for (int i = 0; i < results.size(); i++) {
                System.out.println("\n  Option " + (i + 1));
                results.get(i).displayDetails();
            }

            System.out.print("\n  Select transport (1-" + results.size() + "): ");
            int choice = readInt(1, results.size()) - 1;
            Transportation trans = results.get(choice);

            double price = trans.calculatePrice(1, "");
            System.out.printf("\n  Total Cost: ₹%.2f%n", price);

            PaymentMethod pm = selectPayment();
            if (!PaymentProcessor.processPayment(store.currentUser, price, pm)) return;

            if (!trans.book()) { System.out.println("  ❌ Booking failed."); return; }

            Map<String, Object> det = new LinkedHashMap<>();
            det.put("Provider", trans.provider);
            det.put("Vehicle",  trans.vehicleType);
            det.put("Pickup",   trans.pickupLocation);
            det.put("Drop",     trans.dropLocation);
            det.put("Distance", trans.distanceKm + " km");

            Booking b = new Booking(store.currentUser, trans, price, pm, det);
            store.currentUser.bookingHistory.add(b);
            store.allBookings.add(b);
            b.printConfirmation();
        }

        // ── PAYMENT SELECTOR ──────────────────────────────────
        PaymentMethod selectPayment() {
            System.out.println("\n  Payment Methods:");
            System.out.println("    1. Credit Card");
            System.out.println("    2. Debit Card");
            System.out.println("    3. Net Banking");
            System.out.println("    4. UPI");
            System.out.printf("    5. Wallet (Balance: ₹%.2f)%n", store.currentUser.walletBalance);
            System.out.print("  Choose (1-5): ");
            int c = readInt(1, 5);
            return PaymentMethod.values()[c - 1];
        }

        int readInt(int min, int max) {
            while (true) {
                try {
                    int v = Integer.parseInt(sc.nextLine().trim());
                    if (v >= min && v <= max) return v;
                    System.out.printf("  Enter a number between %d and %d: ", min, max);
                } catch (NumberFormatException e) {
                    System.out.print("  Invalid input. Try again: ");
                }
            }
        }
    }

    // ──────────────────────────────────────────────────────────
    //  CANCELLATION MANAGER
    // ──────────────────────────────────────────────────────────

    static class CancellationManager {

        static void cancel(DataStore store, Scanner sc) {
            List<Booking> active = store.currentUser.bookingHistory.stream()
                    .filter(b -> b.status == BookingStatus.CONFIRMED)
                    .collect(Collectors.toList());

            if (active.isEmpty()) {
                System.out.println("\n  No active bookings to cancel."); return;
            }

            System.out.println("\n══════════════════ MY ACTIVE BOOKINGS ══════════════");
            for (int i = 0; i < active.size(); i++) {
                System.out.printf("  %d. ", i + 1);
                active.get(i).printSummary();
            }
            System.out.print("\n  Select booking to cancel (1-" + active.size() + "): ");
            int choice;
            try {
                choice = Integer.parseInt(sc.nextLine().trim()) - 1;
                if (choice < 0 || choice >= active.size()) { System.out.println("  Invalid choice."); return; }
            } catch (NumberFormatException e) { System.out.println("  Invalid input."); return; }

            Booking b = active.get(choice);
            System.out.printf("%n  ⚠  Cancel booking [%s] for ₹%.2f ? (yes/no): ",
                    b.bookingId, b.totalAmount);
            if (!sc.nextLine().trim().equalsIgnoreCase("yes")) {
                System.out.println("  Cancellation aborted."); return;
            }

            b.status = BookingStatus.CANCELLED;
            double refund = b.totalAmount * 0.80;   // 80% refund policy
            store.currentUser.walletBalance += refund;

            System.out.println("\n  ✅ Booking cancelled successfully.");
            System.out.printf("  💰 Refund of ₹%.2f (80%%) added to your wallet.%n", refund);
            System.out.printf("  Wallet Balance: ₹%.2f%n", store.currentUser.walletBalance);
        }
    }

    // ──────────────────────────────────────────────────────────
    //  REPORT GENERATOR
    // ──────────────────────────────────────────────────────────

    static class ReportGenerator {

        static void systemReport(DataStore store) {
            System.out.println("\n╔══════════════════════════════════════════════════╗");
            System.out.println("║            SYSTEM ANALYTICS REPORT              ║");
            System.out.println("╠══════════════════════════════════════════════════╣");

            long flightBookings = store.allBookings.stream()
                    .filter(b -> b.service instanceof Flight).count();
            long hotelBookings  = store.allBookings.stream()
                    .filter(b -> b.service instanceof Hotel).count();
            long transBookings  = store.allBookings.stream()
                    .filter(b -> b.service instanceof Transportation).count();
            double totalRevenue = store.allBookings.stream()
                    .filter(b -> b.status == BookingStatus.CONFIRMED)
                    .mapToDouble(b -> b.totalAmount).sum();

            System.out.printf("║  Total Users        : %-27d║%n", store.users.size());
            System.out.printf("║  Total Bookings     : %-27d║%n", store.allBookings.size());
            System.out.printf("║  ✈  Flight Bookings : %-27d║%n", flightBookings);
            System.out.printf("║  🏨 Hotel  Bookings : %-27d║%n", hotelBookings);
            System.out.printf("║  🚗 Trans  Bookings : %-27d║%n", transBookings);
            System.out.printf("║  Total Revenue      : ₹%-26.2f║%n", totalRevenue);

            System.out.println("╠══════════════════════════════════════════════════╣");
            System.out.println("║  INVENTORY SUMMARY                               ║");
            System.out.println("╠══════════════════════════════════════════════════╣");

            store.flights.forEach(f ->
                System.out.printf("║  ✈ %-15s %3d seats left%-14s║%n",
                        f.flightNumber, f.availableSeats, ""));
            store.hotels.forEach(h ->
                System.out.printf("║  🏨 %-14s %3d rooms left%-14s║%n",
                        h.name.substring(0, Math.min(h.name.length(), 14)), h.availableRooms, ""));

            System.out.println("╚══════════════════════════════════════════════════╝");
        }
    }

    // ──────────────────────────────────────────────────────────
    //  AUTHENTICATION MANAGER
    // ──────────────────────────────────────────────────────────

    static class AuthManager {
        DataStore store;
        Scanner   sc;

        AuthManager(DataStore store, Scanner sc) {
            this.store = store;
            this.sc    = sc;
        }

        boolean login() {
            System.out.println("\n══════════════════ LOGIN ═══════════════════════════");
            System.out.print("  Email   : "); String email = sc.nextLine().trim();
            System.out.print("  Password: "); String pwd   = sc.nextLine().trim();

            Optional<User> user = store.users.stream()
                    .filter(u -> u.email.equals(email) && u.password.equals(pwd))
                    .findFirst();

            if (user.isPresent()) {
                store.currentUser = user.get();
                System.out.println("\n  ✅ Welcome back, " + store.currentUser.name + "!");
                return true;
            }
            System.out.println("  ❌ Invalid credentials.");
            return false;
        }

        void register() {
            System.out.println("\n══════════════════ REGISTER ════════════════════════");
            System.out.print("  Full Name : "); String name  = sc.nextLine().trim();
            System.out.print("  Email     : "); String email = sc.nextLine().trim();
            System.out.print("  Phone     : "); String phone = sc.nextLine().trim();
            System.out.print("  Password  : "); String pwd   = sc.nextLine().trim();

            boolean exists = store.users.stream()
                    .anyMatch(u -> u.email.equalsIgnoreCase(email));
            if (exists) {
                System.out.println("  ❌ Email already registered."); return;
            }

            User u = new User(name, email, phone, pwd);
            store.users.add(u);
            store.currentUser = u;
            System.out.println("\n  ✅ Registration successful! Welcome, " + name + "!");
            System.out.println("  🎁 ₹5,000 welcome credits added to your wallet.");
        }
    }

    // ──────────────────────────────────────────────────────────
    //  MAIN APPLICATION
    // ──────────────────────────────────────────────────────────

    static void printBanner() {
        System.out.println("╔══════════════════════════════════════════════════════╗");
        System.out.println("║                                                      ║");
        System.out.println("║    ✈   TRAVEL BOOKING SYSTEM   🏨                   ║");
        System.out.println("║         Flights · Hotels · Transport                 ║");
        System.out.println("║                                                      ║");
        System.out.println("╚══════════════════════════════════════════════════════╝");
    }

    static void printMainMenu() {
        System.out.println("\n══════════════════ MAIN MENU ═══════════════════════");
        System.out.println("  1. ✈  Book a Flight");
        System.out.println("  2. 🏨 Book a Hotel");
        System.out.println("  3. 🚗 Book Transportation");
        System.out.println("  4. 📋 My Bookings");
        System.out.println("  5. ❌ Cancel a Booking");
        System.out.println("  6. 👤 My Profile");
        System.out.println("  7. 📊 System Report (Admin)");
        System.out.println("  8. 🔍 Browse All Services");
        System.out.println("  9. 🚪 Logout");
        System.out.println("═══════════════════════════════════════════════════");
        System.out.print("  Enter choice: ");
    }

    static void browseAll(DataStore store) {
        System.out.println("\n══════════════════ ALL FLIGHTS ═════════════════════");
        store.flights.forEach(Flight::displayDetails);
        System.out.println("\n══════════════════ ALL HOTELS ══════════════════════");
        store.hotels.forEach(Hotel::displayDetails);
        System.out.println("\n══════════════════ ALL TRANSPORT ═══════════════════");
        store.transportations.forEach(Transportation::displayDetails);
    }

    static void viewMyBookings(DataStore store) {
        System.out.println("\n══════════════════ MY BOOKINGS ═════════════════════");
        if (store.currentUser.bookingHistory.isEmpty()) {
            System.out.println("  No bookings yet."); return;
        }
        store.currentUser.bookingHistory.forEach(b -> {
            System.out.print("  ");
            b.printSummary();
        });
        double total = store.currentUser.bookingHistory.stream()
                .filter(b -> b.status == BookingStatus.CONFIRMED)
                .mapToDouble(b -> b.totalAmount).sum();
        System.out.printf("%n  Total Spent (active): ₹%.2f%n", total);
    }

    // ──────────────────────────────────────────────────────────
    //  ENTRY POINT
    // ──────────────────────────────────────────────────────────

    public static void main(String[] args) {
        Scanner   sc    = new Scanner(System.in);
        DataStore store = new DataStore();
        store.seedData();

        printBanner();

        AuthManager  auth   = new AuthManager(store, sc);
        BookingEngine engine = null;

        // ── Auth loop ────────────────────────────────────────
        while (store.currentUser == null) {
            System.out.println("\n  1. Login");
            System.out.println("  2. Register");
            System.out.println("  3. Exit");
            System.out.print("  Choice: ");
            String ac = sc.nextLine().trim();
            switch (ac) {
                case "1" -> auth.login();
                case "2" -> auth.register();
                case "3" -> { System.out.println("  Goodbye! 👋"); return; }
                default  -> System.out.println("  Invalid choice.");
            }
        }

        engine = new BookingEngine(store, sc);

        // ── Main app loop ────────────────────────────────────
        while (true) {
            printMainMenu();
            String choice = sc.nextLine().trim();

            switch (choice) {
                case "1" -> engine.bookFlight();
                case "2" -> engine.bookHotel();
                case "3" -> engine.bookTransport();
                case "4" -> viewMyBookings(store);
                case "5" -> CancellationManager.cancel(store, sc);
                case "6" -> store.currentUser.displayProfile();
                case "7" -> ReportGenerator.systemReport(store);
                case "8" -> browseAll(store);
                case "9" -> {
                    System.out.println("\n  Logged out. Safe travels! ✈");
                    store.currentUser = null;
                    // Allow re-login
                    while (store.currentUser == null) {
                        System.out.println("\n  1. Login   2. Register   3. Exit");
                        System.out.print("  Choice: ");
                        String ac = sc.nextLine().trim();
                        switch (ac) {
                            case "1" -> auth.login();
                            case "2" -> auth.register();
                            case "3" -> { System.out.println("  Goodbye! 👋"); return; }
                        }
                    }
                    engine = new BookingEngine(store, sc);
                }
                default -> System.out.println("  Invalid choice. Please enter 1-9.");
            }
        }
    }
}