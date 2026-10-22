import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.Scanner;

/**
 * Personal Expense Tracker (Java 21)
 * Practices: arrays, loops, methods, and calculations.
 *
 * Compile:  javac ExpenseTracker.java
 * Run:      java ExpenseTracker
 */
public class ExpenseTracker {

    // ---------- Data model ----------
    record Expense(LocalDate date, String category, String description, double amount) {}

    // Fixed category list (array practice)
    static final String[] CATEGORIES = {
        "Food", "Transport", "Housing", "Utilities",
        "Entertainment", "Health", "Shopping", "Other"
    };

    // Expense storage: a plain array that grows when full
    static Expense[] expenses = new Expense[10];
    static int count = 0;

    static final Scanner in = new Scanner(System.in);

    // ---------- Main menu loop ----------
    public static void main(String[] args) {
        loadSampleData();
        System.out.println("=== Personal Expense Tracker ===");

        boolean running = true;
        while (running) {
            printMenu();
            int choice = readInt("Choose an option: ", 0, 6);
            switch (choice) {
                case 1 -> addExpense();
                case 2 -> listExpenses();
                case 3 -> showTotal();
                case 4 -> showCategoryTotals();
                case 5 -> showMonthlySummary();
                case 6 -> deleteExpense();
                case 0 -> running = false;
            }
        }
        System.out.println("Goodbye!");
    }

    static void printMenu() {
        System.out.println("""

            1) Add expense
            2) List all expenses
            3) Total spending
            4) Spending by category
            5) Monthly summary
            6) Delete an expense
            0) Exit""");
    }

    // ---------- Features ----------
    static void addExpense() {
        LocalDate date = readDate("Date (YYYY-MM-DD, blank = today): ");
        System.out.println("Categories:");
        for (int i = 0; i < CATEGORIES.length; i++) {
            System.out.printf("  %d) %s%n", i + 1, CATEGORIES[i]);
        }
        int catIndex = readInt("Category number: ", 1, CATEGORIES.length) - 1;
        System.out.print("Description: ");
        String desc = in.nextLine().trim();
        if (desc.isEmpty()) desc = "(none)";
        double amount = readAmount("Amount: $");

        addToArray(new Expense(date, CATEGORIES[catIndex], desc, amount));
        System.out.printf("Added: %s  %-13s %-20s $%.2f%n", date, CATEGORIES[catIndex], desc, amount);
    }

    static void listExpenses() {
        if (count == 0) { System.out.println("No expenses yet."); return; }
        printHeader();
        for (int i = 0; i < count; i++) {
            printRow(i + 1, expenses[i]);
        }
        System.out.printf("%-52s$%9.2f%n", "TOTAL", calculateTotal());
    }

    static void showTotal() {
        System.out.printf("Total spending: $%.2f across %d expense(s)%n", calculateTotal(), count);
        if (count > 0) {
            System.out.printf("Average expense: $%.2f%n", calculateTotal() / count);
            Expense max = findLargest();
            System.out.printf("Largest expense: $%.2f (%s - %s)%n", max.amount(), max.category(), max.description());
        }
    }

    static void showCategoryTotals() {
        double[] totals = totalsByCategory(null);
        double grand = calculateTotal();
        System.out.println("Spending by category:");
        printCategoryTable(totals, grand);
    }

    static void showMonthlySummary() {
        if (count == 0) { System.out.println("No expenses yet."); return; }
        YearMonth[] months = distinctMonths();
        for (YearMonth month : months) {
            double[] totals = totalsByCategory(month);
            double monthTotal = sum(totals);
            int items = countInMonth(month);
            System.out.printf("%n--- %s %d ---  (%d expense%s)%n",
                month.getMonth(), month.getYear(), items, items == 1 ? "" : "s");
            printCategoryTable(totals, monthTotal);
            System.out.printf("  Daily average: $%.2f%n", monthTotal / month.lengthOfMonth());
        }
    }

    static void deleteExpense() {
        if (count == 0) { System.out.println("No expenses to delete."); return; }
        listExpenses();
        int n = readInt("Number to delete (0 = cancel): ", 0, count);
        if (n == 0) return;
        Expense removed = expenses[n - 1];
        // Shift elements left to fill the gap
        for (int i = n - 1; i < count - 1; i++) {
            expenses[i] = expenses[i + 1];
        }
        expenses[--count] = null;
        System.out.printf("Deleted: %s $%.2f%n", removed.description(), removed.amount());
    }

    // ---------- Calculations ----------
    static double calculateTotal() {
        double total = 0;
        for (int i = 0; i < count; i++) total += expenses[i].amount();
        return total;
    }

    static Expense findLargest() {
        Expense max = expenses[0];
        for (int i = 1; i < count; i++) {
            if (expenses[i].amount() > max.amount()) max = expenses[i];
        }
        return max;
    }

    /** Totals per category; pass a month to filter, or null for all time. */
    static double[] totalsByCategory(YearMonth month) {
        double[] totals = new double[CATEGORIES.length];
        for (int i = 0; i < count; i++) {
            Expense e = expenses[i];
            if (month != null && !YearMonth.from(e.date()).equals(month)) continue;
            totals[indexOfCategory(e.category())] += e.amount();
        }
        return totals;
    }

