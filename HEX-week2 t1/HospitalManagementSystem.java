import java.sql.*;
import java.util.Scanner;

/**
 * ╔══════════════════════════════════════════════════════════════╗
 * ║          HOSPITAL MANAGEMENT SYSTEM - Java + JDBC           ║
 * ║  Features: Patients | Doctors | Appointments | Billing      ║
 * ╚══════════════════════════════════════════════════════════════╝
 *
 * DATABASE  : SQLite (via SQLite JDBC driver)
 * COMPILE   : javac HospitalManagementSystem.java
 * RUN       : java -cp .:sqlite-jdbc-*.jar HospitalManagementSystem
 *
 * Maven dependency (add to pom.xml):
 *   <dependency>
 *       <groupId>org.xerial</groupId>
 *       <artifactId>sqlite-jdbc</artifactId>
 *       <version>3.45.1.0</version>
 *   </dependency>
 *
 * Or download the jar:
 *   https://github.com/xerial/sqlite-jdbc/releases
 */
public class HospitalManagementSystem {

    // ─────────────────────────────────────────────────────────────────────────
    //  DATABASE CONNECTION SETTINGS
    // ─────────────────────────────────────────────────────────────────────────
    private static final String DB_URL = "jdbc:sqlite:hospital.db";
    private static Connection conn;
    private static final Scanner sc = new Scanner(System.in);

    // ANSI color codes for a polished console UI
    private static final String RESET  = "\u001B[0m";
    private static final String BOLD   = "\u001B[1m";
    private static final String CYAN   = "\u001B[36m";
    private static final String GREEN  = "\u001B[32m";
    private static final String YELLOW = "\u001B[33m";
    private static final String RED    = "\u001B[31m";
    private static final String BLUE   = "\u001B[34m";
    private static final String PURPLE = "\u001B[35m";

    // =========================================================================
    //  ENTRY POINT
    // =========================================================================
    public static void main(String[] args) {
        System.out.println(CYAN + BOLD);
        System.out.println("╔══════════════════════════════════════════════════════╗");
        System.out.println("║        HOSPITAL MANAGEMENT SYSTEM  v1.0             ║");
        System.out.println("║           Powered by Java + JDBC (SQLite)           ║");
        System.out.println("╚══════════════════════════════════════════════════════╝");
        System.out.println(RESET);

        try {
            initDatabase();
            mainMenu();
        } catch (Exception e) {
            System.out.println(RED + "Fatal error: " + e.getMessage() + RESET);
            e.printStackTrace();
        } finally {
            closeConnection();
            sc.close();
        }
    }

    // =========================================================================
    //  DATABASE INITIALISATION
    // =========================================================================
    private static void initDatabase() throws SQLException {
        try { Class.forName("org.sqlite.JDBC"); }
        catch (ClassNotFoundException e) { throw new SQLException("SQLite driver not found: " + e.getMessage()); }
        conn = DriverManager.getConnection(DB_URL);
        conn.setAutoCommit(true);

        try (Statement st = conn.createStatement()) {

            // ── DEPARTMENTS ────────────────────────────────────────────────
            st.execute("""
                CREATE TABLE IF NOT EXISTS departments (
                    dept_id   INTEGER PRIMARY KEY AUTOINCREMENT,
                    name      TEXT    NOT NULL UNIQUE,
                    location  TEXT
                )
            """);

            // ── DOCTORS ───────────────────────────────────────────────────
            st.execute("""
                CREATE TABLE IF NOT EXISTS doctors (
                    doctor_id    INTEGER PRIMARY KEY AUTOINCREMENT,
                    name         TEXT    NOT NULL,
                    specialization TEXT  NOT NULL,
                    phone        TEXT,
                    email        TEXT    UNIQUE,
                    dept_id      INTEGER REFERENCES departments(dept_id),
                    available    INTEGER DEFAULT 1
                )
            """);

            // ── PATIENTS ──────────────────────────────────────────────────
            st.execute("""
                CREATE TABLE IF NOT EXISTS patients (
                    patient_id  INTEGER PRIMARY KEY AUTOINCREMENT,
                    name        TEXT    NOT NULL,
                    age         INTEGER,
                    gender      TEXT,
                    phone       TEXT,
                    email       TEXT,
                    address     TEXT,
                    blood_group TEXT,
                    admit_date  TEXT    DEFAULT (DATE('now'))
                )
            """);

            // ── APPOINTMENTS ──────────────────────────────────────────────
            st.execute("""
                CREATE TABLE IF NOT EXISTS appointments (
                    appt_id    INTEGER PRIMARY KEY AUTOINCREMENT,
                    patient_id INTEGER NOT NULL REFERENCES patients(patient_id),
                    doctor_id  INTEGER NOT NULL REFERENCES doctors(doctor_id),
                    appt_date  TEXT    NOT NULL,
                    appt_time  TEXT    NOT NULL,
                    reason     TEXT,
                    status     TEXT    DEFAULT 'Scheduled'
                )
            """);

            // ── MEDICAL RECORDS ───────────────────────────────────────────
            st.execute("""
                CREATE TABLE IF NOT EXISTS medical_records (
                    record_id   INTEGER PRIMARY KEY AUTOINCREMENT,
                    patient_id  INTEGER NOT NULL REFERENCES patients(patient_id),
                    doctor_id   INTEGER NOT NULL REFERENCES doctors(doctor_id),
                    visit_date  TEXT    DEFAULT (DATE('now')),
                    diagnosis   TEXT,
                    prescription TEXT,
                    notes       TEXT
                )
            """);

            // ── BILLING ───────────────────────────────────────────────────
            st.execute("""
                CREATE TABLE IF NOT EXISTS billing (
                    bill_id     INTEGER PRIMARY KEY AUTOINCREMENT,
                    patient_id  INTEGER NOT NULL REFERENCES patients(patient_id),
                    total_amount REAL   NOT NULL,
                    paid_amount  REAL   DEFAULT 0,
                    bill_date   TEXT    DEFAULT (DATE('now')),
                    status      TEXT    DEFAULT 'Unpaid',
                    description TEXT
                )
            """);

            // ── SEED DEMO DATA (only if tables are empty) ─────────────────
            seedDemoData(st);
        }

        System.out.println(GREEN + "✔  Database initialised successfully (hospital.db)\n" + RESET);
    }

