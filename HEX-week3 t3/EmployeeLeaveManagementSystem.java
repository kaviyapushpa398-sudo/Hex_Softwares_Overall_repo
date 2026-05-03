import java.util.*;
import java.time.*;
import java.time.format.*;
import java.time.temporal.ChronoUnit;

// ============================================================
//         EMPLOYEE LEAVE MANAGEMENT SYSTEM
//         Single-File Java Application
// ============================================================

public class EmployeeLeaveManagementSystem {

    // ─────────────────────────────────────────────
    //  ENUMS
    // ─────────────────────────────────────────────

    enum LeaveType {
        ANNUAL("Annual Leave", 20),
        SICK("Sick Leave", 10),
        CASUAL("Casual Leave", 7),
        MATERNITY("Maternity Leave", 90),
        PATERNITY("Paternity Leave", 15),
        UNPAID("Unpaid Leave", 0);

        private final String displayName;
        private final int defaultDays;

        LeaveType(String displayName, int defaultDays) {
            this.displayName = displayName;
            this.defaultDays = defaultDays;
        }

        public String getDisplayName() { return displayName; }
        public int getDefaultDays()    { return defaultDays; }
    }

    enum LeaveStatus { PENDING, APPROVED, REJECTED, CANCELLED }

    enum Role { EMPLOYEE, MANAGER, ADMIN }

    // ─────────────────────────────────────────────
    //  MODELS
    // ─────────────────────────────────────────────

    static class Employee {
        private static int counter = 1000;
        private final String id;
        private String name;
        private String email;
        private String department;
        private Role role;
        private String managerId;
        private final Map<LeaveType, Integer> leaveBalances = new HashMap<>();
        private final LocalDate joinDate;

        public Employee(String name, String email, String department, Role role, String managerId) {
            this.id        = "EMP" + (++counter);
            this.name      = name;
            this.email     = email;
            this.department = department;
            this.role      = role;
            this.managerId = managerId;
            this.joinDate  = LocalDate.now();
            initBalances();
        }

        private void initBalances() {
            for (LeaveType lt : LeaveType.values()) {
                leaveBalances.put(lt, lt.getDefaultDays());
            }
        }

        public String  getId()         { return id; }
        public String  getName()       { return name; }
        public String  getEmail()      { return email; }
        public String  getDepartment() { return department; }
        public Role    getRole()       { return role; }
        public String  getManagerId()  { return managerId; }
        public LocalDate getJoinDate() { return joinDate; }

        public int  getBalance(LeaveType lt)           { return leaveBalances.getOrDefault(lt, 0); }
        public void deductBalance(LeaveType lt, int d) { leaveBalances.merge(lt, -d, Integer::sum); }
        public void addBalance(LeaveType lt, int d)    { leaveBalances.merge(lt, d, Integer::sum); }
        public Map<LeaveType, Integer> getAllBalances() { return Collections.unmodifiableMap(leaveBalances); }

        public void setName(String n)       { this.name = n; }
        public void setEmail(String e)      { this.email = e; }
        public void setDepartment(String d) { this.department = d; }
        public void setRole(Role r)         { this.role = r; }
        public void setManagerId(String m)  { this.managerId = m; }
    }

    static class LeaveRequest {
        private static int reqCounter = 5000;
        private final String requestId;
        private final String employeeId;
        private final LeaveType leaveType;
        private final LocalDate startDate;
        private final LocalDate endDate;
        private final int totalDays;
        private final String reason;
        private LeaveStatus status;
        private String reviewedBy;
        private String reviewComment;
        private final LocalDateTime appliedOn;
        private LocalDateTime reviewedOn;

        public LeaveRequest(String employeeId, LeaveType leaveType,
                            LocalDate startDate, LocalDate endDate, String reason) {
            this.requestId  = "LR" + (++reqCounter);
            this.employeeId = employeeId;
            this.leaveType  = leaveType;
            this.startDate  = startDate;
            this.endDate    = endDate;
            this.totalDays  = (int) ChronoUnit.DAYS.between(startDate, endDate) + 1;
            this.reason     = reason;
            this.status     = LeaveStatus.PENDING;
            this.appliedOn  = LocalDateTime.now();
        }

