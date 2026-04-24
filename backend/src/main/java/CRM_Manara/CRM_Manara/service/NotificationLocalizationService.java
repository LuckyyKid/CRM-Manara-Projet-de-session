package CRM_Manara.CRM_Manara.service;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class NotificationLocalizationService {

    private final RequestLanguageService requestLanguageService;

    public NotificationLocalizationService(RequestLanguageService requestLanguageService) {
        this.requestLanguageService = requestLanguageService;
    }

    public String localize(String value) {
        if (value == null || !requestLanguageService.isEnglish()) {
            return value;
        }

        String exact = exactTranslation(value);
        if (exact != null) {
            return exact;
        }

        for (PatternRule rule : rules()) {
            Matcher matcher = rule.pattern.matcher(value);
            if (matcher.matches()) {
                return rule.translate(matcher);
            }
        }

        return value;
    }

    private String exactTranslation(String value) {
        return switch (value) {
            case "Compte approuvé" -> "Account approved";
            case "Compte désactivé" -> "Account disabled";
            case "Enfant approuvé" -> "Child approved";
            case "Enfant désactivé" -> "Child disabled";
            case "Enfant supprimé" -> "Child deleted";
            case "Inscription approuvée" -> "Registration approved";
            case "Demande refusée" -> "Request rejected";
            case "Présence mise à jour" -> "Attendance updated";
            case "Nouvelle note d'incident" -> "New incident note";
            case "Rendez-vous reporté" -> "Appointment rescheduled";
            case "Nouveau rendez-vous reserve" -> "New appointment booked";
            case "Rendez-vous reserve" -> "Appointment booked";
            case "Rendez-vous annule" -> "Appointment cancelled";
            case "Rendez-vous deplace" -> "Appointment moved";
            case "Nouveau devoir disponible" -> "New homework available";
            case "Nouvelle soumission de devoir" -> "New homework submission";
            case "Nouvelle soumission de quiz" -> "New quiz submission";
            case "Nouvelle pratique maison" -> "New home practice";
            case "Nouveau quiz disponible" -> "New quiz available";
            case "Compte créé" -> "Account created";
            case "Profil mis à jour" -> "Profile updated";
            case "Nouvel enfant ajouté" -> "New child added";
            case "Profil enfant mis à jour" -> "Child profile updated";
            case "Profil enfant supprimé" -> "Child profile deleted";
            case "Demande d'inscription envoyée" -> "Registration request sent";
            case "Nouvel enfant ajouté à votre animation" -> "New child added to your session";
            case "Nouvelle animation assignée" -> "New session assigned";
            case "Animation retirée" -> "Session removed";
            case "Animation mise à jour" -> "Session updated";
            case "Animation annulée" -> "Session cancelled";
            default -> null;
        };
    }

    private List<PatternRule> rules() {
        return List.of(
                rule("^Votre compte parent a été approuvé par l'administration\\.$",
                        matcher -> "Your parent account has been approved by the administration."),
                rule("^Votre compte parent a été désactivé par l'administration\\.$",
                        matcher -> "Your parent account has been disabled by the administration."),
                rule("^([^.]+) est maintenant actif et peut être inscrit aux activités\\.$",
                        matcher -> matcher.group(1) + " is now active and can be registered for activities."),
                rule("^([^.]+) a été désactivé par l'administration\\.$",
                        matcher -> matcher.group(1) + " has been disabled by the administration."),
                rule("^Le profil de (.+) a été supprimé par l'administration\\.$",
                        matcher -> "The profile of " + matcher.group(1) + " was deleted by the administration."),
                rule("^La demande pour (.+) a été approuvée\\. Un reçu de confirmation a été envoyé par email\\.$",
                        matcher -> "The request for " + matcher.group(1) + " has been approved. A confirmation receipt was sent by email."),
                rule("^La demande d'inscription pour (.+) a été refusée\\.$",
                        matcher -> "The registration request for " + matcher.group(1) + " has been rejected."),
                rule("^Une note d'incident a ete ajoutee pour (.+)\\.$",
                        matcher -> "An incident note was added for " + matcher.group(1) + "."),
                rule("^Votre rendez-vous avec (.+) a ete reporte au (.+)\\.$",
                        matcher -> "Your appointment with " + matcher.group(1) + " was rescheduled to " + matcher.group(2) + "."),
                rule("^Le rendez-vous avec (.+) a ete reporte au (.+)\\.$",
                        matcher -> "The appointment with " + matcher.group(1) + " was rescheduled to " + matcher.group(2) + "."),
                rule("^(.+) a reserve un appel avec vous pour le (.+)\\.$",
                        matcher -> matcher.group(1) + " booked a call with you for " + matcher.group(2) + "."),
                rule("^Votre appel avec (.+) est reserve pour le (.+)\\.$",
                        matcher -> "Your call with " + matcher.group(1) + " is booked for " + matcher.group(2) + "."),
                rule("^(.+) a annule le rendez-vous prevu le (.+)\\.$",
                        matcher -> matcher.group(1) + " cancelled the appointment scheduled for " + matcher.group(2) + "."),
                rule("^Le rendez-vous avec (.+) prevu le (.+) a ete annule\\.$",
                        matcher -> "The appointment with " + matcher.group(1) + " scheduled for " + matcher.group(2) + " was cancelled."),
                rule("^(.+) a deplace le rendez-vous au (.+)\\.$",
                        matcher -> matcher.group(1) + " moved the appointment to " + matcher.group(2) + "."),
                rule("^Votre rendez-vous avec (.+) a ete deplace au (.+)\\.$",
                        matcher -> "Your appointment with " + matcher.group(1) + " was moved to " + matcher.group(2) + "."),
                rule("^Un nouveau devoir \"(.+)\" a ete genere pour (.+)\\.$",
                        matcher -> "A new homework \"" + matcher.group(1) + "\" was generated for " + matcher.group(2) + "."),
                rule("^(.+) a soumis le devoir \"(.+)\"\\.$",
                        matcher -> matcher.group(1) + " submitted the homework \"" + matcher.group(2) + "\"."),
                rule("^(.+) a soumis le quiz \"(.+)\"\\.$",
                        matcher -> matcher.group(1) + " submitted the quiz \"" + matcher.group(2) + "\"."),
                rule("^Une nouvelle fiche \"(.+)\" est disponible(.*)$",
                        matcher -> "A new guide \"" + matcher.group(1) + "\" is available" + matcher.group(2)),
                rule("^Un nouveau quiz \"(.+)\" est disponible(.*)$",
                        matcher -> "A new quiz \"" + matcher.group(1) + "\" is available" + matcher.group(2)),
                rule("^Votre compte parent a ete cree avec Google\\. Il est en attente d'approbation par l'administration\\.$",
                        matcher -> "Your parent account was created with Google. It is pending approval by the administration."),
                rule("^Votre compte parent a été créé\\. Il est maintenant en attente d'approbation par l'administration\\.$",
                        matcher -> "Your parent account was created. It is now pending approval by the administration."),
                rule("^Vos informations de profil ont été mises à jour avec succès\\.$",
                        matcher -> "Your profile information has been updated successfully."),
                rule("^(.+) a été ajouté\\. Son profil est en attente d'approbation par l'administration\\.$",
                        matcher -> matcher.group(1) + " was added. Their profile is pending approval by the administration."),
                rule("^Le profil de (.+) a été mis à jour\\.$",
                        matcher -> "The profile of " + matcher.group(1) + " has been updated."),
                rule("^Le profil de (.+) a été supprimé de votre compte\\.$",
                        matcher -> "The profile of " + matcher.group(1) + " was removed from your account."),
                rule("^Une nouvelle animation vous a été assignée : (.+) du (.+) au (.+)\\.$",
                        matcher -> "A new session has been assigned to you: " + matcher.group(1) + " from " + matcher.group(2) + " to " + matcher.group(3) + "."),
                rule("^L'animation (.+) du (.+) au (.+) ne vous est plus assignée\\.$",
                        matcher -> "The session " + matcher.group(1) + " from " + matcher.group(2) + " to " + matcher.group(3) + " is no longer assigned to you."),
                rule("^L'animation (.+) que vous animez a été mise à jour\\. Nouveau créneau : (.+) à (.+)\\.$",
                        matcher -> "The session " + matcher.group(1) + " that you lead has been updated. New time slot: " + matcher.group(2) + " to " + matcher.group(3) + "."),
                rule("^L'animation (.+) du (.+) a été supprimée par l'administration\\.$",
                        matcher -> "The session " + matcher.group(1) + " on " + matcher.group(2) + " was deleted by the administration.")
        );
    }

    private PatternRule rule(String expression, java.util.function.Function<Matcher, String> translator) {
        return new PatternRule(Pattern.compile(expression), translator);
    }

    private record PatternRule(Pattern pattern, java.util.function.Function<Matcher, String> translator) {
        String translate(Matcher matcher) {
            return translator.apply(matcher);
        }
    }
}
