package CRM_Manara.CRM_Manara.Controller;

import CRM_Manara.CRM_Manara.Model.Entity.Activity;
import CRM_Manara.CRM_Manara.Model.Entity.Animateur;
import CRM_Manara.CRM_Manara.Model.Entity.Animation;
import CRM_Manara.CRM_Manara.Model.Entity.Inscription;
import CRM_Manara.CRM_Manara.Model.Entity.Enum.AnimationRole;
import CRM_Manara.CRM_Manara.Model.Entity.Enum.animationStatus;
import CRM_Manara.CRM_Manara.Model.Entity.Enum.status;
import CRM_Manara.CRM_Manara.Model.Entity.Enum.typeActivity;
import CRM_Manara.CRM_Manara.Model.Entity.Service.AdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequestMapping("/admin")
public class adminController {

    @Autowired
    private AdminService adminService;

    @GetMapping("/adminDashboard")
    public String adminDashboard(Model model) {
        model.addAttribute("countActivities", adminService.countActivities());
        model.addAttribute("countAnimations", adminService.countAnimations());
        model.addAttribute("countAnimateurs", adminService.countAnimateurs());
        model.addAttribute("countInscriptions", adminService.countInscriptions());
        model.addAttribute("countOpenActivities", adminService.countOpenActivities());
        model.addAttribute("recentInscriptions", adminService.getRecentInscriptions(5));
        model.addAttribute("upcomingAnimations", adminService.getUpcomingAnimations(5));
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
                                 RedirectAttributes redirectAttributes) {
        adminService.createActivity(name, description, ageMin, ageMax, capacity, status, type);
        redirectAttributes.addFlashAttribute("message", "Activité créée.");
        return "redirect:/admin/activities";
    }

    @GetMapping("/activities/{id}/edit")
    public String editActivity(@PathVariable("id") Long id, Model model) {
        Activity activity = adminService.getActivityById(id);
        model.addAttribute("activity", activity);
        model.addAttribute("statuses", status.values());
        model.addAttribute("types", typeActivity.values());
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
                                 RedirectAttributes redirectAttributes) {
        adminService.updateActivity(id, name, description, ageMin, ageMax, capacity, status, type);
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

    @GetMapping("/animations")
    public String animations(Model model) {
        List<Animation> animations = adminService.getAllAnimations();
        model.addAttribute("animations", animations);
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

    @GetMapping("/animateurs")
    public String animateurs(Model model) {
        List<Animateur> animateurs = adminService.getAllAnimateurs();
        model.addAttribute("animateurs", animateurs);
        return "admin/adminAnimateurs";
    }

    @GetMapping("/inscriptions")
    public String inscriptions(Model model) {
        List<Inscription> inscriptions = adminService.getAllInscriptions();
        model.addAttribute("inscriptions", inscriptions);
        return "admin/adminInscriptions";
    }

    @GetMapping("/animateurs/new")
    public String newAnimateur() {
        return "admin/adminAnimateurNew";
    }

    @PostMapping("/animateurs")
    public String createAnimateur(@RequestParam("nom") String nom,
                                  @RequestParam("prenom") String prenom,
                                  @RequestParam("email") String email,
                                  @RequestParam("password") String password,
                                  RedirectAttributes redirectAttributes) {
        adminService.createAnimateur(nom, prenom, email, password);
        redirectAttributes.addFlashAttribute("message", "Animateur créé.");
        return "redirect:/admin/animateurs";
    }

    @GetMapping("/animateurs/{id}/edit")
    public String editAnimateur(@PathVariable("id") Long id, Model model) {
        Animateur animateur = adminService.getAnimateurById(id);
        model.addAttribute("animateur", animateur);
        return "admin/adminAnimateurEdit";
    }

    @PostMapping("/animateurs/{id}/edit")
    public String updateAnimateur(@PathVariable("id") Long id,
                                  @RequestParam("nom") String nom,
                                  @RequestParam("prenom") String prenom,
                                  RedirectAttributes redirectAttributes) {
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
}
