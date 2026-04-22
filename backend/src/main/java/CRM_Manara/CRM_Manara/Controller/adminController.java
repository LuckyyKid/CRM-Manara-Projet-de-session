package CRM_Manara.CRM_Manara.Controller;

import CRM_Manara.CRM_Manara.Model.Entity.Activity;
import CRM_Manara.CRM_Manara.Model.Entity.Animateur;
import CRM_Manara.CRM_Manara.Model.Entity.Animation;
import CRM_Manara.CRM_Manara.Model.Entity.Inscription;
import CRM_Manara.CRM_Manara.Model.Entity.Parent;
import CRM_Manara.CRM_Manara.Model.Entity.Enum.AnimationRole;
import CRM_Manara.CRM_Manara.Model.Entity.Enum.animationStatus;
import CRM_Manara.CRM_Manara.Model.Entity.Enum.status;
import CRM_Manara.CRM_Manara.Model.Entity.Enum.typeActivity;
import CRM_Manara.CRM_Manara.service.AdminService;
import CRM_Manara.CRM_Manara.service.AdminNotificationService;
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

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Profile("thymeleaf")
@Controller
@RequestMapping("/admin")
public class adminController {

    @Autowired
    private AdminService adminService;

    @Autowired
    private AdminNotificationService adminNotificationService;

    @GetMapping("/adminDashboard")
    public String adminDashboard(Model model) {
        model.addAttribute("countActivities", adminService.countActivities());
        model.addAttribute("countAnimations", adminService.countAnimations());
        model.addAttribute("countAnimateurs", adminService.countAnimateurs());
        model.addAttribute("countInscriptions", adminService.countInscriptions());
        model.addAttribute("countOpenActivities", adminService.countOpenActivities());
        model.addAttribute("countPendingInscriptions", adminService.countPendingInscriptions());
        model.addAttribute("countWaitlistEntries", adminService.countWaitlistEntries());
        model.addAttribute("countActiveParents", adminService.countActiveParents());
        model.addAttribute("countPendingChildren", adminService.countPendingChildren());
        model.addAttribute("averageFillRate", adminService.getAverageFillRate());
        model.addAttribute("recentInscriptions", adminService.getRecentInscriptions(5));
        model.addAttribute("upcomingAnimations", adminService.getUpcomingAnimations(5));
        model.addAttribute("recentAdminNotifications", adminNotificationService.getRecent(5));
        return "admin/adminDashboard";
    }

    @GetMapping("/activities")
    public String activities(Model model) {
        List<Activity> activities = adminService.getAllActivities();
        model.addAttribute("activities", activities);
        return "admin/adminActivities";
    }

    @GetMapping("/activities/new")
    public String newActivity(Model model) {
        model.addAttribute("statuses", status.values());
        model.addAttribute("types", typeActivity.values());
        model.addAttribute("errors", new LinkedHashMap<String, String>());
        model.addAttribute("formData", new LinkedHashMap<String, String>());
        return "admin/adminActivityNew";
    }

    @PostMapping("/activities")
    public String createActivity(@RequestParam("name") String name,
                                 @RequestParam("description") String description,
                                 @RequestParam("ageMin") int ageMin,
                                 @RequestParam("ageMax") int ageMax,
                                 @RequestParam("capacity") int capacity,
                                 @RequestParam("status") status status,
                                 @RequestParam("type") typeActivity type,
                                 RedirectAttributes redirectAttributes,
                                 Model model) {
        Map<String, String> errors = validateActivityInputs(name, description, ageMin, ageMax, capacity);
        if (!errors.isEmpty()) {
            model.addAttribute("statuses", status.values());
            model.addAttribute("types", typeActivity.values());
            model.addAttribute("errors", errors);
            model.addAttribute("formData", buildActivityFormData(name, description, ageMin, ageMax, capacity, status, type));
            return "admin/adminActivityNew";
        }
        adminService.createActivity(name, description, null, ageMin, ageMax, capacity, status, type);
        redirectAttributes.addFlashAttribute("message", "Activité créée.");
        return "redirect:/admin/activities";
    }

    @GetMapping("/activities/{id}/edit")
    public String editActivity(@PathVariable("id") Long id, Model model) {
        Activity activity = adminService.getActivityById(id);
        model.addAttribute("activity", activity);
        model.addAttribute("statuses", status.values());
        model.addAttribute("types", typeActivity.values());
        model.addAttribute("errors", new LinkedHashMap<String, String>());
        return "admin/adminActivityEdit";
    }

