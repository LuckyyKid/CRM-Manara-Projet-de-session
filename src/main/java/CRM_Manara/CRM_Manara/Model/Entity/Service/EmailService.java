package CRM_Manara.CRM_Manara.Model.Entity.Service;

import CRM_Manara.CRM_Manara.Model.Entity.Administrateurs;
import CRM_Manara.CRM_Manara.Model.Entity.Inscription;
import CRM_Manara.CRM_Manara.Repository.AdminRepo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Service
public class EmailService {
    private static final Logger log = LoggerFactory.getLogger(EmailService.class);
    private static final String DEMO_COPY_EMAIL = "ahmedbelm51@gmail.com";

    private final Environment environment;
    private final AdminRepo adminRepo;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public EmailService(Environment environment, AdminRepo adminRepo) {
        this.environment = environment;
        this.adminRepo = adminRepo;
    }

    public void sendEmail(String to, String subject, String text) {
        log.info("EMAIL PREPARE to={} subject={}", to, subject);
        try {
            if (!isResendConfigured()) {
                log.error("EMAIL NON ENVOYE: RESEND_API_KEY manquante.");
                return;
            }

            String responseBody = sendWithResend(to, subject, text);
            log.info("EMAIL ENVOYE via Resend to={} cc={} subject={} response={}", to, DEMO_COPY_EMAIL, subject, responseBody);
        } catch (Exception ex) {
            log.error("EMAIL NON ENVOYE to={} subject={} reason={}", to, subject, ex.getMessage(), ex);
        }
    }

    public void sendInscriptionConfirmation(String to, Inscription inscription) {
        sendEmail(to, "Confirmation d'inscription - CRM Manara", buildInscriptionBody(inscription));
    }

    public void sendInscriptionRejected(String to, Inscription inscription) {
        sendEmail(to, "Demande d'inscription refusée - CRM Manara", buildInscriptionRejectedBody(inscription));
    }

    public void sendPresenceUpdate(String to, Inscription inscription) {
        sendEmail(to, "Mise à jour de présence - CRM Manara", buildPresenceUpdateBody(inscription));
    }

    public void sendNotificationEmail(String to, String title, String message) {
        sendEmail(to, title + " - CRM Manara", message + "\n\nCRM Manara");
    }

    public void notifyAdminsOfInscriptionRequest(Inscription inscription) {
        String subject = "Nouvelle demande d'inscription - CRM Manara";
        String body = "Une nouvelle demande d'inscription a été envoyée.\n\n" +
                "Parent: " + inscription.getEnfant().getParent().getPrenom() + " " + inscription.getEnfant().getParent().getNom() + "\n" +
                "Enfant: " + inscription.getEnfant().getPrenom() + " " + inscription.getEnfant().getNom() + "\n" +
                "Activité: " + inscription.getAnimation().getActivity().getActivyName() + "\n" +
                "Début: " + inscription.getAnimation().getStartTime() + "\n" +
                "Fin: " + inscription.getAnimation().getEndTime() + "\n\n" +
                "Consultez la page admin des demandes pour approuver ou refuser.";

        for (Administrateurs admin : adminRepo.findAll()) {
            if (admin.getUser() != null && admin.getUser().getEmail() != null && !admin.getUser().getEmail().isBlank()) {
                sendEmail(admin.getUser().getEmail(), subject, body);
            }
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

    private String buildInscriptionRejectedBody(Inscription inscription) {
        return "Bonjour,\n\n" +
                "La demande d'inscription a ete refusee.\n" +
                "Enfant: " + inscription.getEnfant().getPrenom() + " " + inscription.getEnfant().getNom() + "\n" +
                "Activite: " + inscription.getAnimation().getActivity().getActivyName() + "\n\n" +
                "Consultez vos notifications pour plus de details.\n\n" +
                "Merci,\nCRM Manara";
    }

    private String buildPresenceUpdateBody(Inscription inscription) {
        return "Bonjour,\n\n" +
                "La présence a été mise à jour pour votre enfant.\n" +
                "Enfant: " + inscription.getEnfant().getPrenom() + " " + inscription.getEnfant().getNom() + "\n" +
                "Activité: " + inscription.getAnimation().getActivity().getActivyName() + "\n" +
                "Présence: " + inscription.getPresenceStatus() + "\n" +
                (inscription.getIncidentNote() != null && !inscription.getIncidentNote().isBlank()
                        ? "Note: " + inscription.getIncidentNote() + "\n"
                        : "") +
                "\nMerci,\nCRM Manara";
    }

    private boolean isResendConfigured() {
        String apiKey = environment.getProperty("RESEND_API_KEY");
        return apiKey != null && !apiKey.isBlank();
    }

    private String sendWithResend(String to, String subject, String text) throws Exception {
        String payload = "{"
                + "\"from\":\"" + escapeJson(environment.getProperty("RESEND_FROM_EMAIL", "onboarding@resend.dev")) + "\","
                + "\"to\":[\"" + escapeJson(to) + "\"],"
                + "\"cc\":[\"" + escapeJson(DEMO_COPY_EMAIL) + "\"],"
                + "\"subject\":\"" + escapeJson(subject) + "\","
                + "\"html\":\"" + escapeJson("<div style=\\\"font-family:Arial,sans-serif;line-height:1.6;color:#1f2937;white-space:pre-line;\\\">"
                + escapeHtml(text)
                + "</div>") + "\""
                + "}";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.resend.com/emails"))
                .header("Authorization", "Bearer " + environment.getProperty("RESEND_API_KEY"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 300) {
            throw new IllegalStateException("Resend error " + response.statusCode() + ": " + response.body());
        }
        return response.body();
    }

    private String escapeHtml(String value) {
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private String escapeJson(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }
}
