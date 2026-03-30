package CRM_Manara.CRM_Manara.Model.Entity.Service;

import CRM_Manara.CRM_Manara.Model.Entity.Inscription;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    // ADDED
    public void sendEmail(String to, String subject, String text) {
        // ADDED
        System.out.println("STEP 11 REACHED - EmailService.sendEmail()");
        System.out.println("JavaMailSender injected: " + (mailSender != null));
        System.out.println("Preparing email to: " + to);
        System.out.println("Email subject: " + subject);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);
        try {
            // ADDED
            System.out.println("Sending email...");
            // ADDED
            System.out.println("STEP 12 REACHED - Calling JavaMailSender.send()");
            mailSender.send(message);
            // ADDED
            System.out.println("EMAIL SENT SUCCESS");
            // ADDED
            System.out.println("STEP 13 REACHED - JavaMailSender.send() completed successfully for: " + to);
        } catch (Exception ex) {
            // ADDED
            System.out.println("STEP 13 FAILED - EMAIL NON ENVOYE");
            System.out.println("Email destination: " + to);
            System.out.println("Email error message: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    // MODIFIED
    public void sendInscriptionConfirmation(String to, Inscription inscription) {
        sendEmail(to, "Confirmation d'inscription - CRM Manara", buildInscriptionBody(inscription));
    }

    private String buildInscriptionBody(Inscription inscription) {
        return "Bonjour,\n\n" +
                "Votre inscription a ete confirmee.\n" +
                "Enfant: " + inscription.getEnfant().getPrenom() + " " + inscription.getEnfant().getNom() + "\n" +
                "Activite: " + inscription.getAnimation().getActivity().getActivyName() + "\n" +
                "Debut: " + inscription.getAnimation().getStartTime() + "\n" +
                "Fin: " + inscription.getAnimation().getEndTime() + "\n\n" +
                "Merci,\nCRM Manara";
    }
}
