package CRM_Manara.CRM_Manara.Model.Entity.Service;

import CRM_Manara.CRM_Manara.Model.Entity.Administrateurs;
import CRM_Manara.CRM_Manara.Model.Entity.Inscription;
import CRM_Manara.CRM_Manara.Repository.AdminRepo;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class EmailService {
    private static final Logger log = LoggerFactory.getLogger(EmailService.class);
    private static final String DEFAULT_DEMO_COPY_EMAIL = "ahmedbelm51@gmail.com";
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("EEEE d MMMM yyyy 'à' HH:mm", Locale.CANADA_FRENCH);

    private final Environment environment;
    private final AdminRepo adminRepo;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public EmailService(Environment environment, AdminRepo adminRepo) {
        this.environment = environment;
        this.adminRepo = adminRepo;
    }

    @PostConstruct
    void logEmailConfiguration() {
        if (isResendConfigured()) {
            log.info("EMAIL CONFIGURATION: Resend actif avec expéditeur {}", getFromEmail());
        } else {
            log.warn("EMAIL CONFIGURATION: Resend inactif, aucune clé API détectée.");
        }
    }

    public void sendEmail(String to, String subject, String text) {
        log.info("EMAIL PREPARE to={} subject={}", to, subject);
        try {
            if (!isResendConfigured()) {
                log.error("EMAIL NON ENVOYE: RESEND_API_KEY manquante.");
                return;
            }

            ResendRecipients recipients = resolveRecipients(to);
            String responseBody = sendWithResend(recipients, subject, text);
            log.info("EMAIL ENVOYE via Resend to={} cc={} subject={} response={}",
                    recipients.to, recipients.cc, subject, responseBody);
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
        sendEmail(to, title + " - CRM Manara", message + "\n\nCette notification a été envoyée par l'équipe CRM Manara.");
    }

    public void notifyAdminsOfInscriptionRequest(Inscription inscription) {
        String subject = "Nouvelle demande d'inscription - CRM Manara";
        String body = "Bonjour,\n\n"
                + "Une nouvelle demande d'inscription a été soumise par un parent et requiert une validation.\n\n"
                + "Parent : " + inscription.getEnfant().getParent().getPrenom() + " " + inscription.getEnfant().getParent().getNom() + "\n"
                + "Enfant : " + inscription.getEnfant().getPrenom() + " " + inscription.getEnfant().getNom() + "\n"
                + "Activité : " + inscription.getAnimation().getActivity().getActivyName() + "\n"
                + "Début : " + formatDateTime(inscription) + "\n"
                + "Fin : " + inscription.getAnimation().getEndTime().format(DateTimeFormatter.ofPattern("HH:mm")) + "\n\n"
                + "Veuillez consulter la page Demandes de l'administration pour approuver ou refuser cette requête.\n\n"
                + "CRM Manara";

        for (Administrateurs admin : adminRepo.findAll()) {
            if (admin.getUser() != null && admin.getUser().getEmail() != null && !admin.getUser().getEmail().isBlank()) {
                sendEmail(admin.getUser().getEmail(), subject, body);
            }
        }
    }

    public void notifyAdminsOfParentSignup(String nomComplet, String email, String provider) {
        String subject = "Nouveau compte parent en attente - CRM Manara";
        String body = "Bonjour,\n\n"
                + "Un nouveau compte parent a été créé et demeure en attente d'approbation.\n\n"
                + "Parent : " + nomComplet + "\n"
                + "Courriel : " + email + "\n"
                + "Méthode d'inscription : " + provider + "\n\n"
                + "Veuillez consulter la page Demandes pour approuver ou refuser ce compte.\n\n"
                + "CRM Manara";

        for (Administrateurs admin : adminRepo.findAll()) {
            if (admin.getUser() != null && admin.getUser().getEmail() != null && !admin.getUser().getEmail().isBlank()) {
                sendEmail(admin.getUser().getEmail(), subject, body);
            }
        }
    }

    public void sendAccountUpdatedConfirmation(String to, String displayName, String roleLabel, String changeSummary) {
        String subject = "Confirmation de modification du compte - CRM Manara";
        String body = "Bonjour " + displayName + ",\n\n"
                + "Nous vous confirmons que les paramètres de votre compte ont été mis à jour avec succès.\n\n"
                + "Rôle : " + roleLabel + "\n"
                + "Résumé des changements :\n- " + normalizeBulletList(changeSummary) + "\n\n"
                + "Si vous n'êtes pas à l'origine de cette modification, veuillez contacter l'administration dès que possible.\n\n"
                + "CRM Manara";
        sendEmail(to, subject, body);
    }

    public void notifyAdminsOfAccountUpdate(String displayName, String email, String roleLabel, String changeSummary) {
        String subject = "Compte modifié - CRM Manara";
        String body = "Bonjour,\n\n"
                + "Un compte utilisateur a été modifié dans la plateforme.\n\n"
                + "Utilisateur : " + displayName + "\n"
                + "Courriel : " + email + "\n"
                + "Rôle : " + roleLabel + "\n"
                + "Résumé des changements :\n- " + normalizeBulletList(changeSummary) + "\n\n"
                + "Consultez l'application si une vérification complémentaire est nécessaire.\n\n"
                + "CRM Manara";

        for (Administrateurs admin : adminRepo.findAll()) {
            if (admin.getUser() != null && admin.getUser().getEmail() != null && !admin.getUser().getEmail().isBlank()) {
                sendEmail(admin.getUser().getEmail(), subject, body);
            }
        }
    }

    private String buildInscriptionBody(Inscription inscription) {
        return "Bonjour,\n\n"
                + "La demande d'inscription a été approuvée avec succès.\n\n"
                + "Enfant : " + inscription.getEnfant().getPrenom() + " " + inscription.getEnfant().getNom() + "\n"
                + "Activité : " + inscription.getAnimation().getActivity().getActivyName() + "\n"
                + "Début : " + formatDateTime(inscription) + "\n"
                + "Fin : " + inscription.getAnimation().getEndTime().format(DateTimeFormatter.ofPattern("HH:mm")) + "\n\n"
                + "Vous pouvez consulter le planning et les notifications dans votre espace parent.\n\n"
                + "Cordialement,\nCRM Manara";
    }

    private String buildInscriptionRejectedBody(Inscription inscription) {
        return "Bonjour,\n\n"
                + "Nous vous informons que la demande d'inscription suivante a été refusée.\n\n"
                + "Enfant : " + inscription.getEnfant().getPrenom() + " " + inscription.getEnfant().getNom() + "\n"
                + "Activité : " + inscription.getAnimation().getActivity().getActivyName() + "\n"
                + "Début : " + formatDateTime(inscription) + "\n\n"
                + "Veuillez consulter vos notifications pour prendre connaissance des détails utiles.\n\n"
                + "Cordialement,\nCRM Manara";
    }

    private String buildPresenceUpdateBody(Inscription inscription) {
        return "Bonjour,\n\n"
                + "Une mise à jour a été apportée au suivi de présence de votre enfant.\n\n"
                + "Enfant : " + inscription.getEnfant().getPrenom() + " " + inscription.getEnfant().getNom() + "\n"
                + "Activité : " + inscription.getAnimation().getActivity().getActivyName() + "\n"
                + "Animation : " + formatDateTime(inscription) + "\n"
                + "Statut de présence : " + UiLabelService.presenceStatus(inscription.getPresenceStatus()) + "\n"
                + (inscription.getIncidentNote() != null && !inscription.getIncidentNote().isBlank()
                ? "Note de l'animateur : " + inscription.getIncidentNote().trim() + "\n"
                : "")
                + "\nVous pouvez consulter votre espace parent pour voir le suivi complet.\n\n"
                + "Cordialement,\nCRM Manara";
    }

    private boolean isResendConfigured() {
        String apiKey = getResendApiKey();
        return apiKey != null && !apiKey.isBlank();
    }

    private String sendWithResend(ResendRecipients recipients, String subject, String text) throws Exception {
        String toJson = buildJsonArray(recipients.to);
        String ccJson = buildJsonArray(recipients.cc);
        String htmlBody = buildEmailHtml(subject, text);
        String payload = "{"
                + "\"from\":\"" + escapeJson(getFromEmail()) + "\","
                + "\"to\":" + toJson + ","
                + "\"cc\":" + ccJson + ","
                + "\"subject\":\"" + escapeJson(subject) + "\","
                + "\"html\":\"" + escapeJson(htmlBody) + "\""
                + "}";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.resend.com/emails"))
                .header("Authorization", "Bearer " + getResendApiKey())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 300) {
            throw new IllegalStateException("Resend error " + response.statusCode() + ": " + response.body());
        }
        return response.body();
    }

    private ResendRecipients resolveRecipients(String requestedTo) {
        List<String> to = new ArrayList<>();
        List<String> cc = new ArrayList<>();
        String demoCopyEmail = getDemoCopyEmail();

        if (isDeliverableEmail(requestedTo)) {
            to.add(requestedTo.trim());
            if (!requestedTo.trim().equalsIgnoreCase(demoCopyEmail)) {
                cc.add(demoCopyEmail);
            }
        } else {
            log.warn("EMAIL REROUTE: destination '{}' invalide ou locale, envoi vers la copie de demo uniquement.", requestedTo);
            to.add(demoCopyEmail);
        }

        return new ResendRecipients(to, cc);
    }

    private String getResendApiKey() {
        return firstConfigured("resend.api.key", "RESEND_API_KEY");
    }

    private String getFromEmail() {
        return firstConfigured("resend.from.email", "RESEND_FROM_EMAIL", "onboarding@resend.dev");
    }

    private String getDemoCopyEmail() {
        return firstConfigured("app.email.demo-copy", "DEMO_COPY_EMAIL", DEFAULT_DEMO_COPY_EMAIL);
    }

    private String firstConfigured(String firstKey, String secondKey) {
        return firstConfigured(firstKey, secondKey, null);
    }

    private String firstConfigured(String firstKey, String secondKey, String fallback) {
        String first = environment.getProperty(firstKey);
        if (first != null && !first.isBlank()) {
            return first;
        }
        String second = environment.getProperty(secondKey);
        if (second != null && !second.isBlank()) {
            return second;
        }
        return fallback;
    }

    private boolean isDeliverableEmail(String email) {
        if (email == null) {
            return false;
        }
        String normalized = email.trim().toLowerCase(Locale.ROOT);
        int atIndex = normalized.indexOf('@');
        if (atIndex <= 0 || atIndex == normalized.length() - 1) {
            return false;
        }
        String domain = normalized.substring(atIndex + 1);
        return domain.contains(".") && !domain.endsWith(".local");
    }

    private String buildJsonArray(List<String> emails) {
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < emails.size(); i++) {
            if (i > 0) {
                json.append(",");
            }
            json.append("\"").append(escapeJson(emails.get(i))).append("\"");
        }
        json.append("]");
        return json.toString();
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

    private String buildEmailHtml(String title, String text) {
        String[] blocks = escapeHtml(text).split("\\n\\n");
        StringBuilder content = new StringBuilder();
        for (String block : blocks) {
            String normalized = block.trim();
            if (normalized.isEmpty()) {
                continue;
            }
            content.append("<p style=\"margin:0 0 16px;line-height:1.7;color:#243447;font-size:15px;\">")
                    .append(normalized.replace("\n", "<br>"))
                    .append("</p>");
        }

        return """
                <div style="background:#f4f7fb;padding:32px 16px;font-family:Inter,Arial,sans-serif;">
                    <div style="max-width:680px;margin:0 auto;background:#ffffff;border-radius:20px;overflow:hidden;box-shadow:0 20px 48px rgba(15,23,42,0.08);border:1px solid #e5edf6;">
                        <div style="padding:24px 28px;background:linear-gradient(135deg,#0b3b5c 0%,#114e73 100%);color:#ffffff;">
                            <div style="font-size:12px;letter-spacing:0.16em;text-transform:uppercase;opacity:0.78;margin-bottom:8px;">CRM Manara</div>
                            <h1 style="margin:0;font-size:24px;line-height:1.2;">%s</h1>
                        </div>
                        <div style="padding:28px;">
                            %s
                            <div style="margin-top:28px;padding-top:18px;border-top:1px solid #e5edf6;color:#667085;font-size:13px;line-height:1.7;">
                                Centre Manara<br>
                                Portail famille, animation et administration
                            </div>
                        </div>
                    </div>
                </div>
                """.formatted(escapeHtml(title), content);
    }

    private String formatDateTime(Inscription inscription) {
        return inscription.getAnimation().getStartTime().format(DATE_TIME_FORMATTER);
    }

    private String normalizeBulletList(String changeSummary) {
        if (changeSummary == null || changeSummary.isBlank()) {
            return "Paramètres du compte enregistrés.";
        }
        return changeSummary.trim().replace("\n- ", "\n- ");
    }

    private record ResendRecipients(List<String> to, List<String> cc) {
    }
}
