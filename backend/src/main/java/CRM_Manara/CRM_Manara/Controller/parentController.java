package CRM_Manara.CRM_Manara.Controller;

import CRM_Manara.CRM_Manara.Model.Entity.Activity;
import CRM_Manara.CRM_Manara.Model.Entity.Animation;
import CRM_Manara.CRM_Manara.Model.Entity.Enfant;
import CRM_Manara.CRM_Manara.Model.Entity.Inscription;
import CRM_Manara.CRM_Manara.Model.Entity.Parent;
import CRM_Manara.CRM_Manara.service.parentService;
import CRM_Manara.CRM_Manara.Repository.ParentRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.sql.Date;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Profile("thymeleaf")
@Controller
@RequestMapping("/parent")
public class parentController {

    @Autowired
    private parentService parentService;
    @Autowired
    ParentRepo parentRepo;

    @GetMapping("/dashboard")
    public String parentpage(Model model, Principal principal) {
        String email = principal.getName();
        model.addAttribute("countEnfants", parentService.countEnfantsForParent(email));
        model.addAttribute("countInscriptions", parentService.countInscriptionsForParent(email));
        model.addAttribute("upcomingInscriptions", parentService.getUpcomingInscriptionsForParent(email, 5));
        model.addAttribute("latestNotifications", parentService.getNotificationsForParent(email, 5));
        model.addAttribute("unreadNotifications", parentService.countUnreadNotificationsForParent(email));
        return "parent/parentDashboard";
    }

    @GetMapping("/profil")
    public String profil() {
        return "redirect:/settings";
    }

    @GetMapping("/notifications")
    public String notifications(Model model, Principal principal) {
        String email = principal.getName();
        model.addAttribute("notifications", parentService.getNotificationsForParent(email, 100));
        model.addAttribute("archivedNotifications", parentService.getArchivedNotificationsForParent(email, 100));
        model.addAttribute("unreadNotifications", parentService.countUnreadNotificationsForParent(email));
        return "parent/parentNotifications";
    }

    @PostMapping("/notifications/read-all")
    public String readAllNotifications(Principal principal, RedirectAttributes redirectAttributes) {
        String email = principal.getName();
        parentService.markNotificationsAsRead(email);
        redirectAttributes.addFlashAttribute("message", "Notifications marquées comme lues.");
        return "redirect:/parent/notifications";
    }

    @PostMapping("/notifications/{id}/read")
    public String readNotification(@PathVariable("id") Long id,
                                   Principal principal,
                                   RedirectAttributes redirectAttributes) {
        parentService.markNotificationAsRead(principal.getName(), id);
        redirectAttributes.addFlashAttribute("message", "Notification marquée comme lue.");
        return "redirect:/parent/notifications";
    }

    @PostMapping("/notifications/{id}/archive")
    public String archiveNotification(@PathVariable("id") Long id,
                                      Principal principal,
                                      RedirectAttributes redirectAttributes) {
        parentService.archiveNotification(principal.getName(), id);
        redirectAttributes.addFlashAttribute("message", "Notification archivée.");
        return "redirect:/parent/notifications";
    }

    @PostMapping("/notifications/{id}/restore")
    public String restoreNotification(@PathVariable("id") Long id,
                                      Principal principal,
                                      RedirectAttributes redirectAttributes) {
        parentService.restoreNotification(principal.getName(), id);
        redirectAttributes.addFlashAttribute("message", "Notification restaurée.");
        return "redirect:/parent/notifications";
    }

    @PostMapping("/profil")
    public String updateProfil(@RequestParam("nom") String nom,
                               @RequestParam("prenom") String prenom,
                               @RequestParam("adresse") String adresse,
                               Principal principal,
                               RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("message", "Utilisez désormais la page Paramètres.");
        return "redirect:/settings";
    }

    @GetMapping("/enfants")
    public String listEnfants(Model model, Principal principal) {
        String email = principal.getName();
        List<Enfant> enfants = parentService.getEnfantsForParent(email);
        model.addAttribute("enfants", enfants);
        return "parent/parentEnfants";
    }

    @GetMapping("/enfants/new")
    public String newEnfantForm(Model model) {
        model.addAttribute("errors", new LinkedHashMap<String, String>());
        model.addAttribute("formData", new LinkedHashMap<String, String>());
        return "parent/parentEnfantNew";
    }