    @PostMapping("/activities/{id}/edit")
    public String updateActivity(@PathVariable("id") Long id,
                                 @RequestParam("name") String name,
                                 @RequestParam("description") String description,
                                 @RequestParam("ageMin") int ageMin,
                                 @RequestParam("ageMax") int ageMax,
                                 @RequestParam("capacity") int capacity,
                                 @RequestParam("status") status status,
                                 @RequestParam("type") typeActivity type,
                                 RedirectAttributes redirectAttributes,
                                 Model model) {
        Map<String, String> errors = validateActivityInputs(name, description, ageMin, ageMax, capacity);
        if (!errors.isEmpty()) {
            Activity activity = adminService.getActivityById(id);
            activity.setActivyName(name);
            activity.setDescription(description);
            activity.setAgeMin(ageMin);
            activity.setAgeMax(ageMax);
            activity.setCapacity(capacity);
            model.addAttribute("activity", activity);
            model.addAttribute("statuses", status.values());
            model.addAttribute("types", typeActivity.values());
            model.addAttribute("errors", errors);
            return "admin/adminActivityEdit";
        }
        adminService.updateActivity(id, name, description, null, ageMin, ageMax, capacity, status, type);
        redirectAttributes.addFlashAttribute("message", "Activité mise à jour.");
        return "redirect:/admin/activities";
    }

    @PostMapping("/activities/{id}/delete")
    public String deleteActivity(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        long count = adminService.countInscriptionsForActivity(id);
        adminService.deleteActivity(id);
        if (count > 0) {
            redirectAttributes.addFlashAttribute("message", "Activité supprimée (" + count + " inscriptions supprimées).");
        } else {
            redirectAttributes.addFlashAttribute("message", "Activité supprimée.");
        }
        return "redirect:/admin/activities";
    }

    @PostMapping("/api/activities/{id}/delete")
    @ResponseBody
    public Map<String, Object> deleteActivityAjax(@PathVariable("id") Long id) {
        long count = adminService.countInscriptionsForActivity(id);
        adminService.deleteActivity(id);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("id", id);
        response.put("message", count > 0
                ? "Activité supprimée (" + count + " inscriptions supprimées)."
                : "Activité supprimée.");
        return response;
    }

    @GetMapping("/animations")
    public String animations(Model model) {
        List<Animation> animations = adminService.getAllAnimations();
        model.addAttribute("animations", animations);
        model.addAttribute("animationCapacity", adminService.getAnimationCapacitySnapshots());
        return "admin/adminAnimations";
    }

    @GetMapping("/animations/new")
    public String newAnimation(Model model) {
        model.addAttribute("activities", adminService.getAllActivities());
        model.addAttribute("animateurs", adminService.getAllAnimateurs());
        model.addAttribute("roles", AnimationRole.values());
        model.addAttribute("statuses", animationStatus.values());
        return "admin/adminAnimationNew";
    }

    @PostMapping("/animations")
    public String createAnimation(@RequestParam("activityId") Long activityId,
                                  @RequestParam("animateurId") Long animateurId,
                                  @RequestParam("role") AnimationRole role,
                                  @RequestParam("status") animationStatus status,
                                  @RequestParam("start") @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm") LocalDateTime start,
                                  @RequestParam("end") @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm") LocalDateTime end,
                                  RedirectAttributes redirectAttributes) {
        adminService.createAnimation(activityId, animateurId, role, status, start, end);
        redirectAttributes.addFlashAttribute("message", "Animation créée.");
        return "redirect:/admin/animations";
    }

    @GetMapping("/animations/{id}/edit")
    public String editAnimation(@PathVariable("id") Long id, Model model) {
        Animation animation = adminService.getAnimationById(id);
        model.addAttribute("animation", animation);
        model.addAttribute("activities", adminService.getAllActivities());
        model.addAttribute("animateurs", adminService.getAllAnimateurs());
        model.addAttribute("roles", AnimationRole.values());
        model.addAttribute("statuses", animationStatus.values());
        return "admin/adminAnimationEdit";
    }