        public String      getRequestId()    { return requestId; }
        public String      getEmployeeId()   { return employeeId; }
        public LeaveType   getLeaveType()    { return leaveType; }
        public LocalDate   getStartDate()    { return startDate; }
        public LocalDate   getEndDate()      { return endDate; }
        public int         getTotalDays()    { return totalDays; }
        public String      getReason()       { return reason; }
        public LeaveStatus getStatus()       { return status; }
        public String      getReviewedBy()   { return reviewedBy; }
        public String      getReviewComment(){ return reviewComment; }
        public LocalDateTime getAppliedOn()  { return appliedOn; }
        public LocalDateTime getReviewedOn() { return reviewedOn; }

        public void approve(String managerId, String comment) {
            this.status        = LeaveStatus.APPROVED;
            this.reviewedBy    = managerId;
            this.reviewComment = comment;
            this.reviewedOn    = LocalDateTime.now();
        }

        public void reject(String managerId, String comment) {
            this.status        = LeaveStatus.REJECTED;
            this.reviewedBy    = managerId;
            this.reviewComment = comment;
            this.reviewedOn    = LocalDateTime.now();
        }

        public void cancel() {
            this.status     = LeaveStatus.CANCELLED;
            this.reviewedOn = LocalDateTime.now();
        }
    }

    // ─────────────────────────────────────────────
    //  SERVICE LAYER
    // ─────────────────────────────────────────────

    static class LeaveService {
        private final Map<String, Employee>     employees     = new LinkedHashMap<>();
        private final Map<String, LeaveRequest> leaveRequests = new LinkedHashMap<>();

        // ── Employee management ──────────────────

        public Employee addEmployee(String name, String email, String dept, Role role, String managerId) {
            Employee emp = new Employee(name, email, dept, role, managerId);
            employees.put(emp.getId(), emp);
            return emp;
        }

        public Optional<Employee> findEmployee(String id) {
            return Optional.ofNullable(employees.get(id));
        }

        public List<Employee> getAllEmployees() {
            return new ArrayList<>(employees.values());
        }

        public List<Employee> getSubordinates(String managerId) {
            List<Employee> subs = new ArrayList<>();
            for (Employee e : employees.values()) {
                if (managerId.equals(e.getManagerId())) subs.add(e);
            }
            return subs;
        }

        public boolean updateEmployee(String id, String name, String email, String dept) {
            Employee emp = employees.get(id);
            if (emp == null) return false;
            emp.setName(name);
            emp.setEmail(email);
            emp.setDepartment(dept);
            return true;
        }

        public boolean removeEmployee(String id) {
            return employees.remove(id) != null;
        }

        // ── Leave requests ───────────────────────

        public Result<LeaveRequest> applyLeave(String employeeId, LeaveType leaveType,
                                               LocalDate start, LocalDate end, String reason) {
            Employee emp = employees.get(employeeId);
            if (emp == null) return Result.fail("Employee not found.");

            if (end.isBefore(start))
                return Result.fail("End date cannot be before start date.");

            int days = (int) ChronoUnit.DAYS.between(start, end) + 1;

            if (leaveType != LeaveType.UNPAID && emp.getBalance(leaveType) < days)
                return Result.fail("Insufficient leave balance. Available: "
                        + emp.getBalance(leaveType) + " day(s), Requested: " + days + " day(s).");

            if (hasOverlap(employeeId, start, end))
                return Result.fail("Leave overlaps with an existing approved/pending request.");

            LeaveRequest req = new LeaveRequest(employeeId, leaveType, start, end, reason);
            leaveRequests.put(req.getRequestId(), req);
            return Result.ok(req);
        }

