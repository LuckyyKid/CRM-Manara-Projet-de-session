package CRM_Manara.CRM_Manara.Controller;

import CRM_Manara.CRM_Manara.Model.Entity.Animateur;
import CRM_Manara.CRM_Manara.Model.Entity.Animation;
import CRM_Manara.CRM_Manara.Model.Entity.Inscription;
import CRM_Manara.CRM_Manara.Model.Entity.Enum.PresenceStatus;
import CRM_Manara.CRM_Manara.service.AnimateurService;
import CRM_Manara.CRM_Manara.service.AnimateurNotificationService;
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

@Profile("thymeleaf")
@Controller
@RequestMapping("/animateur")
public class animateurController {

    @Autowired
    private AnimateurService animateurService;

    @Autowired
    private AnimateurNotificationService animateurNotificationService;

    @GetMapping("/dashboard")
    public String dashboard(Model model, Principal principal) {
        String email = principal.getName();
        Animateur animateur = animateurService.getAnimateurByEmail(email);
        List<Animation> animations = animateurService.getAnimationsForAnimateur(animateur.getId());
        List<Inscription> inscriptions = animateurService.getInscriptionsForAnimateur(animateur.getId());
        model.addAttribute("animateur", animateur);
        model.addAttribute("animations", animations);
        model.addAttribute("countAnimations", animations.size());
        model.addAttribute("countInscriptions", inscriptions.size());
        model.addAttribute("upcomingAnimations", animateurService.getUpcomingAnimationsForAnimateur(animateur.getId(), 5));
        model.addAttribute("latestNotifications", animateurNotificationService.getNotificationsForAnimateur(animateur.getId(), 5));
        return "animateur/animateurDashboard";
    }

    @GetMapping("/notifications")
    public String notifications(Model model, Principal principal) {
        Animateur animateur = animateurNotificationService.getAnimateurByUserEmail(principal.getName());
        model.addAttribute("notifications", animateurNotificationService.getNotificationsForAnimateur(animateur.getId(), 100));
        model.addAttribute("archivedNotifications", animateurNotificationService.getArchivedNotificationsForAnimateur(animateur.getId(), 100));
        return "animateur/animateurNotifications";
    }

    @PostMapping("/notifications/read-all")
    public String readAllNotifications(Principal principal, RedirectAttributes redirectAttributes) {
        Animateur animateur = animateurNotificationService.getAnimateurByUserEmail(principal.getName());
        animateurNotificationService.markAllAsReadForAnimateur(animateur.getId());
        redirectAttributes.addFlashAttribute("message", "Notifications marquées comme lues.");
        return "redirect:/animateur/notifications";
    }

    @PostMapping("/notifications/{id}/read")
    public String readNotification(@PathVariable("id") Long id,
                                   Principal principal,
                                   RedirectAttributes redirectAttributes) {
        Animateur animateur = animateurNotificationService.getAnimateurByUserEmail(principal.getName());
        animateurNotificationService.markAsRead(animateur.getId(), id);
        redirectAttributes.addFlashAttribute("message", "Notification marquée comme lue.");
        return "redirect:/animateur/notifications";
    }

    @PostMapping("/notifications/{id}/archive")
    public String archiveNotification(@PathVariable("id") Long id,
                                      Principal principal,
                                      RedirectAttributes redirectAttributes) {
        Animateur animateur = animateurNotificationService.getAnimateurByUserEmail(principal.getName());
        animateurNotificationService.archive(animateur.getId(), id);
        redirectAttributes.addFlashAttribute("message", "Notification archivée.");
        return "redirect:/animateur/notifications";
    }

    @PostMapping("/notifications/{id}/restore")
    public String restoreNotification(@PathVariable("id") Long id,
                                      Principal principal,
                                      RedirectAttributes redirectAttributes) {
        Animateur animateur = animateurNotificationService.getAnimateurByUserEmail(principal.getName());
        animateurNotificationService.restore(animateur.getId(), id);
        redirectAttributes.addFlashAttribute("message", "Notification restaurée.");
        return "redirect:/animateur/notifications";
    }

    @GetMapping("/inscriptions")
    public String inscriptions(@RequestParam(value = "date", required = false)
                               @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                               Model model,
                               Principal principal) {
        String email = principal.getName();
        Animateur animateur = animateurService.getAnimateurByEmail(email);
        LocalDate selectedDate = date == null ? LocalDate.now() : date;
        LocalDate weekStart = selectedDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate weekEnd = weekStart.plusDays(6);
        List<Animation> weekAnimations = animateurService.getAnimationsForAnimateurWeek(email, weekStart, weekEnd);
        int scheduleStartHour = resolveScheduleStartHour(weekAnimations);
        int scheduleEndHour = resolveScheduleEndHour(weekAnimations, scheduleStartHour);

        model.addAttribute("animateur", animateur);
        model.addAttribute("weekStart", weekStart);
        model.addAttribute("weekEnd", weekEnd);
        model.addAttribute("prevWeek", weekStart.minusWeeks(1));
        model.addAttribute("nextWeek", weekStart.plusWeeks(1));
        model.addAttribute("scheduleHours", buildScheduleHours(scheduleStartHour, scheduleEndHour));
        model.addAttribute("scheduleBodyHeight", (scheduleEndHour - scheduleStartHour) * 88);
        model.addAttribute("weekDays", buildPlanningWeek(weekStart, weekAnimations, scheduleStartHour));
        return "animateur/animateurInscriptions";
    }

