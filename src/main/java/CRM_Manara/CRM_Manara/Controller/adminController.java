package CRM_Manara.CRM_Manara.Controller;

import CRM_Manara.CRM_Manara.Model.Entity.Activity;
import CRM_Manara.CRM_Manara.Model.Entity.Animateur;
import CRM_Manara.CRM_Manara.Model.Entity.Animation;
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
    public String adminDashboard() {
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
                                  @RequestParam("start") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
                                  @RequestParam("end") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end,
                                  RedirectAttributes redirectAttributes) {
        adminService.createAnimation(activityId, animateurId, role, status, start, end);
        redirectAttributes.addFlashAttribute("message", "Animation créée.");
        return "redirect:/admin/animations";
    }

    @GetMapping("/animateurs")
    public String animateurs(Model model) {
        List<Animateur> animateurs = adminService.getAllAnimateurs();
        model.addAttribute("animateurs", animateurs);
        return "admin/adminAnimateurs";
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
}