    @PostMapping("/animations/{id}/edit")
    public String updateAnimation(@PathVariable("id") Long id,
                                  @RequestParam("activityId") Long activityId,
                                  @RequestParam("animateurId") Long animateurId,
                                  @RequestParam("role") AnimationRole role,
                                  @RequestParam("status") animationStatus status,
                                  @RequestParam("start") @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm") LocalDateTime start,
                                  @RequestParam("end") @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm") LocalDateTime end,
                                  RedirectAttributes redirectAttributes) {
        adminService.updateAnimation(id, activityId, animateurId, role, status, start, end);
        redirectAttributes.addFlashAttribute("message", "Animation mise à jour.");
        return "redirect:/admin/animations";
    }

    @PostMapping("/animations/{id}/delete")
    public String deleteAnimation(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        long count = adminService.countInscriptionsForAnimation(id);
        adminService.deleteAnimation(id);
        if (count > 0) {
            redirectAttributes.addFlashAttribute("message", "Animation supprimée (" + count + " inscriptions supprimées).");
        } else {
            redirectAttributes.addFlashAttribute("message", "Animation supprimée.");
        }
        return "redirect:/admin/animations";
    }

    @PostMapping("/api/animations/{id}/delete")
    @ResponseBody
    public Map<String, Object> deleteAnimationAjax(@PathVariable("id") Long id) {
        long count = adminService.countInscriptionsForAnimation(id);
        adminService.deleteAnimation(id);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("id", id);
        response.put("message", count > 0
                ? "Animation supprimée (" + count + " inscriptions supprimées)."
                : "Animation supprimée.");
        return response;
    }

    @GetMapping("/animateurs")
    public String animateurs(Model model) {
        List<Animateur> animateurs = adminService.getAllAnimateurs();
        model.addAttribute("animateurs", animateurs);
        return "admin/adminAnimateurs";
    }

    @GetMapping("/parents")
    public String parents(Model model) {
        List<Parent> parents = adminService.getAllParents();
        model.addAttribute("parents", parents);
        return "admin/adminParents";
    }

    @GetMapping("/inscriptions")
    public String inscriptions(Model model) {
        return "redirect:/admin/demandes";
    }

    @GetMapping("/demandes")
    public String demandes(Model model) {
        model.addAttribute("pendingParents", adminService.getPendingParents());
        model.addAttribute("pendingEnfants", adminService.getPendingEnfants());
        model.addAttribute("pendingInscriptions", adminService.getPendingInscriptions());
        model.addAttribute("processedInscriptions", adminService.getProcessedInscriptions());
        model.addAttribute("animationCapacity", adminService.getAnimationCapacitySnapshots());
        return "admin/adminDemandes";
    }

    @GetMapping("/notifications")
    public String notifications(Model model) {
        model.addAttribute("notifications", adminNotificationService.getAll());
        return "admin/adminNotifications";
    }

    @PostMapping("/parents/{id}/status")
    public String updateParentStatus(@PathVariable("id") Long id,
                                     @RequestParam("enabled") boolean enabled,
                                     RedirectAttributes redirectAttributes) {
        adminService.updateParentEnabled(id, enabled);
        redirectAttributes.addFlashAttribute("message", enabled ? "Compte parent confirme." : "Compte parent desactive.");
        return "redirect:/admin/parents";
    }

    @PostMapping("/parents/{id}/delete")
    public String deleteParent(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        adminService.deleteParent(id);
        redirectAttributes.addFlashAttribute("message", "Parent supprime.");
        return "redirect:/admin/parents";
    }

    @PostMapping("/enfants/{id}/status")
    public String updateEnfantStatus(@PathVariable("id") Long id,
                                     @RequestParam("active") boolean active,
                                     RedirectAttributes redirectAttributes) {
        adminService.updateEnfantActive(id, active);
        redirectAttributes.addFlashAttribute("message", active ? "Enfant active." : "Enfant desactive.");
        return "redirect:/admin/parents";
    }

    @PostMapping("/enfants/{id}/delete")
    public String deleteEnfant(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        adminService.deleteEnfant(id);
        redirectAttributes.addFlashAttribute("message", "Enfant supprime.");
        return "redirect:/admin/parents";
    }