        public Result<String> approveLeave(String requestId, String managerId, String comment) {
            LeaveRequest req = leaveRequests.get(requestId);
            if (req == null) return Result.fail("Leave request not found.");
            if (req.getStatus() != LeaveStatus.PENDING) return Result.fail("Only PENDING requests can be approved.");

            Employee manager = employees.get(managerId);
            if (manager == null) return Result.fail("Manager not found.");
            if (manager.getRole() != Role.MANAGER && manager.getRole() != Role.ADMIN)
                return Result.fail("Only managers or admins can approve leaves.");

            Employee emp = employees.get(req.getEmployeeId());
            if (emp == null) return Result.fail("Employee not found.");

            if (manager.getRole() == Role.MANAGER && !managerId.equals(emp.getManagerId()))
                return Result.fail("You are not the direct manager of this employee.");

            req.approve(managerId, comment);
            if (req.getLeaveType() != LeaveType.UNPAID) {
                emp.deductBalance(req.getLeaveType(), req.getTotalDays());
            }
            return Result.ok("Leave approved successfully.");
        }

        public Result<String> rejectLeave(String requestId, String managerId, String comment) {
            LeaveRequest req = leaveRequests.get(requestId);
            if (req == null) return Result.fail("Leave request not found.");
            if (req.getStatus() != LeaveStatus.PENDING) return Result.fail("Only PENDING requests can be rejected.");

            Employee manager = employees.get(managerId);
            if (manager == null) return Result.fail("Manager not found.");
            if (manager.getRole() != Role.MANAGER && manager.getRole() != Role.ADMIN)
                return Result.fail("Only managers or admins can reject leaves.");

            req.reject(managerId, comment);
            return Result.ok("Leave rejected.");
        }

        public Result<String> cancelLeave(String requestId, String employeeId) {
            LeaveRequest req = leaveRequests.get(requestId);
            if (req == null) return Result.fail("Leave request not found.");
            if (!req.getEmployeeId().equals(employeeId)) return Result.fail("You can only cancel your own leaves.");

            if (req.getStatus() == LeaveStatus.APPROVED) {
                Employee emp = employees.get(employeeId);
                if (emp != null && req.getLeaveType() != LeaveType.UNPAID) {
                    emp.addBalance(req.getLeaveType(), req.getTotalDays());
                }
            } else if (req.getStatus() != LeaveStatus.PENDING) {
                return Result.fail("Only PENDING or APPROVED leaves can be cancelled.");
            }

            req.cancel();
            return Result.ok("Leave cancelled. Balance restored (if applicable).");
        }

        public List<LeaveRequest> getEmployeeLeaves(String employeeId) {
            List<LeaveRequest> list = new ArrayList<>();
            for (LeaveRequest r : leaveRequests.values()) {
                if (r.getEmployeeId().equals(employeeId)) list.add(r);
            }
            return list;
        }

        public List<LeaveRequest> getPendingForManager(String managerId) {
            List<LeaveRequest> list = new ArrayList<>();
            Set<String> subIds = new HashSet<>();
            for (Employee e : getSubordinates(managerId)) subIds.add(e.getId());
            for (LeaveRequest r : leaveRequests.values()) {
                if (r.getStatus() == LeaveStatus.PENDING && subIds.contains(r.getEmployeeId()))
                    list.add(r);
            }
            return list;
        }

        public List<LeaveRequest> getAllLeaveRequests() {
            return new ArrayList<>(leaveRequests.values());
        }

        public Optional<LeaveRequest> findRequest(String id) {
            return Optional.ofNullable(leaveRequests.get(id));
        }

        // ── Helpers ──────────────────────────────

        private boolean hasOverlap(String empId, LocalDate start, LocalDate end) {
            for (LeaveRequest r : leaveRequests.values()) {
                if (!r.getEmployeeId().equals(empId)) continue;
                if (r.getStatus() == LeaveStatus.REJECTED || r.getStatus() == LeaveStatus.CANCELLED) continue;
                if (!start.isAfter(r.getEndDate()) && !end.isBefore(r.getStartDate())) return true;
            }
            return false;
        }

        // ── Adjust balance (admin) ───────────────

        public Result<String> adjustBalance(String employeeId, LeaveType lt, int days) {
            Employee emp = employees.get(employeeId);
            if (emp == null) return Result.fail("Employee not found.");
            emp.addBalance(lt, days);
            return Result.ok("Balance updated. New balance for " + lt.getDisplayName()
                    + ": " + emp.getBalance(lt) + " day(s).");
        }
    }