    private static void seedDemoData(Statement st) throws SQLException {
        ResultSet rs = st.executeQuery("SELECT COUNT(*) AS cnt FROM departments");
        if (rs.getInt("cnt") > 0) return;          // already seeded

        st.execute("INSERT INTO departments(name,location) VALUES('Cardiology','Block A')");
        st.execute("INSERT INTO departments(name,location) VALUES('Neurology','Block B')");
        st.execute("INSERT INTO departments(name,location) VALUES('Orthopedics','Block C')");
        st.execute("INSERT INTO departments(name,location) VALUES('General Medicine','Block D')");

        st.execute("INSERT INTO doctors(name,specialization,phone,email,dept_id) VALUES('Dr. Arjun Sharma','Cardiologist','9876543210','arjun@hospital.com',1)");
        st.execute("INSERT INTO doctors(name,specialization,phone,email,dept_id) VALUES('Dr. Priya Menon','Neurologist','9876543211','priya@hospital.com',2)");
        st.execute("INSERT INTO doctors(name,specialization,phone,email,dept_id) VALUES('Dr. Ravi Kumar','Orthopedic Surgeon','9876543212','ravi@hospital.com',3)");
        st.execute("INSERT INTO doctors(name,specialization,phone,email,dept_id) VALUES('Dr. Anitha Nair','General Physician','9876543213','anitha@hospital.com',4)");

        System.out.println(YELLOW + "ℹ  Demo data seeded (4 departments, 4 doctors)." + RESET);
    }

    private static void closeConnection() {
        try { if (conn != null && !conn.isClosed()) conn.close(); }
        catch (SQLException ignored) {}
    }

    // =========================================================================
    //  MAIN MENU
    // =========================================================================
    private static void mainMenu() {
        while (true) {
            System.out.println(BOLD + CYAN);
            System.out.println("\n╔═══════════════════════════════╗");
            System.out.println("║         MAIN  MENU            ║");
            System.out.println("╠═══════════════════════════════╣");
            System.out.println("║  1. Patient Management        ║");
            System.out.println("║  2. Doctor Management         ║");
            System.out.println("║  3. Appointment Management    ║");
            System.out.println("║  4. Medical Records           ║");
            System.out.println("║  5. Billing & Payments        ║");
            System.out.println("║  6. Department Management     ║");
            System.out.println("║  7. Reports & Search          ║");
            System.out.println("║  0. Exit                      ║");
            System.out.println("╚═══════════════════════════════╝" + RESET);
            System.out.print(BOLD + "  ➤  Choose option: " + RESET);

            int choice = readInt();
            switch (choice) {
                case 1 -> patientMenu();
                case 2 -> doctorMenu();
                case 3 -> appointmentMenu();
                case 4 -> medicalRecordMenu();
                case 5 -> billingMenu();
                case 6 -> departmentMenu();
                case 7 -> reportsMenu();
                case 0 -> { System.out.println(GREEN + "\nGoodbye! Stay Healthy 💊\n" + RESET); return; }
                default -> System.out.println(RED + "Invalid option!" + RESET);
            }
        }
    }

    // =========================================================================
    //  1. PATIENT MANAGEMENT
    // =========================================================================
    private static void patientMenu() {
        while (true) {
            printSubMenu("PATIENT MANAGEMENT",
                    new String[]{"Add New Patient", "View All Patients",
                            "Search Patient by ID/Name", "Update Patient",
                            "Delete Patient", "Back to Main Menu"});
            int ch = readInt();
            switch (ch) {
                case 1 -> addPatient();
                case 2 -> viewAllPatients();
                case 3 -> searchPatient();
                case 4 -> updatePatient();
                case 5 -> deletePatient();
                case 0 -> { return; }
                default -> System.out.println(RED + "Invalid option!" + RESET);
            }
        }
    }

