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

    public void sendInscriptionConfirmation(String to, Inscription inscription) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("Confirmation d'inscription - CRM Manara");
        message.setText(buildInscriptionBody(inscription));
        try {
            mailSender.send(message);
        } catch (Exception ex) {
            System.out.println("EMAIL NON ENVOYE: " + ex.getMessage());
        }
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