    // ─────────────────────────────────────────────
    //  RESULT WRAPPER
    // ─────────────────────────────────────────────

    static class Result<T> {
        private final T value;
        private final String error;

        private Result(T value, String error) { this.value = value; this.error = error; }

        public static <T> Result<T> ok(T v)     { return new Result<>(v, null); }
        public static <T> Result<T> fail(String e) { return new Result<>(null, e); }

        public boolean isSuccess() { return error == null; }
        public T       getValue()  { return value; }
        public String  getError()  { return error; }
    }

    // ─────────────────────────────────────────────
    //  UI / CONSOLE LAYER
    // ─────────────────────────────────────────────

    static class ConsoleUI {
        private final Scanner      sc      = new Scanner(System.in);
        private final LeaveService service = new LeaveService();
        private Employee           loggedIn;

        private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        private static final String LINE   = "─".repeat(60);
        private static final String DLINE  = "═".repeat(60);

        // ── Boot ─────────────────────────────────

        public void start() {
            seedData();
            banner();
            mainLoop();
        }

        private void seedData() {
            // Admin
            Employee admin = service.addEmployee("Alice Admin", "alice@corp.com", "HR", Role.ADMIN, null);
            // Managers
            Employee mgr1  = service.addEmployee("Bob Manager", "bob@corp.com", "Engineering", Role.MANAGER, admin.getId());
            Employee mgr2  = service.addEmployee("Carol Manager", "carol@corp.com", "Sales", Role.MANAGER, admin.getId());
            // Employees under mgr1
            service.addEmployee("Dave Dev",   "dave@corp.com",  "Engineering", Role.EMPLOYEE, mgr1.getId());
            service.addEmployee("Eva Eng",    "eva@corp.com",   "Engineering", Role.EMPLOYEE, mgr1.getId());
            // Employees under mgr2
            service.addEmployee("Frank Sales","frank@corp.com", "Sales",       Role.EMPLOYEE, mgr2.getId());
        }

        private void banner() {
            System.out.println("\n" + DLINE);
            System.out.println("   ██╗     ███████╗ █████╗ ██╗   ██╗███████╗");
            System.out.println("   ██║     ██╔════╝██╔══██╗██║   ██║██╔════╝");
            System.out.println("   ██║     █████╗  ███████║██║   ██║█████╗  ");
            System.out.println("   ██║     ██╔══╝  ██╔══██║╚██╗ ██╔╝██╔══╝  ");
            System.out.println("   ███████╗███████╗██║  ██║ ╚████╔╝ ███████╗");
            System.out.println("   ╚══════╝╚══════╝╚═╝  ╚═╝  ╚═══╝  ╚══════╝");
            System.out.println("       Employee Leave Management System");
            System.out.println(DLINE);
            System.out.println("  Sample accounts (enter Employee ID to login):");
            printEmployeeList();
            System.out.println(DLINE + "\n");
        }

        private void printEmployeeList() {
            System.out.printf("  %-10s %-20s %-12s %-12s%n","ID","Name","Role","Department");
            System.out.println("  " + LINE);
            for (Employee e : service.getAllEmployees()) {
                System.out.printf("  %-10s %-20s %-12s %-12s%n",
                        e.getId(), e.getName(), e.getRole(), e.getDepartment());
            }
        }

        // ── Main Loop ────────────────────────────

        private void mainLoop() {
            while (true) {
                if (loggedIn == null) {
                    loginMenu();
                } else {
                    switch (loggedIn.getRole()) {
                        case EMPLOYEE -> employeeMenu();
                        case MANAGER  -> managerMenu();
                        case ADMIN    -> adminMenu();
                    }
                }
            }
        }

        // ── Login ────────────────────────────────