    @PostMapping("/inscriptions/{id}/approve")
    public String approveInscription(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        try {
            adminService.approveInscription(id);
            redirectAttributes.addFlashAttribute("message", "Demande approuvee.");
        } catch (IllegalStateException exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/admin/demandes";
    }

    @PostMapping("/inscriptions/{id}/reject")
    public String rejectInscription(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        adminService.rejectInscription(id);
        redirectAttributes.addFlashAttribute("message", "Demande refusee.");
        return "redirect:/admin/demandes";
    }

    @PostMapping("/api/parents/{id}/status")
    @ResponseBody
    public Map<String, Object> updateParentStatusAjax(@PathVariable("id") Long id,
                                                      @RequestParam("enabled") boolean enabled) {
        adminService.updateParentEnabled(id, enabled);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("message", enabled ? "Compte parent confirmé." : "Compte parent désactivé.");
        return response;
    }

    @PostMapping("/api/parents/{id}/delete")
    @ResponseBody
    public Map<String, Object> deleteParentAjax(@PathVariable("id") Long id) {
        adminService.deleteParent(id);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("id", id);
        response.put("message", "Parent supprimé.");
        return response;
    }

    @PostMapping("/api/enfants/{id}/status")
    @ResponseBody
    public Map<String, Object> updateEnfantStatusAjax(@PathVariable("id") Long id,
                                                      @RequestParam("active") boolean active) {
        adminService.updateEnfantActive(id, active);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("message", active ? "Enfant approuvé." : "Enfant désactivé.");
        return response;
    }

    @PostMapping("/api/enfants/{id}/delete")
    @ResponseBody
    public Map<String, Object> deleteEnfantAjax(@PathVariable("id") Long id) {
        adminService.deleteEnfant(id);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("id", id);
        response.put("message", "Enfant supprimé.");
        return response;
    }

    @PostMapping("/api/inscriptions/{id}/approve")
    @ResponseBody
    public Map<String, Object> approveInscriptionAjax(@PathVariable("id") Long id) {
        Map<String, Object> response = new LinkedHashMap<>();
        try {
            adminService.approveInscription(id);
            response.put("success", true);
            response.put("message", "Demande approuvée.");
        } catch (IllegalStateException exception) {
            response.put("success", false);
            response.put("message", exception.getMessage());
        }
        return response;
    }

    @PostMapping("/api/inscriptions/{id}/reject")
    @ResponseBody
    public Map<String, Object> rejectInscriptionAjax(@PathVariable("id") Long id) {
        adminService.rejectInscription(id);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("message", "Demande refusée.");
        return response;
    }

    @PostMapping("/animateurs/{id}/status")
    public String updateAnimateurStatus(@PathVariable("id") Long id,
                                        @RequestParam("enabled") boolean enabled,
                                        RedirectAttributes redirectAttributes) {
        adminService.updateAnimateurEnabled(id, enabled);
        redirectAttributes.addFlashAttribute("message", enabled ? "Compte animateur activé." : "Compte animateur désactivé.");
        return "redirect:/admin/animateurs";
    }

    @PostMapping("/api/animateurs/{id}/status")
    @ResponseBody
    public Map<String, Object> updateAnimateurStatusAjax(@PathVariable("id") Long id,
                                                         @RequestParam("enabled") boolean enabled) {
        adminService.updateAnimateurEnabled(id, enabled);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("message", enabled ? "Compte animateur activé." : "Compte animateur désactivé.");
        return response;
    }

    @GetMapping("/animateurs/new")
    public String newAnimateur(Model model) {
        model.addAttribute("errors", new LinkedHashMap<String, String>());
        model.addAttribute("formData", new LinkedHashMap<String, String>());
        return "admin/adminAnimateurNew";
    }

    @PostMapping("/animateurs")
    public String createAnimateur(@RequestParam("nom") String nom,
                                  @RequestParam("prenom") String prenom,
                                  @RequestParam("email") String email,
                                  @RequestParam("password") String password,
                                  RedirectAttributes redirectAttributes,
                                  Model model) {
        Map<String, String> errors = validateAnimateurInputs(nom, prenom, email, password, true);
        if (!errors.isEmpty()) {
            model.addAttribute("errors", errors);
            model.addAttribute("formData", buildAnimateurFormData(nom, prenom, email));
            return "admin/adminAnimateurNew";
        }
        adminService.createAnimateur(nom, prenom, email, password);
        redirectAttributes.addFlashAttribute("message", "Animateur créé.");
        return "redirect:/admin/animateurs";
    }

    @GetMapping("/animateurs/{id}/edit")
    public String editAnimateur(@PathVariable("id") Long id, Model model) {
        Animateur animateur = adminService.getAnimateurById(id);
        model.addAttribute("animateur", animateur);
        model.addAttribute("errors", new LinkedHashMap<String, String>());
        return "admin/adminAnimateurEdit";
    }

    @PostMapping("/animateurs/{id}/edit")
    public String updateAnimateur(@PathVariable("id") Long id,
                                  @RequestParam("nom") String nom,
                                  @RequestParam("prenom") String prenom,
                                  RedirectAttributes redirectAttributes,
                                  Model model) {
        Map<String, String> errors = validateAnimateurInputs(nom, prenom, null, null, false);
        if (!errors.isEmpty()) {
            Animateur animateur = adminService.getAnimateurById(id);
            animateur.setNom(nom);
            animateur.setPrenom(prenom);
            model.addAttribute("animateur", animateur);
            model.addAttribute("errors", errors);
            return "admin/adminAnimateurEdit";
        }
        adminService.updateAnimateur(id, nom, prenom);
        redirectAttributes.addFlashAttribute("message", "Animateur mis à jour.");
        return "redirect:/admin/animateurs";
    }

    @PostMapping("/animateurs/{id}/delete")
    public String deleteAnimateur(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        adminService.deleteAnimateur(id);
        redirectAttributes.addFlashAttribute("message", "Animateur supprimé.");
        return "redirect:/admin/animateurs";
    }

    @PostMapping("/api/animateurs/{id}/delete")
    @ResponseBody
    public Map<String, Object> deleteAnimateurAjax(@PathVariable("id") Long id) {
        adminService.deleteAnimateur(id);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("id", id);
        response.put("message", "Animateur supprimé.");
        return response;
    }

    private Map<String, String> validateActivityInputs(String name, String description, int ageMin, int ageMax, int capacity) {
        Map<String, String> errors = new LinkedHashMap<>();
        if (name == null || name.trim().isEmpty()) {
            errors.put("name", "Le nom est obligatoire.");
        }
        if (description == null || description.trim().isEmpty()) {
            errors.put("description", "La description est obligatoire.");
        }
        if (ageMin < 0) {
            errors.put("ageMin", "L'âge minimum doit être positif.");
        }
        if (ageMax < 0) {
            errors.put("ageMax", "L'âge maximum doit être positif.");
        }
        if (ageMin > ageMax) {
            errors.put("ageMax", "L'âge maximum doit être supérieur ou égal à l'âge minimum.");
        }
        if (capacity < 1) {
            errors.put("capacity", "La capacité doit être d'au moins 1.");
        }
        return errors;
    }

    private Map<String, String> buildActivityFormData(String name, String description, int ageMin, int ageMax, int capacity,
                                                      status status, typeActivity type) {
        Map<String, String> formData = new LinkedHashMap<>();
        formData.put("name", name == null ? "" : name);
        formData.put("description", description == null ? "" : description);
        formData.put("ageMin", String.valueOf(ageMin));
        formData.put("ageMax", String.valueOf(ageMax));
        formData.put("capacity", String.valueOf(capacity));
        formData.put("status", status == null ? "" : status.name());
        formData.put("type", type == null ? "" : type.name());
        return formData;
    }

    private Map<String, String> validateAnimateurInputs(String nom, String prenom, String email, String password, boolean includeCredentials) {
        Map<String, String> errors = new LinkedHashMap<>();
        if (nom == null || nom.trim().isEmpty()) {
            errors.put("nom", "Le nom est obligatoire.");
        }
        if (prenom == null || prenom.trim().isEmpty()) {
            errors.put("prenom", "Le prénom est obligatoire.");
        }
        if (includeCredentials) {
            if (email == null || email.trim().isEmpty()) {
                errors.put("email", "L'email est obligatoire.");
            } else if (!email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
                errors.put("email", "Entrez une adresse email valide.");
            } else if (!adminService.isEmailAvailable(email)) {
                errors.put("email", "Un utilisateur existe déjà avec cet email.");
            }
            if (password == null || password.isBlank()) {
                errors.put("password", "Le mot de passe est obligatoire.");
            } else if (password.length() < 6) {
                errors.put("password", "Le mot de passe doit contenir au moins 6 caractères.");
            }
        }
        return errors;
    }

    private Map<String, String> buildAnimateurFormData(String nom, String prenom, String email) {
        Map<String, String> formData = new LinkedHashMap<>();
        formData.put("nom", nom == null ? "" : nom);
        formData.put("prenom", prenom == null ? "" : prenom);
        formData.put("email", email == null ? "" : email);
        return formData;
    }
}
