import java.util.*;
import java.time.*;
import java.time.format.*;
import java.util.stream.*;


/**
 * ╔══════════════════════════════════════════════════════════════╗
 * ║          INVENTORY MANAGEMENT SYSTEM - Single File           ║
 * ║          Products | Transactions | Reports | Search          ║
 * ╚══════════════════════════════════════════════════════════════╝
 *
 * Features:
 *  - Product CRUD (Add, View, Update, Delete)
 *  - Transaction management (Stock IN / Stock OUT / RETURN)
 *  - Low stock alerts
 *  - Search & filter products
 *  - Sales & inventory reports
 *  - Category management
 */
public class InventoryManagementSystem {

    // ─────────────────────────────────────────────
    //  ENUMS
    // ─────────────────────────────────────────────

    enum TransactionType {
        STOCK_IN("Stock In   ▲"),
        STOCK_OUT("Stock Out  ▼"),
        RETURN("Return     ↩");

        private final String label;
        TransactionType(String label) { this.label = label; }
        public String getLabel() { return label; }
    }

    enum Category {
        ELECTRONICS, CLOTHING, FOOD, FURNITURE,
        STATIONERY, HEALTHCARE, TOOLS, OTHER
    }

    // ─────────────────────────────────────────────
    //  MODEL: Product
    // ─────────────────────────────────────────────

    static class Product {
        private static int idCounter = 1;

        private final int id;
        private String name;
        private String sku;
        private Category category;
        private double price;
        private int quantity;
        private int lowStockThreshold;
        private String supplier;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public Product(String name, String sku, Category category,
                       double price, int quantity, int lowStockThreshold, String supplier) {
            this.id               = idCounter++;
            this.name             = name;
            this.sku              = sku.toUpperCase();
            this.category         = category;
            this.price            = price;
            this.quantity         = quantity;
            this.lowStockThreshold = lowStockThreshold;
            this.supplier         = supplier;
            this.createdAt        = LocalDateTime.now();
            this.updatedAt        = LocalDateTime.now();
        }

        // Getters
        public int getId()               { return id; }
        public String getName()          { return name; }
        public String getSku()           { return sku; }
        public Category getCategory()    { return category; }
        public double getPrice()         { return price; }
        public int getQuantity()         { return quantity; }
        public int getLowStockThreshold(){ return lowStockThreshold; }
        public String getSupplier()      { return supplier; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public boolean isLowStock()      { return quantity <= lowStockThreshold; }

        // Setters
        public void setName(String name)              { this.name = name; touch(); }
        public void setSku(String sku)                { this.sku = sku.toUpperCase(); touch(); }
        public void setCategory(Category c)           { this.category = c; touch(); }
        public void setPrice(double price)            { this.price = price; touch(); }
        public void setQuantity(int quantity)         { this.quantity = quantity; touch(); }
        public void setLowStockThreshold(int t)       { this.lowStockThreshold = t; touch(); }
        public void setSupplier(String supplier)      { this.supplier = supplier; touch(); }

        private void touch() { this.updatedAt = LocalDateTime.now(); }

        public double getTotalValue() { return price * quantity; }

        @Override
        public String toString() {
            return String.format("%-4d | %-20s | %-12s | %-12s | %8.2f | %6d | %-15s | %s",
                id, truncate(name, 20), sku, category, price, quantity, truncate(supplier, 15),
                isLowStock() ? "⚠ LOW STOCK" : "OK");
        }
    }

    // ─────────────────────────────────────────────
    //  MODEL: Transaction
    // ─────────────────────────────────────────────

    static class Transaction {
        private static int idCounter = 1;

        private final int id;
        private final int productId;
        private final String productName;
        private final TransactionType type;
        private final int quantity;
        private final double unitPrice;
        private final String note;
        private final LocalDateTime timestamp;