        private void loginMenu() {
            System.out.println("\n" + LINE);
            System.out.println("  LOGIN");
            System.out.println(LINE);
            System.out.print("  Enter Employee ID (or 'exit'): ");
            String input = sc.nextLine().trim();
            if (input.equalsIgnoreCase("exit")) {
                System.out.println("\n  Goodbye! 👋\n");
                System.exit(0);
            }
            Optional<Employee> emp = service.findEmployee(input.toUpperCase());
            if (emp.isPresent()) {
                loggedIn = emp.get();
                success("  ✓ Welcome, " + loggedIn.getName() + "! [" + loggedIn.getRole() + "]");
            } else {
                error("  ✗ Employee not found.");
            }
        }

        // ── Employee Menu ────────────────────────

        private void employeeMenu() {
            System.out.println("\n" + LINE);
            System.out.printf("  EMPLOYEE MENU  —  %s (%s)%n", loggedIn.getName(), loggedIn.getId());
            System.out.println(LINE);
            System.out.println("  1. Apply for Leave");
            System.out.println("  2. View My Leave Requests");
            System.out.println("  3. Cancel a Leave Request");
            System.out.println("  4. View My Leave Balances");
            System.out.println("  5. Logout");
            System.out.print("  Choice: ");
            switch (readInt()) {
                case 1 -> applyLeaveFlow();
                case 2 -> viewMyLeaves();
                case 3 -> cancelLeaveFlow();
                case 4 -> viewBalances(loggedIn);
                case 5 -> logout();
                default -> error("  Invalid option.");
            }
        }

        // ── Manager Menu ─────────────────────────

        private void managerMenu() {
            System.out.println("\n" + LINE);
            System.out.printf("  MANAGER MENU  —  %s (%s)%n", loggedIn.getName(), loggedIn.getId());
            System.out.println(LINE);
            System.out.println("  1. View Pending Leave Requests");
            System.out.println("  2. Approve a Leave Request");
            System.out.println("  3. Reject a Leave Request");
            System.out.println("  4. View Team Leave Summary");
            System.out.println("  5. Apply for My Own Leave");
            System.out.println("  6. View My Leave Balances");
            System.out.println("  7. Logout");
            System.out.print("  Choice: ");
            switch (readInt()) {
                case 1 -> viewPendingLeaves();
                case 2 -> approveLeaveFlow();
                case 3 -> rejectLeaveFlow();
                case 4 -> teamLeaveSummary();
                case 5 -> applyLeaveFlow();
                case 6 -> viewBalances(loggedIn);
                case 7 -> logout();
                default -> error("  Invalid option.");
            }
        }

        // ── Admin Menu ───────────────────────────

        private void adminMenu() {
            System.out.println("\n" + LINE);
            System.out.printf("  ADMIN MENU  —  %s (%s)%n", loggedIn.getName(), loggedIn.getId());
            System.out.println(LINE);
            System.out.println("  1.  Add Employee");
            System.out.println("  2.  View All Employees");
            System.out.println("  3.  Update Employee");
            System.out.println("  4.  Remove Employee");
            System.out.println("  5.  View All Leave Requests");
            System.out.println("  6.  Approve a Leave Request");
            System.out.println("  7.  Reject a Leave Request");
            System.out.println("  8.  Adjust Leave Balance");
            System.out.println("  9.  View Employee Leave Balances");
            System.out.println("  10. Apply for My Own Leave");
            System.out.println("  11. Logout");
            System.out.print("  Choice: ");
            switch (readInt()) {
                case 1  -> addEmployeeFlow();
                case 2  -> viewAllEmployees();
                case 3  -> updateEmployeeFlow();
                case 4  -> removeEmployeeFlow();
                case 5  -> viewAllLeaves();
                case 6  -> approveLeaveFlow();
                case 7  -> rejectLeaveFlow();
                case 8  -> adjustBalanceFlow();
                case 9  -> viewEmployeeBalancesFlow();
                case 10 -> applyLeaveFlow();
                case 11 -> logout();
                default -> error("  Invalid option.");
            }
        }

        // ─────────────────────────────────────────
        //  FLOWS
        // ─────────────────────────────────────────

