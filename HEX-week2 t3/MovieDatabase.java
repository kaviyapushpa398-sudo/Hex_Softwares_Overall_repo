import java.util.*;
import java.util.stream.*;
import java.time.LocalDate;

// ============================================================
//  M O V I E   D A T A B A S E   M A N A G E R
//  Single-file Java application
// ============================================================

public class MovieDatabase {

    // ── ANSI colour palette ──────────────────────────────────
    static final String RESET  = "\u001B[0m";
    static final String BOLD   = "\u001B[1m";
    static final String DIM    = "\u001B[2m";
    static final String ITALIC = "\u001B[3m";

    static final String RED    = "\u001B[31m";
    static final String GREEN  = "\u001B[32m";
    static final String YELLOW = "\u001B[33m";
    static final String BLUE   = "\u001B[34m";
    static final String MAGENTA= "\u001B[35m";
    static final String CYAN   = "\u001B[36m";
    static final String WHITE  = "\u001B[97m";

    static final String BG_DARK   = "\u001B[48;5;235m";
    static final String BG_HEADER = "\u001B[48;5;17m";
    static final String GOLD      = "\u001B[38;5;220m";
    static final String ORANGE    = "\u001B[38;5;208m";
    static final String SILVER    = "\u001B[38;5;250m";

    // ── Movie entity ─────────────────────────────────────────
    static class Movie {
        private static int idCounter = 1;

        int    id;
        String title;
        String director;
        String genre;
        int    year;
        double rating;   // 0.0 – 10.0
        String language;
        int    durationMinutes;
        String synopsis;
        LocalDate addedOn;

        Movie(String title, String director, String genre,
              int year, double rating, String language,
              int durationMinutes, String synopsis) {
            this.id              = idCounter++;
            this.title           = title;
            this.director        = director;
            this.genre           = genre;
            this.year            = year;
            this.rating          = rating;
            this.language        = language;
            this.durationMinutes = durationMinutes;
            this.synopsis        = synopsis;
            this.addedOn         = LocalDate.now();
        }

        // Restore idCounter when loading seed data with explicit IDs
        static void setIdCounter(int n) { idCounter = n; }

        String ratingStars() {
            int full  = (int)(rating / 2);
            int half  = (rating / 2 - full) >= 0.5 ? 1 : 0;
            int empty = 5 - full - half;
            return GOLD  + "★".repeat(full)
                 + YELLOW + (half == 1 ? "½" : "")
                 + DIM    + "☆".repeat(empty)
                 + RESET;
        }

        String durationFmt() {
            return durationMinutes / 60 + "h " + durationMinutes % 60 + "m";
        }
    }

    // ── Database ─────────────────────────────────────────────
    static List<Movie> db = new ArrayList<>();
    static Scanner sc    = new Scanner(System.in);

    // ── Entry point ──────────────────────────────────────────
    public static void main(String[] args) {
        seedDatabase();
        clearScreen();
        printBanner();
        mainMenu();
    }

    // ── Main menu ────────────────────────────────────────────
    static void mainMenu() {
        while (true) {
            printMenuBox();
            String choice = prompt("  Enter choice").trim();
            clearScreen();
            switch (choice) {
                case "1" -> addMovie();
                case "2" -> listAllMovies();
                case "3" -> searchMovies();
                case "4" -> viewMovieDetails();
                case "5" -> editMovie();
                case "6" -> deleteMovie();
                case "7" -> topRatedMovies();
                case "8" -> moviesByGenre();
                case "9" -> statistics();
                case "0" -> {
                    printFarewell();
                    System.exit(0);
                }
                default  -> printError("Invalid choice. Please try again.");
            }
        }
    }

    // ── 1. Add movie ─────────────────────────────────────────
    static void addMovie() {
        printSectionHeader("ADD NEW MOVIE");

        String title    = promptRequired("  Title");
        String director = promptRequired("  Director");
        String genre    = chooseGenre();
        int    year     = promptInt("  Release Year (e.g. 2023)", 1888, LocalDate.now().getYear());
        double rating   = promptDouble("  Rating (0.0 – 10.0)", 0.0, 10.0);
        String language = promptRequired("  Language");
        int    duration = promptInt("  Duration (minutes)", 1, 900);
        System.out.print("  Synopsis " + DIM + "(optional)" + RESET + " : ");
        String synopsis = sc.nextLine().trim();

        Movie m = new Movie(title, director, genre, year, rating, language, duration, synopsis);
        db.add(m);

        printSuccess("Movie \"" + title + "\" added successfully! (ID #" + m.id + ")");
        waitEnter();
    }