    @PostMapping("/enfants")
    public String createEnfant(@RequestParam("nom") String nom,
                               @RequestParam("prenom") String prenom,
                               @RequestParam("dateNaissance") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) java.time.LocalDate dateNaissance,
                               Principal principal,
                               RedirectAttributes redirectAttributes,
                               Model model) {
        Map<String, String> errors = validateEnfantInputs(nom, prenom, dateNaissance);
        if (!errors.isEmpty()) {
            model.addAttribute("errors", errors);
            model.addAttribute("formData", buildEnfantFormData(nom, prenom, dateNaissance));
            return "parent/parentEnfantNew";
        }
        String email = principal.getName();
        parentService.createEnfantForParent(email, nom, prenom, Date.valueOf(dateNaissance));
        redirectAttributes.addFlashAttribute("message", "Enfant créé avec succès.");
        return "redirect:/parent/enfants";
    }

    @GetMapping("/enfants/{id}/edit")
    public String editEnfantForm(@PathVariable("id") Long id, Principal principal, Model model) {
        String email = principal.getName();
        Enfant enfant = parentService.getEnfantForParent(id, email);
        model.addAttribute("enfant", enfant);
        model.addAttribute("errors", new LinkedHashMap<String, String>());
        return "parent/parentEnfantEdit";
    }

    @PostMapping("/enfants/{id}/edit")
    public String updateEnfant(@PathVariable("id") Long id,
                               @RequestParam("nom") String nom,
                               @RequestParam("prenom") String prenom,
                               @RequestParam("dateNaissance") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) java.time.LocalDate dateNaissance,
                               Principal principal,
                               RedirectAttributes redirectAttributes,
                               Model model) {
        String email = principal.getName();
        Map<String, String> errors = validateEnfantInputs(nom, prenom, dateNaissance);
        if (!errors.isEmpty()) {
            Enfant enfant = parentService.getEnfantForParent(id, email);
            enfant.setNom(nom);
            enfant.setPrenom(prenom);
            if (dateNaissance != null) {
                enfant.setDate_de_naissance(Date.valueOf(dateNaissance));
            }
            model.addAttribute("enfant", enfant);
            model.addAttribute("errors", errors);
            return "parent/parentEnfantEdit";
        }
        parentService.updateEnfantForParent(id, email, nom, prenom, Date.valueOf(dateNaissance));
        redirectAttributes.addFlashAttribute("message", "Enfant mis à jour.");
        return "redirect:/parent/enfants";
    }

    @PostMapping("/enfants/{id}/delete")
    public String deleteEnfant(@PathVariable("id") Long id, Principal principal, RedirectAttributes redirectAttributes) {
        String email = principal.getName();
        long count = parentService.countInscriptionsForEnfant(id);
        parentService.deleteEnfantForParent(id, email);
        if (count > 0) {
            redirectAttributes.addFlashAttribute("message", "Enfant supprimé (" + count + " inscriptions supprimées).");
        } else {
            redirectAttributes.addFlashAttribute("message", "Enfant supprimé.");
        }
        return "redirect:/parent/enfants";
    }

    @PostMapping("/api/enfants")
    @ResponseBody
    public Map<String, Object> createEnfantAjax(@RequestParam("nom") String nom,
                                                @RequestParam("prenom") String prenom,
                                                @RequestParam("dateNaissance") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) java.time.LocalDate dateNaissance,
                                                Principal principal) {
        Map<String, String> errors = validateEnfantInputs(nom, prenom, dateNaissance);
        Map<String, Object> response = new LinkedHashMap<>();
        if (!errors.isEmpty()) {
            response.put("success", false);
            response.put("message", "Veuillez corriger les champs invalides.");
            response.put("errors", errors);
            return response;
        }
        String email = principal.getName();
        Enfant enfant = parentService.createEnfantForParent(email, nom, prenom, Date.valueOf(dateNaissance));
        response.put("success", true);
        response.put("message", "Enfant créé avec succès.");
        response.put("id", enfant.getId());
        return response;
    }

    @PostMapping("/api/enfants/{id}/edit")
    @ResponseBody
    public Map<String, Object> updateEnfantAjax(@PathVariable("id") Long id,
                                                @RequestParam("nom") String nom,
                                                @RequestParam("prenom") String prenom,
                                                @RequestParam("dateNaissance") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) java.time.LocalDate dateNaissance,
                                                Principal principal) {
        Map<String, String> errors = validateEnfantInputs(nom, prenom, dateNaissance);
        Map<String, Object> response = new LinkedHashMap<>();
        if (!errors.isEmpty()) {
            response.put("success", false);
            response.put("message", "Veuillez corriger les champs invalides.");
            response.put("errors", errors);
            return response;
        }
        String email = principal.getName();
        parentService.updateEnfantForParent(id, email, nom, prenom, Date.valueOf(dateNaissance));
        response.put("success", true);
        response.put("message", "Enfant mis à jour.");
        return response;
    }

    @PostMapping("/api/enfants/{id}/delete")
    @ResponseBody
    public Map<String, Object> deleteEnfantAjax(@PathVariable("id") Long id, Principal principal) {
        String email = principal.getName();
        long count = parentService.countInscriptionsForEnfant(id);
        parentService.deleteEnfantForParent(id, email);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("id", id);
        response.put("message", count > 0
                ? "Enfant supprimé (" + count + " inscriptions supprimées)."
                : "Enfant supprimé.");
        return response;
    }

    @GetMapping("/activities")
    public String listActivities(Model model, Principal principal) {
        String email = principal.getName();
        List<Activity> activities = parentService.getAllActivities();
        Map<Long, List<Animation>> animationsByActivity = new LinkedHashMap<>();
        for (Activity activity : activities) {
            animationsByActivity.put(activity.getId(), parentService.getAnimationsForActivity(activity.getId()));
        }
        List<Enfant> enfants = parentService.getActiveEnfantsForParent(email);
        List<Inscription> parentInscriptions = parentService.getInscriptionsForParent(email);
        model.addAttribute("activities", activities);
        model.addAttribute("animationsByActivity", animationsByActivity);
        model.addAttribute("animationCapacity", parentService.getAnimationCapacitySnapshotsForActivities(activities));
        model.addAttribute("animationChildren", buildAnimationChildren(parentInscriptions));
        model.addAttribute("enfants", enfants);
        return "parent/parentActivities";
    }

    @PostMapping("/inscriptions")
    public String inscrireEnfant(@RequestParam("enfantId") Long enfantId,
                                 @RequestParam("animationId") Long animationId,
                                 Principal principal,
                                 RedirectAttributes redirectAttributes) {
        String email = principal.getName();
        parentService.inscrireEnfant(enfantId, animationId, email);
        redirectAttributes.addFlashAttribute("message", "Demande d'inscription envoyee. En attente de validation par l'administration.");
        return "redirect:/parent/planning";
    }

    @PostMapping("/api/inscriptions")
    @ResponseBody
    public Map<String, Object> inscrireEnfantAjax(@RequestParam("enfantId") Long enfantId,
                                                  @RequestParam("animationId") Long animationId,
                                                  Principal principal) {
        String email = principal.getName();
        Map<String, Object> response = new LinkedHashMap<>();
        try {
            Inscription inscription = parentService.inscrireEnfant(enfantId, animationId, email);
            response.put("success", true);
            response.put("message", "Demande d'inscription envoyee. Consultez vos notifications pour le suivi.");
            response.put("inscriptionId", inscription.getId());
        } catch (IllegalArgumentException exception) {
            response.put("success", false);
            response.put("message", exception.getMessage());
        }
        return response;
    }

    @GetMapping("/planning")
    public String planning(@RequestParam(value = "date", required = false)
                           @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                           Model model,
                           Principal principal) {
        String email = principal.getName();
        List<Inscription> inscriptions = parentService.getInscriptionsForParent(email);
        LocalDate selectedDate = resolvePlanningDate(date);
        LocalDate weekStart = selectedDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate weekEnd = weekStart.plusDays(6);
        List<Inscription> weekInscriptions = filterInscriptionsForWeek(inscriptions, weekStart, weekEnd);
        int scheduleStartHour = resolveScheduleStartHour(weekInscriptions);
        int scheduleEndHour = resolveScheduleEndHour(weekInscriptions, scheduleStartHour);

        model.addAttribute("selectedDate", selectedDate);
        model.addAttribute("weekStart", weekStart);
        model.addAttribute("weekEnd", weekEnd);
        model.addAttribute("prevWeek", weekStart.minusWeeks(1));
        model.addAttribute("nextWeek", weekStart.plusWeeks(1));
        model.addAttribute("scheduleHours", buildScheduleHours(scheduleStartHour, scheduleEndHour));
        model.addAttribute("scheduleBodyHeight", (scheduleEndHour - scheduleStartHour) * 88);
        model.addAttribute("animationChildren", buildAnimationChildren(weekInscriptions));
        model.addAttribute("weekDays", buildPlanningWeek(weekStart, weekInscriptions, scheduleStartHour));
        model.addAttribute("inscriptions", weekInscriptions);
        return "parent/parentPlanning";
    }

    private LocalDate resolvePlanningDate(LocalDate date) {
        return date == null ? LocalDate.now() : date;
    }

    private List<Map<String, Object>> buildPlanningWeek(LocalDate weekStart, List<Inscription> inscriptions, int scheduleStartHour) {
        Map<LocalDate, List<Inscription>> inscriptionsByDate = new LinkedHashMap<>();
        for (Inscription inscription : inscriptions) {
            if (inscription.getAnimation() == null || inscription.getAnimation().getStartTime() == null) {
                continue;
            }
            LocalDate date = inscription.getAnimation().getStartTime().toLocalDate();
            inscriptionsByDate.computeIfAbsent(date, key -> new ArrayList<>()).add(inscription);
        }

        List<Map<String, Object>> week = new ArrayList<>();
        LocalDate cursor = weekStart;
        for (int i = 0; i < 7; i++) {
            List<Inscription> dayInscriptions = new ArrayList<>(inscriptionsByDate.getOrDefault(cursor, Collections.emptyList()));
            dayInscriptions.sort((left, right) -> left.getAnimation().getStartTime().compareTo(right.getAnimation().getStartTime()));

            Map<String, Object> day = new LinkedHashMap<>();
            day.put("date", cursor);
            day.put("blocks", buildScheduleBlocks(dayInscriptions, scheduleStartHour));
            week.add(day);
            cursor = cursor.plusDays(1);
        }
        return week;
    }

    private List<Inscription> filterInscriptionsForWeek(List<Inscription> inscriptions, LocalDate weekStart, LocalDate weekEnd) {
        List<Inscription> weekInscriptions = new ArrayList<>();
        for (Inscription inscription : inscriptions) {
            if (inscription.getAnimation() == null || inscription.getAnimation().getStartTime() == null) {
                continue;
            }
            LocalDate date = inscription.getAnimation().getStartTime().toLocalDate();
            if (!date.isBefore(weekStart) && !date.isAfter(weekEnd)) {
                weekInscriptions.add(inscription);
            }
        }
        weekInscriptions.sort((left, right) -> left.getAnimation().getStartTime().compareTo(right.getAnimation().getStartTime()));
        return weekInscriptions;
    }

    private int resolveScheduleStartHour(List<Inscription> inscriptions) {
        int earliestHour = 8;
        for (Inscription inscription : inscriptions) {
            if (inscription.getAnimation() == null || inscription.getAnimation().getStartTime() == null) {
                continue;
            }
            int currentHour = inscription.getAnimation().getStartTime().getHour();
            earliestHour = Math.min(earliestHour, currentHour);
        }
        return earliestHour;
    }

    private int resolveScheduleEndHour(List<Inscription> inscriptions, int scheduleStartHour) {
        int latestHour = Math.max(scheduleStartHour + 1, 18);
        for (Inscription inscription : inscriptions) {
            if (inscription.getAnimation() == null || inscription.getAnimation().getEndTime() == null) {
                continue;
            }
            LocalDateTime endTime = inscription.getAnimation().getEndTime();
            int endHour = endTime.getHour();
            if (endTime.getMinute() > 0) {
                endHour += 1;
            }
            latestHour = Math.max(latestHour, endHour);
        }
        return latestHour;
    }

    private List<String> buildScheduleHours(int scheduleStartHour, int scheduleEndHour) {
        List<String> hours = new ArrayList<>();
        for (int hour = scheduleStartHour; hour < scheduleEndHour; hour++) {
            hours.add(String.format("%02d:00", hour));
        }
        return hours;
    }

    private List<Map<String, Object>> buildScheduleBlocks(List<Inscription> inscriptions, int scheduleStartHour) {
        List<Map<String, Object>> blocks = new ArrayList<>();
        LocalTime scheduleStart = LocalTime.of(scheduleStartHour, 0);
        int index = 0;
        for (Inscription inscription : inscriptions) {
            if (inscription.getAnimation() == null
                    || inscription.getAnimation().getStartTime() == null
                    || inscription.getAnimation().getEndTime() == null) {
                continue;
            }

            LocalDateTime startTime = inscription.getAnimation().getStartTime();
            LocalDateTime endTime = inscription.getAnimation().getEndTime();
            long minutesFromStart = java.time.Duration.between(scheduleStart, startTime.toLocalTime()).toMinutes();
            long durationMinutes = Math.max(30, java.time.Duration.between(startTime, endTime).toMinutes());

            Map<String, Object> block = new LinkedHashMap<>();
            block.put("title", inscription.getAnimation().getActivity().getActivyName());
            block.put("child", inscription.getEnfant().getPrenom() + " " + inscription.getEnfant().getNom());
            block.put("time", String.format("%s - %s",
                    startTime.toLocalTime().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")),
                    endTime.toLocalTime().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))));
            block.put("animationId", inscription.getAnimation().getId());
            block.put("description", inscription.getAnimation().getActivity().getDescription());
            block.put("ageRange", inscription.getAnimation().getActivity().getAgeMin() + " à " + inscription.getAnimation().getActivity().getAgeMax() + " ans");
            block.put("status", CRM_Manara.CRM_Manara.service.UiLabelService.inscriptionStatus(inscription.getStatusInscription()));
            block.put("animateur", resolveAnimateurName(inscription.getAnimation()));
            block.put("startLabel", startTime.format(java.time.format.DateTimeFormatter.ofPattern("EEEE d MMMM yyyy 'à' HH:mm")));
            block.put("endLabel", endTime.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")));
            block.put("top", (minutesFromStart * 88.0) / 60.0);
            block.put("height", Math.max(68.0, (durationMinutes * 88.0) / 60.0));
            block.put("variant", index % 2 == 0 ? "primary" : "secondary");
            blocks.add(block);
            index++;
        }
        return blocks;
    }

    private Map<String, String> validateEnfantInputs(String nom, String prenom, java.time.LocalDate dateNaissance) {
        Map<String, String> errors = new LinkedHashMap<>();
        if (nom == null || nom.trim().isEmpty()) {
            errors.put("nom", "Le nom est obligatoire.");
        }
        if (prenom == null || prenom.trim().isEmpty()) {
            errors.put("prenom", "Le prénom est obligatoire.");
        }
        if (dateNaissance == null) {
            errors.put("dateNaissance", "La date de naissance est obligatoire.");
        } else if (dateNaissance.isAfter(java.time.LocalDate.now())) {
            errors.put("dateNaissance", "La date de naissance ne peut pas être dans le futur.");
        }
        return errors;
    }

    private Map<String, String> buildEnfantFormData(String nom, String prenom, java.time.LocalDate dateNaissance) {
        Map<String, String> formData = new LinkedHashMap<>();
        formData.put("nom", nom == null ? "" : nom);
        formData.put("prenom", prenom == null ? "" : prenom);
        formData.put("dateNaissance", dateNaissance == null ? "" : dateNaissance.toString());
        return formData;
    }

    private Map<Long, List<Map<String, String>>> buildAnimationChildren(List<Inscription> inscriptions) {
        return inscriptions.stream()
                .filter(inscription -> inscription.getAnimation() != null && inscription.getAnimation().getId() != null)
                .collect(Collectors.groupingBy(
                        inscription -> inscription.getAnimation().getId(),
                        LinkedHashMap::new,
                        Collectors.mapping(inscription -> {
                            Map<String, String> child = new LinkedHashMap<>();
                            child.put("name", inscription.getEnfant().getPrenom() + " " + inscription.getEnfant().getNom());
                            child.put("status", CRM_Manara.CRM_Manara.service.UiLabelService.inscriptionStatus(inscription.getStatusInscription()));
                            return child;
                        }, Collectors.toList())
                ));
    }

    private String resolveAnimateurName(Animation animation) {
        if (animation.getAnimateur() == null) {
            return "Animateur à confirmer";
        }
        return animation.getAnimateur().getPrenom() + " " + animation.getAnimateur().getNom();
    }
}