        private void applyLeaveFlow() {
            System.out.println("\n  ── APPLY FOR LEAVE ──");
            System.out.println("  Leave Types:");
            LeaveType[] types = LeaveType.values();
            for (int i = 0; i < types.length; i++)
                System.out.printf("  %d. %-20s (Balance: %d days)%n",
                        i + 1, types[i].getDisplayName(), loggedIn.getBalance(types[i]));
            System.out.print("  Select type (number): ");
            int choice = readInt();
            if (choice < 1 || choice > types.length) { error("  Invalid choice."); return; }
            LeaveType lt = types[choice - 1];

            System.out.print("  Start date (yyyy-MM-dd): ");
            LocalDate start = parseDate(sc.nextLine().trim());
            if (start == null) return;

            System.out.print("  End date   (yyyy-MM-dd): ");
            LocalDate end = parseDate(sc.nextLine().trim());
            if (end == null) return;

            System.out.print("  Reason: ");
            String reason = sc.nextLine().trim();

            Result<LeaveRequest> result = service.applyLeave(loggedIn.getId(), lt, start, end, reason);
            if (result.isSuccess()) {
                success("  ✓ Leave applied! Request ID: " + result.getValue().getRequestId()
                        + " | Days: " + result.getValue().getTotalDays());
            } else {
                error("  ✗ " + result.getError());
            }
        }

        private void viewMyLeaves() {
            System.out.println("\n  ── MY LEAVE REQUESTS ──");
            List<LeaveRequest> list = service.getEmployeeLeaves(loggedIn.getId());
            if (list.isEmpty()) { info("  No leave requests found."); return; }
            printLeaveTable(list);
        }

        private void cancelLeaveFlow() {
            System.out.println("\n  ── CANCEL LEAVE ──");
            viewMyLeaves();
            System.out.print("  Enter Request ID to cancel: ");
            String rid = sc.nextLine().trim().toUpperCase();
            Result<String> r = service.cancelLeave(rid, loggedIn.getId());
            if (r.isSuccess()) success("  ✓ " + r.getValue());
            else               error("  ✗ " + r.getError());
        }

        private void viewPendingLeaves() {
            System.out.println("\n  ── PENDING LEAVE REQUESTS (YOUR TEAM) ──");
            List<LeaveRequest> list = service.getPendingForManager(loggedIn.getId());
            if (list.isEmpty()) { info("  No pending requests."); return; }
            printLeaveTableWithEmployee(list);
        }

        private void approveLeaveFlow() {
            System.out.println("\n  ── APPROVE LEAVE ──");
            viewPendingLeaves();
            System.out.print("  Enter Request ID to approve: ");
            String rid = sc.nextLine().trim().toUpperCase();
            System.out.print("  Comment (optional): ");
            String comment = sc.nextLine().trim();
            Result<String> r = service.approveLeave(rid, loggedIn.getId(), comment);
            if (r.isSuccess()) success("  ✓ " + r.getValue());
            else               error("  ✗ " + r.getError());
        }

        private void rejectLeaveFlow() {
            System.out.println("\n  ── REJECT LEAVE ──");
            viewPendingLeaves();
            System.out.print("  Enter Request ID to reject: ");
            String rid = sc.nextLine().trim().toUpperCase();
            System.out.print("  Rejection reason: ");
            String comment = sc.nextLine().trim();
            Result<String> r = service.rejectLeave(rid, loggedIn.getId(), comment);
            if (r.isSuccess()) success("  ✓ " + r.getValue());
            else               error("  ✗ " + r.getError());
        }

        private void teamLeaveSummary() {
            System.out.println("\n  ── TEAM LEAVE SUMMARY ──");
            List<Employee> team = service.getSubordinates(loggedIn.getId());
            if (team.isEmpty()) { info("  No team members found."); return; }
            System.out.printf("  %-10s %-20s %-10s %-10s %-10s%n",
                    "ID", "Name", "Annual", "Sick", "Casual");
            System.out.println("  " + LINE);
            for (Employee e : team) {
                System.out.printf("  %-10s %-20s %-10d %-10d %-10d%n",
                        e.getId(), e.getName(),
                        e.getBalance(LeaveType.ANNUAL),
                        e.getBalance(LeaveType.SICK),
                        e.getBalance(LeaveType.CASUAL));
            }
        }

