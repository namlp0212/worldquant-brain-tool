package demo.webapp;

import demo.webapp.regular.EmailSender;

public class Main {

    public static void main(String[] args) {
        EmailSender emailSender = new EmailSender(
                ConfigLoader.getSmtpHost(),
                ConfigLoader.getSmtpPort(),
                ConfigLoader.getSmtpUsername(),
                ConfigLoader.getSmtpPassword()
        );

        try {
            emailSender.sendEmail(
                    ConfigLoader.getEmailRecipient(),
                    "Library Notification",
                    "Your borrowed book is due tomorrow!"
            );
            System.out.println("Email sent successfully!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}