        public Transaction(int productId, String productName,
                           TransactionType type, int quantity, double unitPrice, String note) {
            this.id          = idCounter++;
            this.productId   = productId;
            this.productName = productName;
            this.type        = type;
            this.quantity    = quantity;
            this.unitPrice   = unitPrice;
            this.note        = note;
            this.timestamp   = LocalDateTime.now();
        }

        public int getId()             { return id; }
        public int getProductId()      { return productId; }
        public String getProductName() { return productName; }
        public TransactionType getType(){ return type; }
        public int getQuantity()       { return quantity; }
        public double getUnitPrice()   { return unitPrice; }
        public double getTotalAmount() { return unitPrice * quantity; }
        public LocalDateTime getTimestamp() { return timestamp; }

        @Override
        public String toString() {
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            return String.format("%-4d | %-12s | %-20s | %6d | %9.2f | %10.2f | %-20s | %s",
                id, type.getLabel(), truncate(productName, 20),
                quantity, unitPrice, getTotalAmount(),
                truncate(note, 20), timestamp.format(fmt));
        }
    }

    // ─────────────────────────────────────────────
    //  DATA STORE
    // ─────────────────────────────────────────────

    static class InventoryStore {
        private final Map<Integer, Product>     products     = new LinkedHashMap<>();
        private final List<Transaction>         transactions = new ArrayList<>();

        // ── Product Operations ──

        public void addProduct(Product p) {
            products.put(p.getId(), p);
        }

        public Optional<Product> findProductById(int id) {
            return Optional.ofNullable(products.get(id));
        }

        public Optional<Product> findProductBySku(String sku) {
            return products.values().stream()
                .filter(p -> p.getSku().equalsIgnoreCase(sku))
                .findFirst();
        }

        public List<Product> getAllProducts() {
            return new ArrayList<>(products.values());
        }

        public List<Product> searchProducts(String query) {
            String q = query.toLowerCase();
            return products.values().stream()
                .filter(p -> p.getName().toLowerCase().contains(q)
                          || p.getSku().toLowerCase().contains(q)
                          || p.getSupplier().toLowerCase().contains(q)
                          || p.getCategory().name().toLowerCase().contains(q))
                .collect(Collectors.toList());
        }

        public List<Product> getLowStockProducts() {
            return products.values().stream()
                .filter(Product::isLowStock)
                .collect(Collectors.toList());
        }

        public List<Product> getByCategory(Category c) {
            return products.values().stream()
                .filter(p -> p.getCategory() == c)
                .collect(Collectors.toList());
        }

        public boolean deleteProduct(int id) {
            return products.remove(id) != null;
        }

        // ── Transaction Operations ──

        public void addTransaction(Transaction t) {
            transactions.add(t);
        }

        public List<Transaction> getAllTransactions() {
            return new ArrayList<>(transactions);
        }

        public List<Transaction> getTransactionsByProduct(int productId) {
            return transactions.stream()
                .filter(t -> t.getProductId() == productId)
                .collect(Collectors.toList());
        }

        public List<Transaction> getTransactionsByType(TransactionType type) {
            return transactions.stream()
                .filter(t -> t.getType() == type)
                .collect(Collectors.toList());
        }

        // ── Report Helpers ──

        public double getTotalInventoryValue() {
            return products.values().stream().mapToDouble(Product::getTotalValue).sum();
        }

        public double getTotalRevenue() {
            return transactions.stream()
                .filter(t -> t.getType() == TransactionType.STOCK_OUT)
                .mapToDouble(Transaction::getTotalAmount)
                .sum();
        }

        public double getTotalPurchases() {
            return transactions.stream()
                .filter(t -> t.getType() == TransactionType.STOCK_IN)
                .mapToDouble(Transaction::getTotalAmount)
                .sum();
        }

        public int getTotalProductCount() { return products.size(); }
        public int getTotalTransactionCount() { return transactions.size(); }
    }

    // ─────────────────────────────────────────────
    //  SERVICE LAYER
    // ─────────────────────────────────────────────

    static class InventoryService {
        private final InventoryStore store;

        public InventoryService(InventoryStore store) {
            this.store = store;
        }

