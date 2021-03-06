//import java.util.List;
//import java.util.Scanner;

import jakarta.persistence.*;
import model.*;

import java.time.LocalDate;
import java.util.*;

import java.time.LocalTime;
class TranscriptComp implements Comparator<Transcript> {
    public int compare(Transcript a, Transcript b)
    {
        return a.getSection().getSemester().getStartDate().compareTo(b.getSection().getSemester().getStartDate());
    }
}

public class App {
    private static EntityManager em;

    public static void main(String[] args) throws Exception {
        EntityManagerFactory factory = Persistence.createEntityManagerFactory("CECS 323 Project 3");
        em = factory.createEntityManager();

        // Initializes Database to specifications
        initDB();

        // Student LookUp
        System.out.println("\nSearching Student Records");
        Scanner input = new Scanner(System.in);
        System.out.println("Please enter a student name:");
        String name = input.nextLine();

        Student student = studentLookup(name);

        // Courses and GPA output
        if (student != null) {
            Set<Transcript> gradeSet = student.getGrades();

            ArrayList<Transcript> grades = new ArrayList<Transcript>();
            grades.addAll(gradeSet);

            grades.sort(new TranscriptComp());

            double GPA = student.getGpa();
            for (Transcript grade : grades) {
                System.out.println(grade.getSection().getCourse().getDepartment().getAbbreviation() + " "
                        + grade.getSection().getCourse().getNumber() + ", "
                        + grade.getSection().getSemester().getTitle() + '.'
                        + " Grade earned: " + grade.getGradeEarned());
            }
            System.out.println("Student's GPA: " + GPA);
        }

        // Registration Application Logic goes here
        System.out.println("\nRegistering for Classes");
        System.out.println("Please enter a semester (Spring 2021, Fall 2021, Spring 2022):");
        String sem = input.nextLine();

        while (!sem.equals("Spring 2021") && sem.equals("Fall 2021") && sem.equals("Spring 2022"))
        {
            System.out.println("Incorrect input, please try again");
            System.out.println("Please enter a semester (Spring 2021, Fall 2021, Spring 2022):");
            sem = input.nextLine();
        }

        System.out.println("Please enter a student's name:");
        name = input.nextLine();

        System.out.println("Please enter a course section in the format <CECS 277-05>:");
        String section = input.nextLine();

        ArrayList<String> parsedInput = parseSection(section);
        while (parsedInput == null)
        {
            System.out.println("Incorrect input, please try again");
            System.out.println("Please enter a course section in the format <CECS 277-05>:");
            section = input.nextLine();
        }

        // A TypedQuery is strongly typed; a normal Query would not be.
        TypedQuery<Section> registerSection = em.createQuery("SELECT s FROM Section s INNER JOIN s.course c " +
                "INNER JOIN c.department d INNER JOIN s.semester ss " +
                "WHERE s.sectionNumber = ?1 AND c.number = ?2 AND d.abbreviation = ?3 AND ss.title = ?4", Section.class);
        registerSection.setParameter(1, (byte) Integer.parseInt(parsedInput.get(2)));
        registerSection.setParameter(2, parsedInput.get(1));
        registerSection.setParameter(3, parsedInput.get(0));
        registerSection.setParameter(4, sem);

        try {
            Section reqSection = registerSection.getSingleResult();

            Student stu = studentLookup(name);

            if (stu != null) {
                Student.RegistrationResult rr = stu.registerForSection(reqSection);

                // If successful, save to database
                if (rr.equals(Student.RegistrationResult.SUCCESS))
                {
                    em.getTransaction().begin();
                    reqSection.addEnrolled(stu);
                    em.getTransaction().commit();

                    System.out.println("Currently Enrolled:");
                    for (Section s : stu.getEnrollment())
                    {
                        System.out.println(s.toString());
                    }
                }
                else
                {
                    System.out.println("Could not register for course: " + rr);
                }
            }
        }
        catch (NoResultException ex) {
            System.out.println("Section was not found");
        }

        em.close();
    }

    public static Student studentLookup(String name)
    {
        // A TypedQuery is strongly typed; a normal Query would not be.
        TypedQuery<Student> namedStudent = em.createQuery("SELECT s FROM Student s WHERE s.name = ?1", Student.class);
        namedStudent.setParameter(1, name);
        try {
            Student requested = namedStudent.getSingleResult();
            return requested;
        }
        catch (NoResultException ex) {
            System.out.println("Student with name '" + name + "' not found.");
            return null;
        }
    }

    public static ArrayList<String> parseSection(String sec)
    {
        String dep;
        String num;
        String secNum;

        int space = sec.indexOf(" ");
        int dash = sec.indexOf("-");

        dep = sec.substring(0, space);
        num = sec.substring(space + 1, dash);
        String temp = sec.substring(dash + 1);

        char charArr[] = temp.toCharArray();
        int zeroLoc = -1;
        for (int i = 0; i < charArr.length; i++)
        {
            if (charArr[i] != '0')
            {
                zeroLoc = i + dash + 1;
            }
        }

        if (zeroLoc != -1)
        {
            secNum = sec.substring(zeroLoc);
        }
        else
        {
            secNum = temp;
        }

        if (dep.length() < 1 || num.length() < 1  || secNum.length() < 1)
        {
            return null;
        }

        return new ArrayList<String>(){{add(dep); add(num); add(secNum);}};
    }