    private static void addPatient() {
        System.out.println(GREEN + "\n── Add New Patient ──" + RESET);
        System.out.print("Full Name       : "); String name   = sc.nextLine().trim();
        System.out.print("Age             : "); int age        = readInt();
        System.out.print("Gender (M/F/O)  : "); String gender = sc.nextLine().trim();
        System.out.print("Phone           : "); String phone   = sc.nextLine().trim();
        System.out.print("Email           : "); String email   = sc.nextLine().trim();
        System.out.print("Address         : "); String addr    = sc.nextLine().trim();
        System.out.print("Blood Group     : "); String blood   = sc.nextLine().trim();

        String sql = "INSERT INTO patients(name,age,gender,phone,email,address,blood_group) VALUES(?,?,?,?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, name);  ps.setInt(2, age);  ps.setString(3, gender);
            ps.setString(4, phone); ps.setString(5, email);
            ps.setString(6, addr);  ps.setString(7, blood);
            ps.executeUpdate();
            ResultSet gk = ps.getGeneratedKeys();
            System.out.println(GREEN + "✔  Patient added! ID = " + gk.getInt(1) + RESET);
        } catch (SQLException e) { sqlError(e); }
    }

    private static void viewAllPatients() {
        String sql = "SELECT * FROM patients ORDER BY patient_id";
        System.out.println(CYAN + "\n" + "─".repeat(100));
        System.out.printf("%-5s %-22s %-4s %-6s %-14s %-20s %-8s %-12s%n",
                "ID","Name","Age","Gender","Phone","Email","Blood","Admit Date");
        System.out.println("─".repeat(100) + RESET);
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            int count = 0;
            while (rs.next()) {
                System.out.printf("%-5d %-22s %-4d %-6s %-14s %-20s %-8s %-12s%n",
                        rs.getInt("patient_id"), rs.getString("name"),
                        rs.getInt("age"), rs.getString("gender"),
                        rs.getString("phone"), rs.getString("email"),
                        rs.getString("blood_group"), rs.getString("admit_date"));
                count++;
            }
            System.out.println(CYAN + "─".repeat(100) + RESET);
            System.out.println(YELLOW + "Total: " + count + " patient(s)" + RESET);
        } catch (SQLException e) { sqlError(e); }
    }

    private static void searchPatient() {
        System.out.print("Search by (1) ID  (2) Name : ");
        int opt = readInt();
        String sql;
        PreparedStatement ps;
        try {
            if (opt == 1) {
                System.out.print("Enter Patient ID: ");
                int id = readInt();
                sql = "SELECT * FROM patients WHERE patient_id = ?";
                ps  = conn.prepareStatement(sql);
                ps.setInt(1, id);
            } else {
                System.out.print("Enter name keyword: ");
                String kw = "%" + sc.nextLine().trim() + "%";
                sql = "SELECT * FROM patients WHERE name LIKE ?";
                ps  = conn.prepareStatement(sql);
                ps.setString(1, kw);
            }
            ResultSet rs = ps.executeQuery();
            System.out.println(CYAN + "\n── Search Results ──" + RESET);
            boolean found = false;
            while (rs.next()) {
                found = true;
                printPatientCard(rs);
            }
            if (!found) System.out.println(YELLOW + "No matching patients found." + RESET);
            ps.close();
        } catch (SQLException e) { sqlError(e); }
    }

    private static void printPatientCard(ResultSet rs) throws SQLException {
        System.out.println(PURPLE + "┌─────────────────────────────────────────┐" + RESET);
        System.out.printf(PURPLE + "│" + RESET + " ID: %-5d  Name: %-25s " + PURPLE + "│%n" + RESET,
                rs.getInt("patient_id"), rs.getString("name"));
        System.out.printf(PURPLE + "│" + RESET + " Age: %-3d  Gender: %-4s  Blood: %-4s    " + PURPLE + "│%n" + RESET,
                rs.getInt("age"), rs.getString("gender"), rs.getString("blood_group"));
        System.out.printf(PURPLE + "│" + RESET + " Phone: %-13s  Admitted: %-10s " + PURPLE + "│%n" + RESET,
                rs.getString("phone"), rs.getString("admit_date"));
        System.out.println(PURPLE + "└─────────────────────────────────────────┘" + RESET);
    }

    private static void updatePatient() {
        System.out.print("Enter Patient ID to update: "); int id = readInt();
        System.out.print("New Phone : "); String phone = sc.nextLine().trim();
        System.out.print("New Email : "); String email = sc.nextLine().trim();
        System.out.print("New Address: "); String addr = sc.nextLine().trim();

        String sql = "UPDATE patients SET phone=?, email=?, address=? WHERE patient_id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, phone); ps.setString(2, email);
            ps.setString(3, addr);  ps.setInt(4, id);
            int rows = ps.executeUpdate();
            System.out.println(rows > 0 ? GREEN + "✔  Patient updated!" + RESET : RED + "Patient not found." + RESET);
        } catch (SQLException e) { sqlError(e); }
    }

    private static void deletePatient() {
        System.out.print("Enter Patient ID to delete: "); int id = readInt();
        System.out.print(RED + "Are you sure? (yes/no): " + RESET); String confirm = sc.nextLine().trim();
        if (!confirm.equalsIgnoreCase("yes")) { System.out.println("Cancelled."); return; }

        try (PreparedStatement ps = conn.prepareStatement("DELETE FROM patients WHERE patient_id=?")) {
            ps.setInt(1, id);
            int rows = ps.executeUpdate();
            System.out.println(rows > 0 ? GREEN + "✔  Patient deleted." + RESET : RED + "Patient not found." + RESET);
        } catch (SQLException e) { sqlError(e); }
    }

    // =========================================================================
    //  2. DOCTOR MANAGEMENT
    // =========================================================================
    private static void doctorMenu() {
        while (true) {
            printSubMenu("DOCTOR MANAGEMENT",
                    new String[]{"Add Doctor", "View All Doctors",
                            "View Doctors by Department", "Update Availability",
                            "Delete Doctor", "Back to Main Menu"});
            int ch = readInt();
            switch (ch) {
                case 1 -> addDoctor();
                case 2 -> viewAllDoctors();
                case 3 -> doctorsByDept();
                case 4 -> updateDoctorAvailability();
                case 5 -> deleteDoctor();
                case 0 -> { return; }
                default -> System.out.println(RED + "Invalid option!" + RESET);
            }
        }
    }

    private static void addDoctor() {
        System.out.println(GREEN + "\n── Add New Doctor ──" + RESET);
        viewAllDepartments();
        System.out.print("Full Name         : "); String name  = sc.nextLine().trim();
        System.out.print("Specialization    : "); String spec  = sc.nextLine().trim();
        System.out.print("Phone             : "); String phone = sc.nextLine().trim();
        System.out.print("Email             : "); String email = sc.nextLine().trim();
        System.out.print("Department ID     : "); int deptId   = readInt();

        String sql = "INSERT INTO doctors(name,specialization,phone,email,dept_id) VALUES(?,?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, name); ps.setString(2, spec);
            ps.setString(3, phone); ps.setString(4, email); ps.setInt(5, deptId);
            ps.executeUpdate();
            ResultSet gk = ps.getGeneratedKeys();
            System.out.println(GREEN + "✔  Doctor added! ID = " + gk.getInt(1) + RESET);
        } catch (SQLException e) { sqlError(e); }
    }

    private static void viewAllDoctors() {
        String sql = """
            SELECT d.*, dept.name AS dept_name
            FROM doctors d
            LEFT JOIN departments dept ON d.dept_id = dept.dept_id
            ORDER BY d.doctor_id
        """;
        System.out.println(BLUE + "\n" + "─".repeat(100));
        System.out.printf("%-5s %-22s %-20s %-13s %-22s %-15s %-5s%n",
                "ID","Name","Specialization","Phone","Email","Department","Avail");
        System.out.println("─".repeat(100) + RESET);
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                System.out.printf("%-5d %-22s %-20s %-13s %-22s %-15s %-5s%n",
                        rs.getInt("doctor_id"), rs.getString("name"),
                        rs.getString("specialization"), rs.getString("phone"),
                        rs.getString("email"), rs.getString("dept_name"),
                        rs.getInt("available") == 1 ? GREEN+"Yes"+RESET : RED+"No"+RESET);
            }
            System.out.println(BLUE + "─".repeat(100) + RESET);
        } catch (SQLException e) { sqlError(e); }
    }

    private static void doctorsByDept() {
        viewAllDepartments();
        System.out.print("Enter Department ID: "); int deptId = readInt();
        String sql = "SELECT * FROM doctors WHERE dept_id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, deptId);
            ResultSet rs = ps.executeQuery();
            System.out.println(BLUE + "\n── Doctors in Department ──" + RESET);
            while (rs.next()) {
                System.out.printf("  [%d] %-22s | %-20s | %s%n",
                        rs.getInt("doctor_id"), rs.getString("name"),
                        rs.getString("specialization"),
                        rs.getInt("available") == 1 ? GREEN+"Available"+RESET : RED+"Unavailable"+RESET);
            }
        } catch (SQLException e) { sqlError(e); }
    }

    private static void updateDoctorAvailability() {
        System.out.print("Enter Doctor ID       : "); int id = readInt();
        System.out.print("Set Available? (1=Yes/0=No): "); int avail = readInt();
        try (PreparedStatement ps = conn.prepareStatement("UPDATE doctors SET available=? WHERE doctor_id=?")) {
            ps.setInt(1, avail); ps.setInt(2, id);
            int rows = ps.executeUpdate();
            System.out.println(rows > 0 ? GREEN + "✔  Availability updated." + RESET : RED + "Doctor not found." + RESET);
        } catch (SQLException e) { sqlError(e); }
    }

    private static void deleteDoctor() {
        System.out.print("Enter Doctor ID to delete: "); int id = readInt();
        try (PreparedStatement ps = conn.prepareStatement("DELETE FROM doctors WHERE doctor_id=?")) {
            ps.setInt(1, id);
            int rows = ps.executeUpdate();
            System.out.println(rows > 0 ? GREEN + "✔  Doctor deleted." + RESET : RED + "Doctor not found." + RESET);
        } catch (SQLException e) { sqlError(e); }
    }

    // =========================================================================
    //  3. APPOINTMENT MANAGEMENT
    // =========================================================================
    private static void appointmentMenu() {
        while (true) {
            printSubMenu("APPOINTMENT MANAGEMENT",
                    new String[]{"Book Appointment", "View All Appointments",
                            "View by Patient ID", "View by Doctor ID",
                            "Update Status", "Cancel Appointment",
                            "Back to Main Menu"});
            int ch = readInt();
            switch (ch) {
                case 1 -> bookAppointment();
                case 2 -> viewAllAppointments();
                case 3 -> appointmentsByPatient();
                case 4 -> appointmentsByDoctor();
                case 5 -> updateAppointmentStatus();
                case 6 -> cancelAppointment();
                case 0 -> { return; }
                default -> System.out.println(RED + "Invalid option!" + RESET);
            }
        }
    }

    private static void bookAppointment() {
        System.out.println(GREEN + "\n── Book Appointment ──" + RESET);
        System.out.print("Patient ID   : "); int patId    = readInt();
        System.out.print("Doctor ID    : "); int docId    = readInt();
        System.out.print("Date (YYYY-MM-DD): "); String date = sc.nextLine().trim();
        System.out.print("Time (HH:MM) : "); String time  = sc.nextLine().trim();
        System.out.print("Reason       : "); String reason = sc.nextLine().trim();

        String sql = "INSERT INTO appointments(patient_id,doctor_id,appt_date,appt_time,reason) VALUES(?,?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, patId); ps.setInt(2, docId);
            ps.setString(3, date); ps.setString(4, time); ps.setString(5, reason);
            ps.executeUpdate();
            ResultSet gk = ps.getGeneratedKeys();
            System.out.println(GREEN + "✔  Appointment booked! ID = " + gk.getInt(1) + RESET);
        } catch (SQLException e) { sqlError(e); }
    }

    private static void viewAllAppointments() {
        String sql = """
            SELECT a.*, p.name AS patient_name, d.name AS doctor_name
            FROM appointments a
            JOIN patients p ON a.patient_id = p.patient_id
            JOIN doctors  d ON a.doctor_id  = d.doctor_id
            ORDER BY a.appt_date, a.appt_time
        """;
        printAppointmentTable(sql, new Object[]{});
    }

    private static void appointmentsByPatient() {
        System.out.print("Enter Patient ID: "); int id = readInt();
        String sql = """
            SELECT a.*, p.name AS patient_name, d.name AS doctor_name
            FROM appointments a
            JOIN patients p ON a.patient_id = p.patient_id
            JOIN doctors  d ON a.doctor_id  = d.doctor_id
            WHERE a.patient_id = ?
            ORDER BY a.appt_date
        """;
        printAppointmentTable(sql, new Object[]{id});
    }

    private static void appointmentsByDoctor() {
        System.out.print("Enter Doctor ID: "); int id = readInt();
        String sql = """
            SELECT a.*, p.name AS patient_name, d.name AS doctor_name
            FROM appointments a
            JOIN patients p ON a.patient_id = p.patient_id
            JOIN doctors  d ON a.doctor_id  = d.doctor_id
            WHERE a.doctor_id = ?
            ORDER BY a.appt_date
        """;
        printAppointmentTable(sql, new Object[]{id});
    }

    private static void printAppointmentTable(String sql, Object[] params) {
        System.out.println(YELLOW + "\n" + "─".repeat(105));
        System.out.printf("%-6s %-20s %-22s %-12s %-7s %-22s %-12s%n",
                "ID","Patient","Doctor","Date","Time","Reason","Status");
        System.out.println("─".repeat(105) + RESET);
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                if (params[i] instanceof Integer) ps.setInt(i+1, (Integer) params[i]);
                else ps.setString(i+1, params[i].toString());
            }
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String status = rs.getString("status");
                String colored = switch (status) {
                    case "Scheduled"  -> CYAN   + status + RESET;
                    case "Completed"  -> GREEN  + status + RESET;
                    case "Cancelled"  -> RED    + status + RESET;
                    default           -> status;
                };
                System.out.printf("%-6d %-20s %-22s %-12s %-7s %-22s %-12s%n",
                        rs.getInt("appt_id"),
                        rs.getString("patient_name"), rs.getString("doctor_name"),
                        rs.getString("appt_date"), rs.getString("appt_time"),
                        truncate(rs.getString("reason"), 20), colored);
            }
            System.out.println(YELLOW + "─".repeat(105) + RESET);
        } catch (SQLException e) { sqlError(e); }
    }

    private static void updateAppointmentStatus() {
        System.out.print("Enter Appointment ID: "); int id = readInt();
        System.out.println("Status: 1=Scheduled  2=Completed  3=Cancelled");
        System.out.print("Choose: "); int opt = readInt();
        String status = switch (opt) { case 1->"Scheduled"; case 2->"Completed"; case 3->"Cancelled"; default->"Scheduled"; };
        try (PreparedStatement ps = conn.prepareStatement("UPDATE appointments SET status=? WHERE appt_id=?")) {
            ps.setString(1, status); ps.setInt(2, id);
            int rows = ps.executeUpdate();
            System.out.println(rows > 0 ? GREEN + "✔  Status updated to " + status + RESET : RED + "Appointment not found." + RESET);
        } catch (SQLException e) { sqlError(e); }
    }

    private static void cancelAppointment() {
        System.out.print("Enter Appointment ID to cancel: "); int id = readInt();
        try (PreparedStatement ps = conn.prepareStatement("UPDATE appointments SET status='Cancelled' WHERE appt_id=?")) {
            ps.setInt(1, id);
            int rows = ps.executeUpdate();
            System.out.println(rows > 0 ? GREEN + "✔  Appointment cancelled." + RESET : RED + "Not found." + RESET);
        } catch (SQLException e) { sqlError(e); }
    }

    // =========================================================================
    //  4. MEDICAL RECORDS
    // =========================================================================
    private static void medicalRecordMenu() {
        while (true) {
            printSubMenu("MEDICAL RECORDS",
                    new String[]{"Add Medical Record", "View All Records",
                            "View Records by Patient", "View Records by Doctor",
                            "Delete Record", "Back to Main Menu"});
            int ch = readInt();
            switch (ch) {
                case 1 -> addMedicalRecord();
                case 2 -> viewAllRecords();
                case 3 -> recordsByPatient();
                case 4 -> recordsByDoctor();
                case 5 -> deleteRecord();
                case 0 -> { return; }
                default -> System.out.println(RED + "Invalid option!" + RESET);
            }
        }
    }

    private static void addMedicalRecord() {
        System.out.println(GREEN + "\n── Add Medical Record ──" + RESET);
        System.out.print("Patient ID    : "); int patId     = readInt();
        System.out.print("Doctor ID     : "); int docId     = readInt();
        System.out.print("Visit Date (YYYY-MM-DD): "); String date = sc.nextLine().trim();
        System.out.print("Diagnosis     : "); String diag   = sc.nextLine().trim();
        System.out.print("Prescription  : "); String presc  = sc.nextLine().trim();
        System.out.print("Notes         : "); String notes  = sc.nextLine().trim();

        String sql = "INSERT INTO medical_records(patient_id,doctor_id,visit_date,diagnosis,prescription,notes) VALUES(?,?,?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, patId); ps.setInt(2, docId); ps.setString(3, date);
            ps.setString(4, diag); ps.setString(5, presc); ps.setString(6, notes);
            ps.executeUpdate();
            ResultSet gk = ps.getGeneratedKeys();
            System.out.println(GREEN + "✔  Record added! ID = " + gk.getInt(1) + RESET);
        } catch (SQLException e) { sqlError(e); }
    }

    private static void viewAllRecords() {
        String sql = """
            SELECT mr.*, p.name AS pname, d.name AS dname
            FROM medical_records mr
            JOIN patients p ON mr.patient_id=p.patient_id
            JOIN doctors  d ON mr.doctor_id =d.doctor_id
            ORDER BY mr.visit_date DESC
        """;
        printRecordsTable(sql, new Object[]{});
    }

    private static void recordsByPatient() {
        System.out.print("Patient ID: "); int id = readInt();
        String sql = """
            SELECT mr.*, p.name AS pname, d.name AS dname
            FROM medical_records mr
            JOIN patients p ON mr.patient_id=p.patient_id
            JOIN doctors  d ON mr.doctor_id =d.doctor_id
            WHERE mr.patient_id=?
            ORDER BY mr.visit_date DESC
        """;
        printRecordsTable(sql, new Object[]{id});
    }

    private static void recordsByDoctor() {
        System.out.print("Doctor ID: "); int id = readInt();
        String sql = """
            SELECT mr.*, p.name AS pname, d.name AS dname
            FROM medical_records mr
            JOIN patients p ON mr.patient_id=p.patient_id
            JOIN doctors  d ON mr.doctor_id =d.doctor_id
            WHERE mr.doctor_id=?
            ORDER BY mr.visit_date DESC
        """;
        printRecordsTable(sql, new Object[]{id});
    }

    private static void printRecordsTable(String sql, Object[] params) {
        System.out.println(PURPLE + "\n" + "─".repeat(115));
        System.out.printf("%-6s %-18s %-18s %-12s %-22s %-22s %-15s%n",
                "RecID","Patient","Doctor","Visit Date","Diagnosis","Prescription","Notes");
        System.out.println("─".repeat(115) + RESET);
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                if (params[i] instanceof Integer) ps.setInt(i+1, (Integer) params[i]);
                else ps.setString(i+1, params[i].toString());
            }
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                System.out.printf("%-6d %-18s %-18s %-12s %-22s %-22s %-15s%n",
                        rs.getInt("record_id"), rs.getString("pname"), rs.getString("dname"),
                        rs.getString("visit_date"), truncate(rs.getString("diagnosis"),20),
                        truncate(rs.getString("prescription"),20), truncate(rs.getString("notes"),13));
            }
            System.out.println(PURPLE + "─".repeat(115) + RESET);
        } catch (SQLException e) { sqlError(e); }
    }

    private static void deleteRecord() {
        System.out.print("Enter Record ID to delete: "); int id = readInt();
        try (PreparedStatement ps = conn.prepareStatement("DELETE FROM medical_records WHERE record_id=?")) {
            ps.setInt(1, id);
            int rows = ps.executeUpdate();
            System.out.println(rows > 0 ? GREEN + "✔  Record deleted." + RESET : RED + "Not found." + RESET);
        } catch (SQLException e) { sqlError(e); }
    }

    // =========================================================================
    //  5. BILLING & PAYMENTS
    // =========================================================================
    private static void billingMenu() {
        while (true) {
            printSubMenu("BILLING & PAYMENTS",
                    new String[]{"Create Bill", "View All Bills",
                            "View Bills by Patient", "Record Payment",
                            "View Unpaid Bills", "Delete Bill",
                            "Back to Main Menu"});
            int ch = readInt();
            switch (ch) {
                case 1 -> createBill();
                case 2 -> viewAllBills();
                case 3 -> billsByPatient();
                case 4 -> recordPayment();
                case 5 -> viewUnpaidBills();
                case 6 -> deleteBill();
                case 0 -> { return; }
                default -> System.out.println(RED + "Invalid option!" + RESET);
            }
        }
    }

    private static void createBill() {
        System.out.println(GREEN + "\n── Create Bill ──" + RESET);
        System.out.print("Patient ID    : "); int patId      = readInt();
        System.out.print("Total Amount  : "); double total   = readDouble();
        System.out.print("Description   : "); String desc    = sc.nextLine().trim();

        String sql = "INSERT INTO billing(patient_id,total_amount,description) VALUES(?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, patId); ps.setDouble(2, total); ps.setString(3, desc);
            ps.executeUpdate();
            ResultSet gk = ps.getGeneratedKeys();
            System.out.println(GREEN + "✔  Bill created! ID = " + gk.getInt(1) + RESET);
        } catch (SQLException e) { sqlError(e); }
    }

    private static void viewAllBills() {
        String sql = """
            SELECT b.*, p.name AS pname FROM billing b
            JOIN patients p ON b.patient_id=p.patient_id
            ORDER BY b.bill_date DESC
        """;
        printBillTable(sql, new Object[]{});
    }

    private static void billsByPatient() {
        System.out.print("Patient ID: "); int id = readInt();
        String sql = """
            SELECT b.*, p.name AS pname FROM billing b
            JOIN patients p ON b.patient_id=p.patient_id
            WHERE b.patient_id=?
            ORDER BY b.bill_date DESC
        """;
        printBillTable(sql, new Object[]{id});
    }

    private static void printBillTable(String sql, Object[] params) {
        System.out.println(GREEN + "\n" + "─".repeat(100));
        System.out.printf("%-7s %-20s %-12s %-10s %-10s %-10s %-8s %-20s%n",
                "BillID","Patient","Date","Total","Paid","Balance","Status","Description");
        System.out.println("─".repeat(100) + RESET);
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                if (params[i] instanceof Integer) ps.setInt(i+1, (Integer) params[i]);
                else ps.setString(i+1, params[i].toString());
            }
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                double total = rs.getDouble("total_amount");
                double paid  = rs.getDouble("paid_amount");
                double bal   = total - paid;
                String st    = rs.getString("status");
                String coloredStatus = st.equals("Paid") ? GREEN + st + RESET : RED + st + RESET;
                System.out.printf("%-7d %-20s %-12s %-10.2f %-10.2f %-10.2f %-8s %-20s%n",
                        rs.getInt("bill_id"), rs.getString("pname"),
                        rs.getString("bill_date"), total, paid, bal,
                        coloredStatus, truncate(rs.getString("description"), 18));
            }
            System.out.println(GREEN + "─".repeat(100) + RESET);
        } catch (SQLException e) { sqlError(e); }
    }

    private static void recordPayment() {
        System.out.print("Bill ID          : "); int billId    = readInt();
        System.out.print("Payment Amount   : "); double amount = readDouble();

        String sql = """
            UPDATE billing
            SET paid_amount = paid_amount + ?,
                status = CASE WHEN paid_amount + ? >= total_amount THEN 'Paid' ELSE 'Partial' END
            WHERE bill_id = ?
        """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDouble(1, amount); ps.setDouble(2, amount); ps.setInt(3, billId);
            int rows = ps.executeUpdate();
            System.out.println(rows > 0 ? GREEN + "✔  Payment recorded." + RESET : RED + "Bill not found." + RESET);
        } catch (SQLException e) { sqlError(e); }
    }

    private static void viewUnpaidBills() {
        String sql = """
            SELECT b.*, p.name AS pname FROM billing b
            JOIN patients p ON b.patient_id=p.patient_id
            WHERE b.status != 'Paid'
            ORDER BY b.bill_date
        """;
        System.out.println(RED + "\n── Unpaid / Partial Bills ──" + RESET);
        printBillTable(sql, new Object[]{});
    }

    private static void deleteBill() {
        System.out.print("Bill ID to delete: "); int id = readInt();
        try (PreparedStatement ps = conn.prepareStatement("DELETE FROM billing WHERE bill_id=?")) {
            ps.setInt(1, id);
            int rows = ps.executeUpdate();
            System.out.println(rows > 0 ? GREEN + "✔  Bill deleted." + RESET : RED + "Not found." + RESET);
        } catch (SQLException e) { sqlError(e); }
    }

    // =========================================================================
    //  6. DEPARTMENT MANAGEMENT
    // =========================================================================
    private static void departmentMenu() {
        while (true) {
            printSubMenu("DEPARTMENT MANAGEMENT",
                    new String[]{"Add Department", "View All Departments",
                            "Update Department", "Delete Department",
                            "Back to Main Menu"});
            int ch = readInt();
            switch (ch) {
                case 1 -> addDepartment();
                case 2 -> viewAllDepartments();
                case 3 -> updateDepartment();
                case 4 -> deleteDepartment();
                case 0 -> { return; }
                default -> System.out.println(RED + "Invalid option!" + RESET);
            }
        }
    }

    private static void addDepartment() {
        System.out.print("Department Name : "); String name = sc.nextLine().trim();
        System.out.print("Location        : "); String loc  = sc.nextLine().trim();
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO departments(name,location) VALUES(?,?)", Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, name); ps.setString(2, loc);
            ps.executeUpdate();
            System.out.println(GREEN + "✔  Department added! ID = " + ps.getGeneratedKeys().getInt(1) + RESET);
        } catch (SQLException e) { sqlError(e); }
    }

    private static void viewAllDepartments() {
        System.out.println(BLUE + "\n── Departments ──" + RESET);
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM departments ORDER BY dept_id")) {
            System.out.printf("  %-5s %-22s %-15s%n","ID","Name","Location");
            System.out.println("  " + "─".repeat(44));
            while (rs.next()) {
                System.out.printf("  %-5d %-22s %-15s%n",
                        rs.getInt("dept_id"), rs.getString("name"), rs.getString("location"));
            }
        } catch (SQLException e) { sqlError(e); }
    }

    private static void updateDepartment() {
        System.out.print("Dept ID to update: "); int id = readInt();
        System.out.print("New Name         : "); String name = sc.nextLine().trim();
        System.out.print("New Location     : "); String loc  = sc.nextLine().trim();
        try (PreparedStatement ps = conn.prepareStatement("UPDATE departments SET name=?,location=? WHERE dept_id=?")) {
            ps.setString(1, name); ps.setString(2, loc); ps.setInt(3, id);
            int rows = ps.executeUpdate();
            System.out.println(rows > 0 ? GREEN + "✔  Updated." + RESET : RED + "Not found." + RESET);
        } catch (SQLException e) { sqlError(e); }
    }

    private static void deleteDepartment() {
        System.out.print("Dept ID to delete: "); int id = readInt();
        try (PreparedStatement ps = conn.prepareStatement("DELETE FROM departments WHERE dept_id=?")) {
            ps.setInt(1, id);
            int rows = ps.executeUpdate();
            System.out.println(rows > 0 ? GREEN + "✔  Deleted." + RESET : RED + "Not found." + RESET);
        } catch (SQLException e) { sqlError(e); }
    }

    // =========================================================================
    //  7. REPORTS & SEARCH
    // =========================================================================
    private static void reportsMenu() {
        while (true) {
            printSubMenu("REPORTS & STATISTICS",
                    new String[]{"Patient Count per Blood Group",
                            "Doctor Count per Department",
                            "Today's Appointments",
                            "Revenue Summary (Total / Collected / Pending)",
                            "Top 5 Most-Visited Patients",
                            "Available Doctors",
                            "Back to Main Menu"});
            int ch = readInt();
            switch (ch) {
                case 1 -> reportBloodGroups();
                case 2 -> reportDeptDoctors();
                case 3 -> reportTodayAppointments();
                case 4 -> reportRevenue();
                case 5 -> reportTopPatients();
                case 6 -> reportAvailableDoctors();
                case 0 -> { return; }
                default -> System.out.println(RED + "Invalid option!" + RESET);
            }
        }
    }

    private static void reportBloodGroups() {
        String sql = "SELECT blood_group, COUNT(*) AS cnt FROM patients GROUP BY blood_group ORDER BY cnt DESC";
        System.out.println(CYAN + "\n── Patient Count by Blood Group ──" + RESET);
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            System.out.printf("  %-12s %-8s%n","Blood Group","Count");
            System.out.println("  " + "─".repeat(22));
            while (rs.next())
                System.out.printf("  %-12s %-8d%n", rs.getString("blood_group"), rs.getInt("cnt"));
        } catch (SQLException e) { sqlError(e); }
    }

    private static void reportDeptDoctors() {
        String sql = """
            SELECT dept.name, COUNT(d.doctor_id) AS cnt
            FROM departments dept
            LEFT JOIN doctors d ON dept.dept_id=d.dept_id
            GROUP BY dept.dept_id ORDER BY cnt DESC
        """;
        System.out.println(CYAN + "\n── Doctors per Department ──" + RESET);
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            System.out.printf("  %-24s %-8s%n","Department","Doctors");
            System.out.println("  " + "─".repeat(34));
            while (rs.next())
                System.out.printf("  %-24s %-8d%n", rs.getString("name"), rs.getInt("cnt"));
        } catch (SQLException e) { sqlError(e); }
    }

    private static void reportTodayAppointments() {
        String sql = """
            SELECT a.*, p.name AS pname, d.name AS dname
            FROM appointments a
            JOIN patients p ON a.patient_id=p.patient_id
            JOIN doctors  d ON a.doctor_id =d.doctor_id
            WHERE a.appt_date = DATE('now')
            ORDER BY a.appt_time
        """;
        System.out.println(CYAN + "\n── Today's Appointments ──" + RESET);
        printAppointmentTable(sql, new Object[]{});
    }

    private static void reportRevenue() {
        String sql = "SELECT SUM(total_amount) AS total, SUM(paid_amount) AS paid FROM billing";
        System.out.println(CYAN + "\n── Revenue Summary ──" + RESET);
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) {
                double total   = rs.getDouble("total");
                double paid    = rs.getDouble("paid");
                double pending = total - paid;
                System.out.printf("  Total Billed   : %s₹ %.2f%s%n", BOLD, total,   RESET);
                System.out.printf("  Amount Paid    : %s₹ %.2f%s%n", GREEN, paid,   RESET);
                System.out.printf("  Amount Pending : %s₹ %.2f%s%n", RED,   pending, RESET);
            }
        } catch (SQLException e) { sqlError(e); }
    }

    private static void reportTopPatients() {
        String sql = """
            SELECT p.name, COUNT(mr.record_id) AS visits
            FROM patients p
            LEFT JOIN medical_records mr ON p.patient_id=mr.patient_id
            GROUP BY p.patient_id ORDER BY visits DESC LIMIT 5
        """;
        System.out.println(CYAN + "\n── Top 5 Most-Visited Patients ──" + RESET);
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            int rank = 1;
            System.out.printf("  %-4s %-24s %-8s%n","#","Patient","Visits");
            System.out.println("  " + "─".repeat(38));
            while (rs.next())
                System.out.printf("  %-4d %-24s %-8d%n", rank++, rs.getString("name"), rs.getInt("visits"));
        } catch (SQLException e) { sqlError(e); }
    }

    private static void reportAvailableDoctors() {
        String sql = """
            SELECT d.*, dept.name AS dept_name
            FROM doctors d
            LEFT JOIN departments dept ON d.dept_id=dept.dept_id
            WHERE d.available=1
            ORDER BY dept.name
        """;
        System.out.println(CYAN + "\n── Available Doctors ──" + RESET);
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            System.out.printf("  %-5s %-22s %-22s %-15s%n","ID","Name","Specialization","Department");
            System.out.println("  " + "─".repeat(68));
            while (rs.next())
                System.out.printf("  %-5d %-22s %-22s %-15s%n",
                        rs.getInt("doctor_id"), rs.getString("name"),
                        rs.getString("specialization"), rs.getString("dept_name"));
        } catch (SQLException e) { sqlError(e); }
    }

    // =========================================================================
    //  UTILITY HELPERS
    // =========================================================================
    private static void printSubMenu(String title, String[] options) {
        System.out.println(BOLD + YELLOW + "\n╔══════════════════════════════════╗");
        System.out.printf("║  %-32s║%n", title);
        System.out.println("╠══════════════════════════════════╣" + RESET);
        for (int i = 0; i < options.length; i++) {
            int label = (i == options.length - 1) ? 0 : i + 1;
            System.out.printf(YELLOW + "║  %d. %-30s║%n" + RESET, label, options[i]);
        }
        System.out.println(YELLOW + "╚══════════════════════════════════╝" + RESET);
        System.out.print(BOLD + "  ➤  Choose option: " + RESET);
    }

    private static int readInt() {
        try {
            String line = sc.nextLine().trim();
            return Integer.parseInt(line);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private static double readDouble() {
        try {
            String line = sc.nextLine().trim();
            return Double.parseDouble(line);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    private static void sqlError(SQLException e) {
        System.out.println(RED + "Database error: " + e.getMessage() + RESET);
    }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max - 2) + "..";
    }
}