        public Result<Product> addProduct(String name, String sku, String categoryStr,
                                          double price, int qty, int threshold, String supplier) {
            if (name.isBlank())    return Result.fail("Product name cannot be empty.");
            if (sku.isBlank())     return Result.fail("SKU cannot be empty.");
            if (price < 0)        return Result.fail("Price cannot be negative.");
            if (qty < 0)          return Result.fail("Quantity cannot be negative.");
            if (threshold < 0)    return Result.fail("Threshold cannot be negative.");

            if (store.findProductBySku(sku).isPresent())
                return Result.fail("A product with SKU '" + sku.toUpperCase() + "' already exists.");

            Category cat;
            try {
                cat = Category.valueOf(categoryStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                return Result.fail("Invalid category. Choose from: " + Arrays.toString(Category.values()));
            }

            Product p = new Product(name, sku, cat, price, qty, threshold, supplier);
            store.addProduct(p);
            return Result.ok(p);
        }

        public Result<Transaction> stockIn(int productId, int qty, double unitPrice, String note) {
            if (qty <= 0)     return Result.fail("Quantity must be positive.");
            if (unitPrice < 0) return Result.fail("Unit price cannot be negative.");

            Optional<Product> opt = store.findProductById(productId);
            if (opt.isEmpty()) return Result.fail("Product ID " + productId + " not found.");

            Product p = opt.get();
            p.setQuantity(p.getQuantity() + qty);

            Transaction t = new Transaction(p.getId(), p.getName(),
                TransactionType.STOCK_IN, qty, unitPrice, note);
            store.addTransaction(t);
            return Result.ok(t);
        }

        public Result<Transaction> stockOut(int productId, int qty, double unitPrice, String note) {
            if (qty <= 0)     return Result.fail("Quantity must be positive.");
            if (unitPrice < 0) return Result.fail("Unit price cannot be negative.");

            Optional<Product> opt = store.findProductById(productId);
            if (opt.isEmpty()) return Result.fail("Product ID " + productId + " not found.");

            Product p = opt.get();
            if (p.getQuantity() < qty)
                return Result.fail("Insufficient stock. Available: " + p.getQuantity());

            p.setQuantity(p.getQuantity() - qty);

            Transaction t = new Transaction(p.getId(), p.getName(),
                TransactionType.STOCK_OUT, qty, unitPrice, note);
            store.addTransaction(t);
            return Result.ok(t);
        }

        public Result<Transaction> returnStock(int productId, int qty, double unitPrice, String note) {
            if (qty <= 0)     return Result.fail("Quantity must be positive.");

            Optional<Product> opt = store.findProductById(productId);
            if (opt.isEmpty()) return Result.fail("Product ID " + productId + " not found.");

            Product p = opt.get();
            p.setQuantity(p.getQuantity() + qty);

            Transaction t = new Transaction(p.getId(), p.getName(),
                TransactionType.RETURN, qty, unitPrice, note);
            store.addTransaction(t);
            return Result.ok(t);
        }

        public Result<Product> updateProduct(int id, String name, double price,
                                             int threshold, String supplier) {
            Optional<Product> opt = store.findProductById(id);
            if (opt.isEmpty()) return Result.fail("Product ID " + id + " not found.");

            Product p = opt.get();
            if (!name.isBlank())     p.setName(name);
            if (price >= 0)         p.setPrice(price);
            if (threshold >= 0)     p.setLowStockThreshold(threshold);
            if (!supplier.isBlank()) p.setSupplier(supplier);
            return Result.ok(p);
        }

        public Result<String> deleteProduct(int id) {
            if (!store.findProductById(id).isPresent())
                return Result.fail("Product ID " + id + " not found.");
            boolean removed = store.deleteProduct(id);
            return removed ? Result.ok("Product deleted.") : Result.fail("Could not delete product.");
        }

        public InventoryStore getStore() { return store; }
    }

    // ─────────────────────────────────────────────
    //  RESULT WRAPPER
    // ─────────────────────────────────────────────