    // ── 2. List all movies ───────────────────────────────────
    static void listAllMovies() {
        printSectionHeader("ALL MOVIES  [" + db.size() + " total]");
        if (db.isEmpty()) { printWarning("Database is empty."); waitEnter(); return; }

        printTableHeader();
        for (Movie m : db) printTableRow(m);
        printTableFooter();
        waitEnter();
    }

    // ── 3. Search ────────────────────────────────────────────
    static void searchMovies() {
        printSectionHeader("SEARCH MOVIES");
        System.out.println("  " + CYAN + "1" + RESET + " › By Title");
        System.out.println("  " + CYAN + "2" + RESET + " › By Director");
        System.out.println("  " + CYAN + "3" + RESET + " › By Genre");
        System.out.println("  " + CYAN + "4" + RESET + " › By Year");
        System.out.println("  " + CYAN + "5" + RESET + " › By Rating (≥ value)");
        System.out.println();

        String choice = prompt("  Search by").trim();
        List<Movie> results;

        switch (choice) {
            case "1" -> {
                String q = prompt("  Title keyword").toLowerCase();
                results = db.stream()
                    .filter(m -> m.title.toLowerCase().contains(q))
                    .collect(Collectors.toList());
            }
            case "2" -> {
                String q = prompt("  Director name").toLowerCase();
                results = db.stream()
                    .filter(m -> m.director.toLowerCase().contains(q))
                    .collect(Collectors.toList());
            }
            case "3" -> {
                String g = chooseGenre();
                results = db.stream()
                    .filter(m -> m.genre.equalsIgnoreCase(g))
                    .collect(Collectors.toList());
            }
            case "4" -> {
                int y = promptInt("  Year", 1888, LocalDate.now().getYear());
                results = db.stream()
                    .filter(m -> m.year == y)
                    .collect(Collectors.toList());
            }
            case "5" -> {
                double r = promptDouble("  Minimum rating", 0.0, 10.0);
                results = db.stream()
                    .filter(m -> m.rating >= r)
                    .sorted((a, b) -> Double.compare(b.rating, a.rating))
                    .collect(Collectors.toList());
            }
            default -> { printError("Invalid choice."); waitEnter(); return; }
        }

        System.out.println();
        if (results.isEmpty()) {
            printWarning("No movies found matching your criteria.");
        } else {
            printSuccess(results.size() + " movie(s) found:");
            printTableHeader();
            results.forEach(MovieDatabase::printTableRow);
            printTableFooter();
        }
        waitEnter();
    }

    // ── 4. View details ──────────────────────────────────────
    static void viewMovieDetails() {
        printSectionHeader("MOVIE DETAILS");
        listAllMoviesBrief();
        int id = promptInt("  Enter Movie ID", 1, Integer.MAX_VALUE);
        Movie m = findById(id);
        if (m == null) { printError("Movie ID #" + id + " not found."); waitEnter(); return; }
        printDetailCard(m);
        waitEnter();
    }