    static int countInMonth(YearMonth month) {
        int c = 0;
        for (int i = 0; i < count; i++) {
            if (YearMonth.from(expenses[i].date()).equals(month)) c++;
        }
        return c;
    }

    /** Unique months found in the data, sorted oldest to newest. */
    static YearMonth[] distinctMonths() {
        YearMonth[] temp = new YearMonth[count];
        int n = 0;
        for (int i = 0; i < count; i++) {
            YearMonth ym = YearMonth.from(expenses[i].date());
            boolean seen = false;
            for (int j = 0; j < n; j++) {
                if (temp[j].equals(ym)) { seen = true; break; }
            }
            if (!seen) temp[n++] = ym;
        }
        // Simple insertion sort
        for (int i = 1; i < n; i++) {
            YearMonth key = temp[i];
            int j = i - 1;
            while (j >= 0 && temp[j].isAfter(key)) { temp[j + 1] = temp[j]; j--; }
            temp[j + 1] = key;
        }
        YearMonth[] result = new YearMonth[n];
        for (int i = 0; i < n; i++) result[i] = temp[i];
        return result;
    }

    static double sum(double[] values) {
        double s = 0;
        for (double v : values) s += v;
        return s;
    }

    static int indexOfCategory(String name) {
        for (int i = 0; i < CATEGORIES.length; i++) {
            if (CATEGORIES[i].equals(name)) return i;
        }
        return CATEGORIES.length - 1; // "Other"
    }

    // ---------- Array helper ----------
    static void addToArray(Expense e) {
        if (count == expenses.length) {
            Expense[] bigger = new Expense[expenses.length * 2];
            for (int i = 0; i < count; i++) bigger[i] = expenses[i];
            expenses = bigger;
        }
        expenses[count++] = e;
    }

    // ---------- Output helpers ----------
    static void printHeader() {
        System.out.printf("%-4s %-10s  %-13s %-20s %10s%n", "#", "Date", "Category", "Description", "Amount");
        System.out.println("-".repeat(62));
    }

    static void printRow(int num, Expense e) {
        System.out.printf("%-4d %-10s  %-13s %-20s $%9.2f%n",
            num, e.date(), e.category(), truncate(e.description(), 20), e.amount());
    }

    static void printCategoryTable(double[] totals, double grand) {
        for (int i = 0; i < CATEGORIES.length; i++) {
            if (totals[i] == 0) continue;
            double pct = grand == 0 ? 0 : totals[i] / grand * 100;
            int bars = (int) Math.round(pct / 5); // 1 block per 5%
            System.out.printf("  %-13s $%9.2f  %5.1f%%  %s%n", CATEGORIES[i], totals[i], pct, "#".repeat(bars));
        }
        System.out.printf("  %-13s $%9.2f%n", "TOTAL", grand);
    }

    static String truncate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }

    // ---------- Input helpers (validated) ----------
    static int readInt(String prompt, int min, int max) {
        while (true) {
            System.out.print(prompt);
            if (!in.hasNextLine()) System.exit(0);
            String line = in.nextLine().trim();
            try {
                int v = Integer.parseInt(line);
                if (v >= min && v <= max) return v;
            } catch (NumberFormatException ignored) { }
            System.out.printf("Please enter a number from %d to %d.%n", min, max);
        }
    }

    static double readAmount(String prompt) {
        while (true) {
            System.out.print(prompt);
            if (!in.hasNextLine()) System.exit(0);
            String line = in.nextLine().trim().replace("$", "").replace(",", "");
            try {
                double v = Double.parseDouble(line);
                if (v > 0) return Math.round(v * 100) / 100.0;
            } catch (NumberFormatException ignored) { }
            System.out.println("Please enter a positive amount, e.g. 12.50");
        }
    }

    static LocalDate readDate(String prompt) {
        while (true) {
            System.out.print(prompt);
            if (!in.hasNextLine()) System.exit(0);
            String line = in.nextLine().trim();
            if (line.isEmpty()) return LocalDate.now();
            try {
                return LocalDate.parse(line);
            } catch (DateTimeParseException e) {
                System.out.println("Use the format YYYY-MM-DD, e.g. 2026-10-21");
            }
        }
    }

    // ---------- Sample data so the summaries have something to show ----------
    static void loadSampleData() {
        addToArray(new Expense(LocalDate.of(2026, 9, 3),  "Housing",       "Rent",            1250.00));
        addToArray(new Expense(LocalDate.of(2026, 9, 8),  "Food",          "Groceries",         86.40));
        addToArray(new Expense(LocalDate.of(2026, 9, 15), "Transport",     "SEPTA pass",        103.00));
        addToArray(new Expense(LocalDate.of(2026, 9, 22), "Entertainment", "Movie night",        32.50));
        addToArray(new Expense(LocalDate.of(2026, 10, 1), "Housing",       "Rent",            1250.00));
        addToArray(new Expense(LocalDate.of(2026, 10, 5), "Utilities",     "Electric bill",     74.18));
        addToArray(new Expense(LocalDate.of(2026, 10, 9), "Food",          "Groceries",         92.15));
        addToArray(new Expense(LocalDate.of(2026, 10, 14),"Health",        "Pharmacy",          18.99));
        addToArray(new Expense(LocalDate.of(2026, 10, 18),"Shopping",      "Work boots",        64.00));
    }
}
