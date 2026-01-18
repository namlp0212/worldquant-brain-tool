package demo.webapp;

import demo.webapp.regular.EmailSender;
import java.util.List;

public class Main {
    private static final String cookie = "";

    public static void main(String[] args) {
        EmailSender emailSender = new EmailSender(
                "smtp.gmail.com",
                587,
                "namlp0212@gmail.com",
                "cgpv mcoj shed raep"
        );

        try {
            emailSender.sendEmail(
                    "namlp0212@gmail.com",
                    "Library Notification",
                    "Your borrowed book is due tomorrow!"
            );
            System.out.println("Email sent successfully!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}