    static class Result<T> {
        private final T value;
        private final String error;

        private Result(T value, String error) {
            this.value = value;
            this.error = error;
        }

        public static <T> Result<T> ok(T value) { return new Result<>(value, null); }
        public static <T> Result<T> fail(String error) { return new Result<>(null, error); }

        public boolean isSuccess() { return error == null; }
        public T getValue()        { return value; }
        public String getError()   { return error; }
    }

    // ─────────────────────────────────────────────
    //  CLI / UI LAYER
    // ─────────────────────────────────────────────

    static class CLI {
        private final Scanner          scanner = new Scanner(System.in);
        private final InventoryService service;
        private final InventoryStore   store;

        public CLI(InventoryService service) {
            this.service = service;
            this.store   = service.getStore();
        }

        public void start() {
            printBanner();
            seedSampleData();

            boolean running = true;
            while (running) {
                printMainMenu();
                int choice = readInt("Enter choice: ");
                switch (choice) {
                    case 1  -> productMenu();
                    case 2  -> transactionMenu();
                    case 3  -> reportsMenu();
                    case 4  -> searchMenu();
                    case 5  -> lowStockAlert();
                    case 0  -> { running = false; print("\n  👋  Goodbye!\n"); }
                    default -> error("Invalid choice. Please try again.");
                }
            }
        }

        // ── Menus ──

        private void printMainMenu() {
            line('═', 50);
            print("  MAIN MENU");
            line('─', 50);
            print("  [1] Product Management");
            print("  [2] Transaction Management");
            print("  [3] Reports & Summary");
            print("  [4] Search Products");
            print("  [5] Low Stock Alerts");
            print("  [0] Exit");
            line('═', 50);
        }

        private void productMenu() {
            boolean back = false;
            while (!back) {
                line('─', 50);
                print("  PRODUCT MANAGEMENT");
                line('─', 50);
                print("  [1] Add Product");
                print("  [2] View All Products");
                print("  [3] View Product Details");
                print("  [4] Update Product");
                print("  [5] Delete Product");
                print("  [6] View by Category");
                print("  [0] Back");
                line('─', 50);
                int ch = readInt("Choice: ");
                switch (ch) {
                    case 1 -> addProductFlow();
                    case 2 -> viewAllProducts();
                    case 3 -> viewProductDetails();
                    case 4 -> updateProductFlow();
                    case 5 -> deleteProductFlow();
                    case 6 -> viewByCategory();
                    case 0 -> back = true;
                    default -> error("Invalid choice.");
                }
            }
        }

        private void transactionMenu() {
            boolean back = false;
            while (!back) {
                line('─', 50);
                print("  TRANSACTION MANAGEMENT");
                line('─', 50);
                print("  [1] Stock IN  (Purchase / Restock)");
                print("  [2] Stock OUT (Sale / Dispatch)");
                print("  [3] Return Stock");
                print("  [4] View All Transactions");
                print("  [5] View Transactions by Product");
                print("  [0] Back");
                line('─', 50);
                int ch = readInt("Choice: ");
                switch (ch) {
                    case 1 -> stockInFlow();
                    case 2 -> stockOutFlow();
                    case 3 -> returnFlow();
                    case 4 -> viewAllTransactions();
                    case 5 -> viewTransactionsByProduct();
                    case 0 -> back = true;
                    default -> error("Invalid choice.");
                }
            }
        }

        private void reportsMenu() {
            boolean back = false;
            while (!back) {
                line('─', 50);
                print("  REPORTS & SUMMARY");
                line('─', 50);
                print("  [1] Inventory Summary");
                print("  [2] Financial Report");
                print("  [3] Category Report");
                print("  [4] Transaction Summary by Type");
                print("  [0] Back");
                line('─', 50);
                int ch = readInt("Choice: ");
                switch (ch) {
                    case 1 -> inventorySummary();
                    case 2 -> financialReport();
                    case 3 -> categoryReport();
                    case 4 -> transactionTypeReport();
                    case 0 -> back = true;
                    default -> error("Invalid choice.");
                }
            }
        }