    // ── 5. Edit movie ────────────────────────────────────────
    static void editMovie() {
        printSectionHeader("EDIT MOVIE");
        listAllMoviesBrief();
        int id = promptInt("  Enter Movie ID to edit", 1, Integer.MAX_VALUE);
        Movie m = findById(id);
        if (m == null) { printError("Movie ID #" + id + " not found."); waitEnter(); return; }

        System.out.println();
        System.out.println("  " + DIM + "(Press ENTER to keep current value)" + RESET);
        System.out.println();

        String t = promptOptional("  Title [" + m.title + "]");
        if (!t.isEmpty()) m.title = t;

        String d = promptOptional("  Director [" + m.director + "]");
        if (!d.isEmpty()) m.director = d;

        String g = promptOptional("  Genre [" + m.genre + "] (enter new or ENTER to skip)");
        if (!g.isEmpty()) m.genre = g;

        String y = promptOptional("  Year [" + m.year + "]");
        if (!y.isEmpty()) m.year = Integer.parseInt(y);

        String r = promptOptional("  Rating [" + m.rating + "]");
        if (!r.isEmpty()) m.rating = Double.parseDouble(r);

        String l = promptOptional("  Language [" + m.language + "]");
        if (!l.isEmpty()) m.language = l;

        String dur = promptOptional("  Duration mins [" + m.durationMinutes + "]");
        if (!dur.isEmpty()) m.durationMinutes = Integer.parseInt(dur);

        String s = promptOptional("  Synopsis (enter new or ENTER to skip)");
        if (!s.isEmpty()) m.synopsis = s;

        printSuccess("Movie #" + id + " updated successfully!");
        waitEnter();
    }

    // ── 6. Delete movie ──────────────────────────────────────
    static void deleteMovie() {
        printSectionHeader("DELETE MOVIE");
        listAllMoviesBrief();
        int id = promptInt("  Enter Movie ID to delete", 1, Integer.MAX_VALUE);
        Movie m = findById(id);
        if (m == null) { printError("Movie ID #" + id + " not found."); waitEnter(); return; }

        System.out.print("  " + RED + "Delete \"" + m.title + "\"? [y/N] " + RESET);
        String conf = sc.nextLine().trim();
        if (conf.equalsIgnoreCase("y")) {
            db.remove(m);
            printSuccess("\"" + m.title + "\" deleted from database.");
        } else {
            printWarning("Deletion cancelled.");
        }
        waitEnter();
    }

    // ── 7. Top rated ─────────────────────────────────────────
    static void topRatedMovies() {
        printSectionHeader("TOP RATED MOVIES");
        int n = promptInt("  Show top N movies", 1, db.size() == 0 ? 1 : db.size());
        List<Movie> top = db.stream()
            .sorted((a, b) -> Double.compare(b.rating, a.rating))
            .limit(n)
            .collect(Collectors.toList());

        if (top.isEmpty()) { printWarning("Database is empty."); waitEnter(); return; }

        System.out.println();
        for (int i = 0; i < top.size(); i++) {
            Movie m = top.get(i);
            String medal = switch (i) {
                case 0 -> GOLD   + " 🥇 #1 ";
                case 1 -> SILVER + " 🥈 #2 ";
                case 2 -> ORANGE + " 🥉 #3 ";
                default -> CYAN  + "  #" + (i + 1) + " ";
            };
            System.out.println("  " + medal + BOLD + WHITE + m.title + RESET
                + "  " + m.ratingStars()
                + "  " + DIM + m.genre + " · " + m.year + RESET);
        }
        System.out.println();
        waitEnter();
    }

    // ── 8. Movies by genre ───────────────────────────────────
    static void moviesByGenre() {
        printSectionHeader("MOVIES BY GENRE");
        Map<String, List<Movie>> grouped = db.stream()
            .collect(Collectors.groupingBy(m -> m.genre));

        if (grouped.isEmpty()) { printWarning("Database is empty."); waitEnter(); return; }

        grouped.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .forEach(e -> {
                System.out.println("  " + BOLD + CYAN + e.getKey().toUpperCase() + RESET
                    + DIM + "  (" + e.getValue().size() + " movie" + (e.getValue().size()==1?"":"s") + ")" + RESET);
                e.getValue().forEach(m ->
                    System.out.println("    " + DIM + "┣" + RESET + " " + m.title
                        + "  " + DIM + "(" + m.year + ")" + RESET
                        + "  " + m.ratingStars()));
                System.out.println();
            });
        waitEnter();
    }