        private void viewBalances(Employee emp) {
            System.out.println("\n  ── LEAVE BALANCES: " + emp.getName() + " ──");
            System.out.printf("  %-25s %s%n", "Leave Type", "Balance (days)");
            System.out.println("  " + "─".repeat(40));
            for (Map.Entry<LeaveType, Integer> entry : emp.getAllBalances().entrySet()) {
                System.out.printf("  %-25s %d%n", entry.getKey().getDisplayName(), entry.getValue());
            }
        }

        private void viewAllEmployees() {
            System.out.println("\n  ── ALL EMPLOYEES ──");
            printEmployeeList();
        }

        private void viewAllLeaves() {
            System.out.println("\n  ── ALL LEAVE REQUESTS ──");
            List<LeaveRequest> list = service.getAllLeaveRequests();
            if (list.isEmpty()) { info("  No leave requests."); return; }
            printLeaveTableWithEmployee(list);
        }

        private void addEmployeeFlow() {
            System.out.println("\n  ── ADD EMPLOYEE ──");
            System.out.print("  Name: ");       String name = sc.nextLine().trim();
            System.out.print("  Email: ");      String email = sc.nextLine().trim();
            System.out.print("  Department: "); String dept = sc.nextLine().trim();
            System.out.println("  Role: 1.EMPLOYEE  2.MANAGER  3.ADMIN");
            System.out.print("  Select: ");
            Role role = switch (readInt()) {
                case 2 -> Role.MANAGER;
                case 3 -> Role.ADMIN;
                default -> Role.EMPLOYEE;
            };
            System.out.print("  Manager ID (leave blank if none): ");
            String mgrId = sc.nextLine().trim();
            if (mgrId.isEmpty()) mgrId = null;
            Employee emp = service.addEmployee(name, email, dept, role, mgrId);
            success("  ✓ Employee added. ID: " + emp.getId());
        }

        private void updateEmployeeFlow() {
            System.out.println("\n  ── UPDATE EMPLOYEE ──");
            viewAllEmployees();
            System.out.print("  Employee ID: ");
            String id = sc.nextLine().trim().toUpperCase();
            Optional<Employee> opt = service.findEmployee(id);
            if (opt.isEmpty()) { error("  ✗ Not found."); return; }
            Employee e = opt.get();
            System.out.print("  New Name [" + e.getName() + "]: ");
            String name = sc.nextLine().trim();
            System.out.print("  New Email [" + e.getEmail() + "]: ");
            String email = sc.nextLine().trim();
            System.out.print("  New Department [" + e.getDepartment() + "]: ");
            String dept = sc.nextLine().trim();
            service.updateEmployee(id,
                    name.isEmpty()  ? e.getName()       : name,
                    email.isEmpty() ? e.getEmail()      : email,
                    dept.isEmpty()  ? e.getDepartment() : dept);
            success("  ✓ Employee updated.");
        }

        private void removeEmployeeFlow() {
            System.out.println("\n  ── REMOVE EMPLOYEE ──");
            viewAllEmployees();
            System.out.print("  Employee ID to remove: ");
            String id = sc.nextLine().trim().toUpperCase();
            System.out.print("  Confirm removal of " + id + "? (yes/no): ");
            if (sc.nextLine().trim().equalsIgnoreCase("yes")) {
                if (service.removeEmployee(id)) success("  ✓ Employee removed.");
                else error("  ✗ Not found.");
            } else {
                info("  Cancelled.");
            }
        }

        private void adjustBalanceFlow() {
            System.out.println("\n  ── ADJUST LEAVE BALANCE ──");
            System.out.print("  Employee ID: ");
            String id = sc.nextLine().trim().toUpperCase();
            Optional<Employee> opt = service.findEmployee(id);
            if (opt.isEmpty()) { error("  ✗ Not found."); return; }
            viewBalances(opt.get());
            LeaveType[] types = LeaveType.values();
            for (int i = 0; i < types.length; i++)
                System.out.printf("  %d. %s%n", i + 1, types[i].getDisplayName());
            System.out.print("  Select leave type: ");
            int c = readInt();
            if (c < 1 || c > types.length) { error("  Invalid."); return; }
            System.out.print("  Days to add (use negative to deduct): ");
            int days = readInt();
            Result<String> r = service.adjustBalance(id, types[c - 1], days);
            if (r.isSuccess()) success("  ✓ " + r.getValue());
            else               error("  ✗ " + r.getError());
        }