        // ── Product Flows ──

        private void addProductFlow() {
            header("ADD NEW PRODUCT");
            String name     = readString("Product Name     : ");
            String sku      = readString("SKU              : ");
            printCategories();
            String catStr   = readString("Category         : ");
            double price    = readDouble("Unit Price (₹)   : ");
            int qty         = readInt("Initial Qty      : ");
            int threshold   = readInt("Low Stock Alert  : ");
            String supplier = readString("Supplier         : ");

            Result<Product> result = service.addProduct(name, sku, catStr,
                                        price, qty, threshold, supplier);
            if (result.isSuccess()) {
                success("Product added! ID: " + result.getValue().getId());
            } else {
                error(result.getError());
            }
        }

        private void viewAllProducts() {
            List<Product> list = store.getAllProducts();
            if (list.isEmpty()) { info("No products found."); return; }
            header("ALL PRODUCTS (" + list.size() + ")");
            printProductHeader();
            list.forEach(p -> print("  " + p));
            line('─', 110);
            print(String.format("  Total Inventory Value: ₹%,.2f", store.getTotalInventoryValue()));
        }

        private void viewProductDetails() {
            int id = readInt("Enter Product ID: ");
            store.findProductById(id).ifPresentOrElse(p -> {
                header("PRODUCT DETAILS");
                DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
                print("  ID           : " + p.getId());
                print("  Name         : " + p.getName());
                print("  SKU          : " + p.getSku());
                print("  Category     : " + p.getCategory());
                print("  Price        : ₹" + String.format("%,.2f", p.getPrice()));
                print("  Quantity     : " + p.getQuantity());
                print("  Total Value  : ₹" + String.format("%,.2f", p.getTotalValue()));
                print("  Low Stock At : " + p.getLowStockThreshold());
                print("  Status       : " + (p.isLowStock() ? "⚠ LOW STOCK" : "✓ OK"));
                print("  Supplier     : " + p.getSupplier());
                print("  Created      : " + p.getCreatedAt().format(fmt));
            }, () -> error("Product not found."));
        }

        private void updateProductFlow() {
            int id = readInt("Enter Product ID to update: ");
            store.findProductById(id).ifPresentOrElse(p -> {
                header("UPDATE PRODUCT (leave blank to keep current)");
                print("  Current Name    : " + p.getName());
                String name = readString("  New Name        : ");
                print("  Current Price   : ₹" + p.getPrice());
                String priceStr = readString("  New Price       : ");
                print("  Current Alert   : " + p.getLowStockThreshold());
                String thrStr = readString("  New Alert Level : ");
                print("  Current Supplier: " + p.getSupplier());
                String supp = readString("  New Supplier    : ");

                double price = priceStr.isBlank() ? p.getPrice() : Double.parseDouble(priceStr);
                int    thr   = thrStr.isBlank()   ? p.getLowStockThreshold() : Integer.parseInt(thrStr);

                Result<Product> result = service.updateProduct(id, name, price, thr, supp);
                if (result.isSuccess()) success("Product updated.");
                else error(result.getError());

            }, () -> error("Product not found."));
        }

        private void deleteProductFlow() {
            int id = readInt("Enter Product ID to delete: ");
            store.findProductById(id).ifPresentOrElse(p -> {
                print("  ⚠ Delete '" + p.getName() + "'? This cannot be undone.");
                String confirm = readString("  Type YES to confirm: ");
                if ("YES".equals(confirm)) {
                    Result<String> result = service.deleteProduct(id);
                    if (result.isSuccess()) success(result.getValue());
                    else error(result.getError());
                } else {
                    info("Deletion cancelled.");
                }
            }, () -> error("Product not found."));
        }