    // ── 9. Statistics ────────────────────────────────────────
    static void statistics() {
        printSectionHeader("DATABASE STATISTICS");
        if (db.isEmpty()) { printWarning("Database is empty."); waitEnter(); return; }

        double avgRating = db.stream().mapToDouble(m -> m.rating).average().orElse(0);
        double avgDur    = db.stream().mapToInt(m -> m.durationMinutes).average().orElse(0);
        Movie  best      = db.stream().max(Comparator.comparingDouble(m -> m.rating)).get();
        Movie  oldest    = db.stream().min(Comparator.comparingInt(m -> m.year)).get();
        Movie  newest    = db.stream().max(Comparator.comparingInt(m -> m.year)).get();
        long   genres    = db.stream().map(m -> m.genre).distinct().count();

        stat("Total Movies",       String.valueOf(db.size()));
        stat("Unique Genres",      String.valueOf(genres));
        stat("Average Rating",     String.format("%.1f / 10.0", avgRating));
        stat("Average Duration",   String.format("%dh %dm", (int)avgDur/60, (int)avgDur%60));
        stat("Highest Rated",      best.title + " (" + best.rating + ")");
        stat("Oldest Movie",       oldest.title + " (" + oldest.year + ")");
        stat("Newest Movie",       newest.title + " (" + newest.year + ")");

        System.out.println();
        System.out.println("  " + BOLD + "Rating Distribution:" + RESET);
        int[] buckets = new int[5]; // 0-1, 2-3, 4-5, 6-7, 8-9... adjusted
        for (Movie m : db) {
            int bucket = Math.min((int)(m.rating / 2), 4);
            buckets[bucket]++;
        }
        String[] labels = {"0–2", "2–4", "4–6", "6–8", "8–10"};
        int maxB = Arrays.stream(buckets).max().getAsInt();
        for (int i = 4; i >= 0; i--) {
            int barLen = maxB == 0 ? 0 : (buckets[i] * 30 / maxB);
            System.out.println("  " + labels[i] + "  "
                + GOLD + "█".repeat(barLen) + RESET
                + DIM  + "░".repeat(30 - barLen) + RESET
                + "  " + buckets[i]);
        }
        System.out.println();
        waitEnter();
    }

    // ── UI helpers ───────────────────────────────────────────

    static void printBanner() {
        System.out.println(BOLD + BG_HEADER + GOLD);
        System.out.println("  ╔══════════════════════════════════════════════════════╗  ");
        System.out.println("  ║                                                      ║  ");
        System.out.println("  ║    🎬  M O V I E   D A T A B A S E   M A N A G E R  ║  ");
        System.out.println("  ║                                                      ║  ");
        System.out.println("  ╚══════════════════════════════════════════════════════╝  ");
        System.out.println(RESET);
    }

    static void printMenuBox() {
        System.out.println();
        System.out.println(BOLD + CYAN + "  ┌─────────────────────────────┐" + RESET);
        System.out.println(BOLD + CYAN + "  │       MAIN MENU             │" + RESET);
        System.out.println(BOLD + CYAN + "  ├─────────────────────────────┤" + RESET);
        menuItem("1", "Add New Movie");
        menuItem("2", "List All Movies");
        menuItem("3", "Search Movies");
        menuItem("4", "View Movie Details");
        menuItem("5", "Edit Movie");
        menuItem("6", "Delete Movie");
        menuItem("7", "Top Rated Movies");
        menuItem("8", "Movies by Genre");
        menuItem("9", "Statistics");
        System.out.println(CYAN + "  │                             │" + RESET);
        menuItem("0", "Exit");
        System.out.println(BOLD + CYAN + "  └─────────────────────────────┘" + RESET);
        System.out.println();
    }

    static void menuItem(String key, String label) {
        System.out.println(CYAN + "  │  " + BOLD + GOLD + key + RESET + CYAN
            + "  " + WHITE + label
            + " ".repeat(25 - label.length())
            + CYAN + "│" + RESET);
    }

    static void printSectionHeader(String title) {
        System.out.println();
        String line = "═".repeat(title.length() + 4);
        System.out.println("  " + BOLD + BLUE + "╔" + line + "╗" + RESET);
        System.out.println("  " + BOLD + BLUE + "║  " + WHITE + title + BLUE + "  ║" + RESET);
        System.out.println("  " + BOLD + BLUE + "╚" + line + "╝" + RESET);
        System.out.println();
    }

    static void printTableHeader() {
        System.out.println("  " + DIM
            + String.format("%-4s  %-32s %-18s %-12s %6s %6s",
                "ID", "TITLE", "DIRECTOR", "GENRE", "YEAR", "RATING")
            + RESET);
        System.out.println("  " + DIM + "─".repeat(82) + RESET);
    }