        private void viewEmployeeBalancesFlow() {
            System.out.println("\n  ── VIEW EMPLOYEE BALANCES ──");
            viewAllEmployees();
            System.out.print("  Employee ID: ");
            String id = sc.nextLine().trim().toUpperCase();
            Optional<Employee> opt = service.findEmployee(id);
            if (opt.isEmpty()) { error("  ✗ Not found."); return; }
            viewBalances(opt.get());
        }

        // ─────────────────────────────────────────
        //  PRINT HELPERS
        // ─────────────────────────────────────────

        private void printLeaveTable(List<LeaveRequest> list) {
            System.out.printf("  %-8s %-12s %-12s %-12s %-6s %-10s%n",
                    "ID", "Type", "Start", "End", "Days", "Status");
            System.out.println("  " + LINE);
            for (LeaveRequest r : list) {
                System.out.printf("  %-8s %-12s %-12s %-12s %-6d %-10s%n",
                        r.getRequestId(),
                        r.getLeaveType().name(),
                        r.getStartDate().format(DATE_FMT),
                        r.getEndDate().format(DATE_FMT),
                        r.getTotalDays(),
                        r.getStatus());
                if (r.getReviewComment() != null && !r.getReviewComment().isEmpty())
                    System.out.println("         Comment: " + r.getReviewComment());
            }
        }

        private void printLeaveTableWithEmployee(List<LeaveRequest> list) {
            System.out.printf("  %-8s %-10s %-20s %-12s %-6s %-10s%n",
                    "ID", "EmpID", "Type", "Start", "Days", "Status");
            System.out.println("  " + LINE);
            for (LeaveRequest r : list) {
                Optional<Employee> emp = service.findEmployee(r.getEmployeeId());
                String empName = emp.map(Employee::getName).orElse("Unknown");
                System.out.printf("  %-8s %-10s %-20s %-12s %-6d %-10s%n",
                        r.getRequestId(),
                        r.getEmployeeId(),
                        r.getLeaveType().getDisplayName(),
                        r.getStartDate().format(DATE_FMT),
                        r.getTotalDays(),
                        r.getStatus());
                System.out.printf("           Employee: %-20s  End: %s%n",
                        empName, r.getEndDate().format(DATE_FMT));
                System.out.printf("           Reason: %s%n", r.getReason());
                if (r.getReviewComment() != null && !r.getReviewComment().isEmpty())
                    System.out.printf("           Comment: %s%n", r.getReviewComment());
                System.out.println();
            }
        }

        // ─────────────────────────────────────────
        //  UTILS
        // ─────────────────────────────────────────

        private void logout() {
            info("  Logged out. Goodbye, " + loggedIn.getName() + "!");
            loggedIn = null;
        }

        private int readInt() {
            try {
                return Integer.parseInt(sc.nextLine().trim());
            } catch (NumberFormatException e) {
                return -1;
            }
        }

        private LocalDate parseDate(String s) {
            try {
                return LocalDate.parse(s, DATE_FMT);
            } catch (DateTimeParseException e) {
                error("  ✗ Invalid date format. Use yyyy-MM-dd.");
                return null;
            }
        }

        private void success(String msg) { System.out.println("\u001B[32m" + msg + "\u001B[0m"); }
        private void error(String msg)   { System.out.println("\u001B[31m" + msg + "\u001B[0m"); }
        private void info(String msg)    { System.out.println("\u001B[36m" + msg + "\u001B[0m"); }
    }

    // ─────────────────────────────────────────────
    //  ENTRY POINT
    // ─────────────────────────────────────────────

    public static void main(String[] args) {
        new ConsoleUI().start();
    }
}