        private void viewByCategory() {
            printCategories();
            String catStr = readString("Enter Category: ");
            try {
                Category cat = Category.valueOf(catStr.toUpperCase());
                List<Product> list = store.getByCategory(cat);
                if (list.isEmpty()) { info("No products in this category."); return; }
                header("PRODUCTS IN " + cat);
                printProductHeader();
                list.forEach(p -> print("  " + p));
            } catch (IllegalArgumentException e) {
                error("Invalid category.");
            }
        }

        // ── Transaction Flows ──

        private void stockInFlow() {
            header("STOCK IN — PURCHASE / RESTOCK");
            int id = readInt("Product ID  : ");
            if (store.findProductById(id).isEmpty()) { error("Product not found."); return; }
            int qty       = readInt("Quantity    : ");
            double price  = readDouble("Unit Price  : ");
            String note   = readString("Note/Ref    : ");

            Result<Transaction> result = service.stockIn(id, qty, price, note);
            if (result.isSuccess()) {
                success("Stock IN recorded. Transaction ID: " + result.getValue().getId());
                store.findProductById(id).ifPresent(p ->
                    print("  New Quantity: " + p.getQuantity()));
            } else error(result.getError());
        }

        private void stockOutFlow() {
            header("STOCK OUT — SALE / DISPATCH");
            int id = readInt("Product ID  : ");
            store.findProductById(id).ifPresentOrElse(p -> {
                print("  Available   : " + p.getQuantity());
                int qty      = readInt("Quantity    : ");
                double price = readDouble("Sale Price  : ");
                String note  = readString("Note/Ref    : ");

                Result<Transaction> result = service.stockOut(id, qty, price, note);
                if (result.isSuccess()) {
                    success("Stock OUT recorded. Transaction ID: " + result.getValue().getId());
                    print("  Remaining   : " + p.getQuantity());
                    if (p.isLowStock()) print("  ⚠ LOW STOCK WARNING for " + p.getName());
                } else error(result.getError());
            }, () -> error("Product not found."));
        }

        private void returnFlow() {
            header("RETURN STOCK");
            int id = readInt("Product ID  : ");
            if (store.findProductById(id).isEmpty()) { error("Product not found."); return; }
            int qty      = readInt("Quantity    : ");
            double price = readDouble("Unit Price  : ");
            String note  = readString("Reason      : ");

            Result<Transaction> result = service.returnStock(id, qty, price, note);
            if (result.isSuccess()) {
                success("Return recorded. Transaction ID: " + result.getValue().getId());
            } else error(result.getError());
        }

        private void viewAllTransactions() {
            List<Transaction> list = store.getAllTransactions();
            if (list.isEmpty()) { info("No transactions found."); return; }
            header("ALL TRANSACTIONS (" + list.size() + ")");
            printTransactionHeader();
            list.forEach(t -> print("  " + t));
        }

        private void viewTransactionsByProduct() {
            int id = readInt("Enter Product ID: ");
            List<Transaction> list = store.getTransactionsByProduct(id);
            if (list.isEmpty()) { info("No transactions for this product."); return; }
            store.findProductById(id).ifPresent(p ->
                header("TRANSACTIONS — " + p.getName()));
            printTransactionHeader();
            list.forEach(t -> print("  " + t));
        }

        // ── Reports ──

        private void inventorySummary() {
            header("INVENTORY SUMMARY");
            List<Product> products = store.getAllProducts();
            long lowStockCount = products.stream().filter(Product::isLowStock).count();
            int totalQty = products.stream().mapToInt(Product::getQuantity).sum();
            double topValue = products.stream().mapToDouble(Product::getTotalValue).max().orElse(0);

            print(String.format("  %-30s : %d",  "Total Products",      store.getTotalProductCount()));
            print(String.format("  %-30s : %d",  "Total Units in Stock", totalQty));
            print(String.format("  %-30s : ₹%,.2f", "Total Inventory Value", store.getTotalInventoryValue()));
            print(String.format("  %-30s : %d",  "Low Stock Items",     lowStockCount));
            print(String.format("  %-30s : %d",  "Total Transactions",  store.getTotalTransactionCount()));
            print(String.format("  %-30s : ₹%,.2f", "Highest Single Product Value", topValue));

            if (lowStockCount > 0) {
                print("\n  ⚠ LOW STOCK ITEMS:");
                store.getLowStockProducts().forEach(p ->
                    print(String.format("    • %-20s  Qty: %d  (min %d)",
                        p.getName(), p.getQuantity(), p.getLowStockThreshold())));
            }
        }

