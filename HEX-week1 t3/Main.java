import java.util.*;

// Book class
class Book {
    int bookId;
    String title;
    String author;
    boolean isIssued;
    int issuedTo; // Member ID

    Book(int bookId, String title, String author) {
        this.bookId = bookId;
        this.title = title;
        this.author = author;
        this.isIssued = false;
        this.issuedTo = -1;
    }
}

// Member class
class Member {
    int memberId;
    String name;

    Member(int memberId, String name) {
        this.memberId = memberId;
        this.name = name;
    }
}

// Library class
class Library {
    List<Book> books = new ArrayList<>();
    List<Member> members = new ArrayList<>();

    // Add book
    void addBook(Book book) {
        books.add(book);
        System.out.println("Book added successfully!");
    }

    // Add member
    void addMember(Member member) {
        members.add(member);
        System.out.println("Member added successfully!");
    }

    // Issue book
    void issueBook(int bookId, int memberId) {
        for (Book b : books) {
            if (b.bookId == bookId) {
                if (!b.isIssued) {
                    b.isIssued = true;
                    b.issuedTo = memberId;
                    System.out.println("Book issued successfully!");
                } else {
                    System.out.println("Book is already issued!");
                }
                return;
            }
        }
        System.out.println("Book not found!");
    }

    // Return book
    void returnBook(int bookId) {
        for (Book b : books) {
            if (b.bookId == bookId) {
                if (b.isIssued) {
                    b.isIssued = false;
                    b.issuedTo = -1;
                    System.out.println("Book returned successfully!");
                } else {
                    System.out.println("Book was not issued!");
                }
                return;
            }
        }
        System.out.println("Book not found!");
    }

    // Display all books
    void displayBooks() {
        System.out.println("\n--- Book List ---");
        for (Book b : books) {
            System.out.print("ID: " + b.bookId + ", Title: " + b.title + ", Author: " + b.author);
            if (b.isIssued) {
                System.out.println(" (Issued to Member ID: " + b.issuedTo + ")");
            } else {
                System.out.println(" (Available)");
            }
        }
    }
}

// Main class
public class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        Library lib = new Library();

        while (true) {
            System.out.println("\n===== Library Menu =====");
            System.out.println("1. Add Book");
            System.out.println("2. Add Member");
            System.out.println("3. Issue Book");
            System.out.println("4. Return Book");
            System.out.println("5. Display Books");
            System.out.println("6. Exit");

            System.out.print("Enter choice: ");
            int choice = sc.nextInt();

            switch (choice) {
                case 1:
                    System.out.print("Enter Book ID: ");
                    int bId = sc.nextInt();
                    sc.nextLine();

                    System.out.print("Enter Title: ");
                    String title = sc.nextLine();

                    System.out.print("Enter Author: ");
                    String author = sc.nextLine();

                    lib.addBook(new Book(bId, title, author));
                    break;

                case 2:
                    System.out.print("Enter Member ID: ");
                    int mId = sc.nextInt();
                    sc.nextLine();

                    System.out.print("Enter Member Name: ");
                    String name = sc.nextLine();

                    lib.addMember(new Member(mId, name));
                    break;

                case 3:
                    System.out.print("Enter Book ID to issue: ");
                    int issueBookId = sc.nextInt();

                    System.out.print("Enter Member ID: ");
                    int issueMemberId = sc.nextInt();

                    lib.issueBook(issueBookId, issueMemberId);
                    break;

                case 4:
                    System.out.print("Enter Book ID to return: ");
                    int returnBookId = sc.nextInt();

                    lib.returnBook(returnBookId);
                    break;

                case 5:
                    lib.displayBooks();
                    break;

                case 6:
                    System.out.println("Exiting...");
                    sc.close();
                    return;

                default:
                    System.out.println("Invalid choice!");
            }
        }
    }
}