    static void printTableRow(Movie m) {
        String title    = truncate(m.title, 31);
        String director = truncate(m.director, 17);
        String genre    = truncate(m.genre, 11);
        System.out.printf("  " + CYAN + "%-4d" + RESET + "  "
            + WHITE + "%-32s" + RESET
            + DIM   + "%-18s" + RESET
            + MAGENTA + "%-12s" + RESET
            + YELLOW + "%6d" + RESET
            + GOLD   + "%6.1f" + RESET + "%n",
            m.id, title, director, genre, m.year, m.rating);
    }

    static void printTableFooter() {
        System.out.println("  " + DIM + "─".repeat(82) + RESET);
    }

    static void printDetailCard(Movie m) {
        System.out.println();
        System.out.println("  " + BOLD + BG_DARK + WHITE + "  🎬  " + m.title + "  " + RESET);
        System.out.println();
        detail("ID",        "#" + m.id);
        detail("Director",  m.director);
        detail("Genre",     m.genre);
        detail("Year",      String.valueOf(m.year));
        detail("Rating",    m.rating + " / 10.0  " + m.ratingStars());
        detail("Language",  m.language);
        detail("Duration",  m.durationFmt());
        detail("Added On",  m.addedOn.toString());
        if (!m.synopsis.isEmpty()) {
            System.out.println();
            System.out.println("  " + BOLD + "Synopsis:" + RESET);
            wordWrap(m.synopsis, 70).forEach(line -> System.out.println("  " + DIM + line + RESET));
        }
        System.out.println();
    }

    static void detail(String label, String value) {
        System.out.printf("  " + BOLD + CYAN + "%-12s" + RESET + " : " + WHITE + "%s" + RESET + "%n",
            label, value);
    }

    static void stat(String label, String value) {
        System.out.printf("  " + BOLD + "%-20s" + RESET + " : " + GOLD + "%s" + RESET + "%n",
            label, value);
    }

    static void printSuccess(String msg) {
        System.out.println("\n  " + BOLD + GREEN + "✔  " + msg + RESET + "\n");
    }

    static void printError(String msg) {
        System.out.println("\n  " + BOLD + RED + "✘  " + msg + RESET + "\n");
    }

    static void printWarning(String msg) {
        System.out.println("\n  " + BOLD + YELLOW + "⚠  " + msg + RESET + "\n");
    }

    static void printFarewell() {
        System.out.println("\n  " + GOLD + BOLD + "Thank you for using Movie Database Manager. Goodbye! 🎬" + RESET + "\n");
    }

    // ── Input helpers ────────────────────────────────────────

    static String prompt(String label) {
        System.out.print("  " + BOLD + CYAN + label + RESET + " : ");
        return sc.nextLine();
    }

    static String promptOptional(String label) {
        System.out.print("  " + label + " : ");
        return sc.nextLine().trim();
    }

    static String promptRequired(String label) {
        while (true) {
            String v = prompt(label).trim();
            if (!v.isEmpty()) return v;
            printError("This field is required.");
        }
    }

    static int promptInt(String label, int min, int max) {
        while (true) {
            try {
                int v = Integer.parseInt(prompt(label).trim());
                if (v >= min && v <= max) return v;
                printError("Enter a value between " + min + " and " + max + ".");
            } catch (NumberFormatException e) {
                printError("Please enter a valid integer.");
            }
        }
    }

    static double promptDouble(String label, double min, double max) {
        while (true) {
            try {
                double v = Double.parseDouble(prompt(label).trim());
                if (v >= min && v <= max) return v;
                printError("Enter a value between " + min + " and " + max + ".");
            } catch (NumberFormatException e) {
                printError("Please enter a valid number.");
            }
        }
    }