        private void financialReport() {
            header("FINANCIAL REPORT");
            double revenue   = store.getTotalRevenue();
            double purchases = store.getTotalPurchases();
            double invValue  = store.getTotalInventoryValue();
            double profit    = revenue - purchases;

            print(String.format("  %-30s : ₹%,.2f", "Total Purchases (Stock IN)",  purchases));
            print(String.format("  %-30s : ₹%,.2f", "Total Revenue   (Stock OUT)", revenue));
            print(String.format("  %-30s : ₹%,.2f", "Gross Profit/Loss",           profit));
            print(String.format("  %-30s : ₹%,.2f", "Current Inventory Value",     invValue));
            line('─', 55);
            if (profit >= 0) success(String.format("  Net Profit: ₹%,.2f", profit));
            else             error(String.format(  "  Net Loss:   ₹%,.2f", Math.abs(profit)));
        }

        private void categoryReport() {
            header("CATEGORY REPORT");
            Map<Category, List<Product>> grouped = store.getAllProducts().stream()
                .collect(Collectors.groupingBy(Product::getCategory));

            print(String.format("  %-14s | %5s | %8s | %14s",
                "Category", "Items", "Units", "Value (₹)"));
            line('─', 55);
            grouped.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.comparing(Enum::name)))
                .forEach(e -> {
                    int    count = e.getValue().size();
                    int    units = e.getValue().stream().mapToInt(Product::getQuantity).sum();
                    double val   = e.getValue().stream().mapToDouble(Product::getTotalValue).sum();
                    print(String.format("  %-14s | %5d | %8d | %14.2f",
                        e.getKey(), count, units, val));
                });
        }

        private void transactionTypeReport() {
            header("TRANSACTION TYPE SUMMARY");
            for (TransactionType type : TransactionType.values()) {
                List<Transaction> list = store.getTransactionsByType(type);
                int    count  = list.size();
                int    units  = list.stream().mapToInt(Transaction::getQuantity).sum();
                double amount = list.stream().mapToDouble(Transaction::getTotalAmount).sum();
                print(String.format("  %-12s : %4d txns | %6d units | ₹%,.2f",
                    type.getLabel(), count, units, amount));
            }
        }

        private void searchMenu() {
            header("SEARCH PRODUCTS");
            String query = readString("Enter search term (name/SKU/supplier/category): ");
            List<Product> results = store.searchProducts(query);
            if (results.isEmpty()) {
                info("No products found matching '" + query + "'.");
            } else {
                print("  Found " + results.size() + " result(s):");
                printProductHeader();
                results.forEach(p -> print("  " + p));
            }
        }

        private void lowStockAlert() {
            List<Product> low = store.getLowStockProducts();
            header("LOW STOCK ALERTS");
            if (low.isEmpty()) {
                success("All products are well-stocked!");
            } else {
                print("  ⚠ " + low.size() + " product(s) need restocking:\n");
                printProductHeader();
                low.forEach(p -> print("  " + p));
            }
        }

        // ── Display Helpers ──

        private void printBanner() {
            System.out.println();
            System.out.println("  ╔══════════════════════════════════════════════════════╗");
            System.out.println("  ║       📦  INVENTORY MANAGEMENT SYSTEM  📦            ║");
            System.out.println("  ║        Products | Transactions | Reports             ║");
            System.out.println("  ╚══════════════════════════════════════════════════════╝");
            System.out.println();
        }

        private void printProductHeader() {
            line('─', 110);
            print(String.format("  %-4s | %-20s | %-12s | %-12s | %8s | %6s | %-15s | %s",
                "ID", "Name", "SKU", "Category", "Price(₹)", "Qty", "Supplier", "Status"));
            line('─', 110);
        }

