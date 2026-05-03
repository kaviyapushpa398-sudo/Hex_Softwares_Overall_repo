import java.util.Scanner;

class Student {
    String name;
    int studentId;
    int[] grades;

    // Constructor
    Student(String name, int studentId, int[] grades) {
        this.name = name;
        this.studentId = studentId;
        this.grades = grades;
    }

    // Calculate average grade
    double calculateAverage() {
        int sum = 0;
        for (int grade : grades) {
            sum += grade;
        }
        return (double) sum / grades.length;
    }

    // Display grades
    void displayGrades() {
        System.out.print("Grades: ");
        for (int grade : grades) {
            System.out.print(grade + " ");
        }
        System.out.println();
    }

    // Check pass or fail
    void checkResult() {
        double avg = calculateAverage();
        if (avg >= 50) {
            System.out.println("Result: PASS");
        } else {
            System.out.println("Result: FAIL");
        }
    }
}

public class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);

        System.out.print("Enter number of students: ");
        int n = sc.nextInt();

        Student[] students = new Student[n];
        double classTotal = 0;

        for (int i = 0; i < n; i++) {
            sc.nextLine(); // clear buffer

            System.out.println("\nEnter details for Student " + (i + 1));

            System.out.print("Name: ");
            String name = sc.nextLine();

            System.out.print("Student ID: ");
            int id = sc.nextInt();

            System.out.print("Enter number of subjects: ");
            int subjects = sc.nextInt();

            int[] grades = new int[subjects];

            System.out.println("Enter grades:");
            for (int j = 0; j < subjects; j++) {
                grades[j] = sc.nextInt();
            }

            students[i] = new Student(name, id, grades);
        }

        System.out.println("\n--- Student Details ---");

        for (Student s : students) {
            System.out.println("\nName: " + s.name);
            System.out.println("ID: " + s.studentId);
            s.displayGrades();

            double avg = s.calculateAverage();
            System.out.println("Average: " + avg);

            s.checkResult();

            classTotal += avg;
        }

        double classAverage = classTotal / n;
        System.out.println("\nClass Average: " + classAverage);

        sc.close();
    }
}