    static String chooseGenre() {
        String[] genres = {
            "Action", "Adventure", "Animation", "Biography", "Comedy",
            "Crime", "Documentary", "Drama", "Fantasy", "Horror",
            "Musical", "Mystery", "Romance", "Sci-Fi", "Thriller",
            "Western", "Other"
        };
        System.out.println("  " + BOLD + "Select Genre:" + RESET);
        for (int i = 0; i < genres.length; i++) {
            System.out.printf("  " + CYAN + "%-2d" + RESET + " › %-14s", (i + 1), genres[i]);
            if ((i + 1) % 3 == 0) System.out.println();
        }
        System.out.println();
        int idx = promptInt("  Genre number", 1, genres.length);
        return genres[idx - 1];
    }

    static void listAllMoviesBrief() {
        if (db.isEmpty()) return;
        System.out.println("  " + DIM + "Available movies:" + RESET);
        db.forEach(m -> System.out.println("  " + CYAN + "#" + m.id + RESET + "  " + m.title));
        System.out.println();
    }

    static void waitEnter() {
        System.out.print("  " + DIM + "[Press ENTER to return to menu]" + RESET);
        sc.nextLine();
        clearScreen();
        printBanner();
    }

    static void clearScreen() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }

    // ── Utilities ────────────────────────────────────────────

    static Movie findById(int id) {
        return db.stream().filter(m -> m.id == id).findFirst().orElse(null);
    }

    static String truncate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }

    static List<String> wordWrap(String text, int width) {
        List<String> lines = new ArrayList<>();
        String[] words = text.split("\\s+");
        StringBuilder current = new StringBuilder();
        for (String w : words) {
            if (current.length() + w.length() + 1 > width) {
                lines.add(current.toString());
                current = new StringBuilder(w);
            } else {
                if (current.length() > 0) current.append(" ");
                current.append(w);
            }
        }
        if (current.length() > 0) lines.add(current.toString());
        return lines;
    }

    // ── Seed data ────────────────────────────────────────────
    static void seedDatabase() {
        db.add(new Movie("The Shawshank Redemption", "Frank Darabont",     "Drama",    1994, 9.3, "English", 142,
            "Two imprisoned men bond over a number of years, finding solace and eventual redemption through acts of common decency."));
        db.add(new Movie("The Godfather",            "Francis Ford Coppola","Crime",   1972, 9.2, "English", 175,
            "The aging patriarch of an organized crime dynasty transfers control of his clandestine empire to his reluctant son."));
        db.add(new Movie("The Dark Knight",          "Christopher Nolan",   "Action",  2008, 9.0, "English", 152,
            "When the menace known as the Joker wreaks havoc on Gotham City, Batman must accept one of the greatest tests of his ability to fight injustice."));
        db.add(new Movie("Schindler's List",         "Steven Spielberg",    "Biography",1993,8.9, "English", 195,
            "In German-occupied Poland during World War II, industrialist Oskar Schindler becomes concerned for his Jewish workforce."));
        db.add(new Movie("Pulp Fiction",             "Quentin Tarantino",   "Crime",   1994, 8.9, "English", 154,
            "The lives of two mob hitmen, a boxer, a gangster and his wife intertwine in four tales of violence and redemption."));
        db.add(new Movie("Inception",                "Christopher Nolan",   "Sci-Fi",  2010, 8.8, "English", 148,
            "A thief who steals corporate secrets through dream-sharing technology is given the inverse task of planting an idea into the mind of a CEO."));
        db.add(new Movie("Parasite",                 "Bong Joon-ho",        "Thriller",2019, 8.5, "Korean",  132,
            "Greed and class discrimination threaten the newly formed symbiotic relationship between the wealthy Park family and the destitute Kim clan."));
        db.add(new Movie("Spirited Away",            "Hayao Miyazaki",      "Animation",2001,8.6, "Japanese",125,
            "During her family's move to the suburbs, a sullen 10-year-old girl wanders into a world ruled by gods, witches, and spirits."));
        db.add(new Movie("Interstellar",             "Christopher Nolan",   "Sci-Fi",  2014, 8.6, "English", 169,
            "A team of explorers travel through a wormhole in space in an attempt to ensure humanity's survival."));
        db.add(new Movie("The Lion King",            "Roger Allers",        "Animation",1994,8.5, "English",  88,
            "Lion prince Simba and his father are targeted by his bitter uncle, who wants to ascend the throne himself."));

        Movie.setIdCounter(db.size() + 1);
    }
}