        private void printTransactionHeader() {
            line('─', 115);
            print(String.format("  %-4s | %-12s | %-20s | %6s | %9s | %10s | %-20s | %s",
                "ID", "Type", "Product", "Qty", "Unit(₹)", "Total(₹)", "Note", "Timestamp"));
            line('─', 115);
        }

        private void printCategories() {
            print("  Categories: " + Arrays.toString(Category.values()));
        }

        private void header(String title) {
            System.out.println();
            line('═', 60);
            System.out.println("  " + title);
            line('─', 60);
        }

        private void line(char c, int n) {
            System.out.println("  " + String.valueOf(c).repeat(n));
        }

        private void print(String s)   { System.out.println(s); }
        private void success(String s) { System.out.println("\n  ✓ " + s + "\n"); }
        private void error(String s)   { System.out.println("\n  ✗ ERROR: " + s + "\n"); }
        private void info(String s)    { System.out.println("\n  ℹ " + s + "\n"); }

        // ── Input Helpers ──

        private String readString(String prompt) {
            System.out.print("  " + prompt);
            return scanner.nextLine().trim();
        }

        private int readInt(String prompt) {
            while (true) {
                try {
                    System.out.print("  " + prompt);
                    int val = Integer.parseInt(scanner.nextLine().trim());
                    return val;
                } catch (NumberFormatException e) {
                    System.out.println("  ✗ Please enter a valid integer.");
                }
            }
        }

        private double readDouble(String prompt) {
            while (true) {
                try {
                    System.out.print("  " + prompt);
                    return Double.parseDouble(scanner.nextLine().trim());
                } catch (NumberFormatException e) {
                    System.out.println("  ✗ Please enter a valid number.");
                }
            }
        }

        // ── Sample Data ──

        private void seedSampleData() {
            // Products
            service.addProduct("HP Laptop 15",        "SKU-001", "ELECTRONICS", 55000, 12,  3,  "HP India");
            service.addProduct("Dell Monitor 24\"",   "SKU-002", "ELECTRONICS", 18500, 8,   2,  "Dell India");
            service.addProduct("USB-C Hub 7-Port",    "SKU-003", "ELECTRONICS",  2299, 45, 10,  "Anker");
            service.addProduct("Office Chair Pro",    "SKU-004", "FURNITURE",   12000, 5,   2,  "Featherlite");
            service.addProduct("A4 Paper 500 sheets", "SKU-005", "STATIONERY",    350, 120, 20, "JK Copier");
            service.addProduct("Paracetamol 500mg",   "SKU-006", "HEALTHCARE",     80, 300, 50, "Cipla");
            service.addProduct("Hammer 500g",         "SKU-007", "TOOLS",         550, 2,   3,  "Stanley");
            service.addProduct("Blue T-Shirt XL",     "SKU-008", "CLOTHING",      699, 30, 10,  "Raymond");

            // Transactions
            service.stockOut(1, 2, 58000, "Invoice #INV-001");
            service.stockOut(2, 1, 19000, "Invoice #INV-002");
            service.stockIn(3, 20, 1800, "PO #PO-001 Anker restock");
            service.stockOut(5, 30, 400, "Office supplies");
            service.returnStock(1, 1, 55000, "Customer return - defect");
            service.stockOut(7, 1, 580, "Maintenance dept");
        }
    }

    // ─────────────────────────────────────────────
    //  UTILITY
    // ─────────────────────────────────────────────

    static String truncate(String s, int maxLen) {
        if (s == null) return "";
        return s.length() <= maxLen ? s : s.substring(0, maxLen - 1) + "…";
    }

    // ─────────────────────────────────────────────
    //  MAIN
    // ─────────────────────────────────────────────

    public static void main(String[] args) {
        InventoryStore   store   = new InventoryStore();
        InventoryService service = new InventoryService(store);
        CLI              cli     = new CLI(service);
        cli.start();
    }
}