    @GetMapping("/animations/{id}/presence")
    public String animationPresence(@PathVariable("id") Long id,
                                    Model model,
                                    Principal principal) {
        String email = principal.getName();
        Animation animation = animateurService.getAnimationForAnimateur(id, email);
        List<Inscription> inscriptions = animateurService.getInscriptionsForAnimation(id, email);
        model.addAttribute("animation", animation);
        model.addAttribute("inscriptions", inscriptions);
        model.addAttribute("presenceStatuses", PresenceStatus.values());
        return "animateur/animateurPresenceDetail";
    }

    @PostMapping("/inscriptions/{id}/presence")
    public String updatePresence(@PathVariable("id") Long id,
                                 @RequestParam("presenceStatus") PresenceStatus presenceStatus,
                                 @RequestParam(name = "incidentNote", required = false) String incidentNote,
                                 @RequestParam(name = "redirectTo", required = false) String redirectTo,
                                 Principal principal,
                                 RedirectAttributes redirectAttributes) {
        String email = principal.getName();
        animateurService.updatePresence(id, email, presenceStatus, incidentNote);
        redirectAttributes.addFlashAttribute("message", "Présence mise à jour.");
        if (redirectTo != null && !redirectTo.isBlank() && redirectTo.startsWith("/animateur/")) {
            return "redirect:" + redirectTo;
        }
        return "redirect:/animateur/inscriptions";
    }

    @PostMapping("/api/inscriptions/{id}/presence")
    @ResponseBody
    public Map<String, Object> updatePresenceAjax(@PathVariable("id") Long id,
                                                  @RequestParam("presenceStatus") PresenceStatus presenceStatus,
                                                  @RequestParam(name = "incidentNote", required = false) String incidentNote,
                                                  Principal principal) {
        String email = principal.getName();
        Map<String, Object> response = new LinkedHashMap<>();
        try {
            animateurService.updatePresence(id, email, presenceStatus, incidentNote);
            response.put("success", true);
            response.put("message", "Présence mise à jour.");
            response.put("presenceStatus", presenceStatus.name());
            response.put("incidentNote", incidentNote == null ? "" : incidentNote.trim());
        } catch (IllegalArgumentException exception) {
            response.put("success", false);
            response.put("message", exception.getMessage());
        }
        return response;
    }

    private int resolveScheduleStartHour(List<Animation> animations) {
        int earliestHour = 8;
        for (Animation animation : animations) {
            if (animation.getStartTime() == null) {
                continue;
            }
            earliestHour = Math.min(earliestHour, animation.getStartTime().getHour());
        }
        return earliestHour;
    }

    private int resolveScheduleEndHour(List<Animation> animations, int scheduleStartHour) {
        int latestHour = Math.max(scheduleStartHour + 1, 18);
        for (Animation animation : animations) {
            if (animation.getEndTime() == null) {
                continue;
            }
            int endHour = animation.getEndTime().getHour();
            if (animation.getEndTime().getMinute() > 0) {
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

    private List<Map<String, Object>> buildPlanningWeek(LocalDate weekStart, List<Animation> animations, int scheduleStartHour) {
        Map<LocalDate, List<Animation>> animationsByDate = new LinkedHashMap<>();
        for (Animation animation : animations) {
            LocalDate date = animation.getStartTime().toLocalDate();
            animationsByDate.computeIfAbsent(date, key -> new ArrayList<>()).add(animation);
        }

        List<Map<String, Object>> week = new ArrayList<>();
        LocalDate cursor = weekStart;
        for (int i = 0; i < 7; i++) {
            List<Animation> dayAnimations = new ArrayList<>(animationsByDate.getOrDefault(cursor, Collections.emptyList()));
            dayAnimations.sort((left, right) -> left.getStartTime().compareTo(right.getStartTime()));

            Map<String, Object> day = new LinkedHashMap<>();
            day.put("date", cursor);
            day.put("blocks", buildScheduleBlocks(dayAnimations, scheduleStartHour));
            week.add(day);
            cursor = cursor.plusDays(1);
        }
        return week;
    }

    private List<Map<String, Object>> buildScheduleBlocks(List<Animation> animations, int scheduleStartHour) {
        List<Map<String, Object>> blocks = new ArrayList<>();
        LocalTime scheduleStart = LocalTime.of(scheduleStartHour, 0);
        int index = 0;
        for (Animation animation : animations) {
            LocalDateTime startTime = animation.getStartTime();
            LocalDateTime endTime = animation.getEndTime();
            long minutesFromStart = java.time.Duration.between(scheduleStart, startTime.toLocalTime()).toMinutes();
            long durationMinutes = Math.max(30, java.time.Duration.between(startTime, endTime).toMinutes());

            Map<String, Object> block = new LinkedHashMap<>();
            block.put("title", animation.getActivity().getActivyName());
            block.put("time", String.format("%s - %s",
                    startTime.toLocalTime().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")),
                    endTime.toLocalTime().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))));
            block.put("top", (minutesFromStart * 88.0) / 60.0);
            block.put("height", Math.max(68.0, (durationMinutes * 88.0) / 60.0));
            block.put("variant", index % 2 == 0 ? "primary" : "secondary");
            block.put("url", "/animateur/animations/" + animation.getId() + "/presence");
            blocks.add(block);
            index++;
        }
        return blocks;
    }

}