    public static void initDB()
    {
        em.getTransaction().begin();

        // Semesters
        Semester sp2021 = new Semester("Spring 2021", LocalDate.of(2021, 1, 19));
        Semester fa2021 = new Semester("Fall 2021", LocalDate.of(2021, 8, 17));
        Semester sp2022 = new Semester("Spring 2022", LocalDate.of(2022, 1, 20));

        em.persist(sp2021);
        em.persist(fa2021);
        em.persist(sp2022);

        // Departments
        Department cecs = new Department("Computer Engineering and Computer Science", "CECS");
        Department ital = new Department("Italian Studies", "ITAL");
        em.persist(cecs);
        em.persist(ital);

        em.getTransaction().commit();
        em.getTransaction().begin();

        // Courses
        Course cecs174 = new Course(cecs, "174", "Introduction to Programming and Problem Solving", (byte) 3);
        em.persist(cecs174);

        em.getTransaction().commit();
        em.getTransaction().begin();

        Course cecs274 = new Course(cecs, "274", "Data Structures", (byte) 3);
        em.persist(cecs274);
        Prerequisite p274 = new Prerequisite(cecs174, cecs274, 'C');
        em.persist(p274);

        em.getTransaction().commit();
        em.getTransaction().begin();

        Course cecs277 = new Course(cecs, "277", "Object Oriented Application Programming", (byte) 3);
        em.persist(cecs277);
        Prerequisite p277 = new Prerequisite(cecs174, cecs277, 'C');
        em.persist(p277);

        em.getTransaction().commit();
        em.getTransaction().begin();


        em.getTransaction().commit();
        em.getTransaction().begin();


        Course cecs282 = new Course(cecs, "282", "Advanced C++", (byte) 3);
        Prerequisite p282_1 = new Prerequisite(cecs274, cecs282, 'C');
        em.persist(p282_1);

        Prerequisite p282_2 = new Prerequisite(cecs277, cecs282, 'C');
        em.persist(p282_2);
        em.persist(cecs282);

        em.getTransaction().commit();
        em.getTransaction().begin();

        Course ital101A = new Course(ital, "101A", "Fundamentals of Italian", (byte) 4);
        em.persist(ital101A);

        Course ital101B = new Course(ital, "101B", "Fundamentals of Italian", (byte) 4);
        Prerequisite p101B = new Prerequisite(ital101A, ital101B, 'D');
        em.persist(p101B);
        em.persist(ital101B);

        em.getTransaction().commit();
        em.getTransaction().begin();

        // TimeSlots
        TimeSlot MW = new TimeSlot((byte) 40, LocalTime.of(12, 30), LocalTime.of(13, 45));
        TimeSlot TuTh = new TimeSlot((byte) 20, LocalTime.of(14, 0), LocalTime.of(15, 15));
        TimeSlot MWF = new TimeSlot((byte) 42, LocalTime.of(12, 0), LocalTime.of(12, 50));
        TimeSlot F = new TimeSlot((byte) 2, LocalTime.of(8, 00), LocalTime.of(10, 45));

        em.persist(MW);
        em.persist(TuTh);
        em.persist(MWF);
        em.persist(F);

        // Sections
        Section cecs174s1 = new Section(cecs174, (byte) 1, sp2021, MW, (short) 105);
        Section cecs274s1 = new Section(cecs274, (byte) 1, fa2021, TuTh, (short) 140);
        Section cecs277s3 = new Section(cecs277, (byte) 3, fa2021, F, (short) 35);
        Section cecs282s5 = new Section(cecs282, (byte) 5, sp2022, TuTh, (short) 35);
        Section cecs277s1 = new Section(cecs277, (byte) 1, sp2022, MW, (short) 35);
        Section cecs282s7 = new Section(cecs282, (byte) 7, sp2022, MW, (short) 35);
        Section ital101As1 = new Section(ital101A, (byte) 1, sp2022, MWF, (short) 25);

        em.persist(cecs174s1);
        em.persist(cecs274s1);
        em.persist(cecs277s3);
        em.persist(cecs282s5);
        em.persist(cecs277s1);
        em.persist(cecs282s7);
        em.persist(ital101As1);

        em.getTransaction().commit();
        em.getTransaction().begin();

        // Students
        Student naomi = new Student("Naomi Nagata", 123456789);
        naomi.addEnrollment(cecs282s5);

        em.persist(naomi);
        em.getTransaction().commit();
        em.getTransaction().begin();

        Student james = new Student("James Holden", 987654321);

        em.persist(james);

        em.getTransaction().commit();
        em.getTransaction().begin();

        Student amos = new Student("Amos Burton", 555555555);

        em.persist(amos);

        em.getTransaction().commit();
        em.getTransaction().begin();

        Transcript naa = new Transcript(naomi, cecs174s1, "A");

        em.persist(naa);

        em.getTransaction().commit();
        em.getTransaction().begin();

        Transcript nab = new Transcript(naomi, cecs274s1, "A");

        em.persist(nab);

        em.getTransaction().commit();
        em.getTransaction().begin();

        Transcript nac = new Transcript(naomi, cecs277s3, "A");

        em.persist(nac);

        em.getTransaction().commit();
        em.getTransaction().begin();

        Transcript jca = new Transcript(james, cecs174s1, "C");

        em.persist(jca);

        em.getTransaction().commit();
        em.getTransaction().begin();

        Transcript jcb = new Transcript(james, cecs274s1, "C");

        em.persist(jcb);

        em.getTransaction().commit();
        em.getTransaction().begin();

        Transcript jcc = new Transcript(james, cecs277s3, "C");

        em.persist(jcc);

        em.getTransaction().commit();
        em.getTransaction().begin();

        Transcript aca = new Transcript(amos, cecs174s1, "C");

        em.persist(aca);

        em.getTransaction().commit();
        em.getTransaction().begin();

        Transcript abb = new Transcript(amos, cecs274s1, "B");

        em.persist(abb);

        em.getTransaction().commit();
        em.getTransaction().begin();

        Transcript adc = new Transcript(amos, cecs277s3, "D");

        em.persist(adc);

        em.getTransaction().commit();
    }
}
