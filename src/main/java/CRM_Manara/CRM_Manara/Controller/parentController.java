package CRM_Manara.CRM_Manara.Controller;

import CRM_Manara.CRM_Manara.Model.Entity.Activity;
import CRM_Manara.CRM_Manara.Model.Entity.Animation;
import CRM_Manara.CRM_Manara.Model.Entity.Enfant;
import CRM_Manara.CRM_Manara.Model.Entity.Inscription;
import CRM_Manara.CRM_Manara.Model.Entity.Service.parentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.sql.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/parent")
public class parentController {

    @Autowired
    private parentService parentService;

    @GetMapping("/dashboard")
    public String parentpage() {
        return "parent/parentDashboard";
    }

    @GetMapping("/enfants")
    public String listEnfants(Model model, Principal principal) {
        String email = principal.getName();
        List<Enfant> enfants = parentService.getEnfantsForParent(email);
        model.addAttribute("enfants", enfants);
        return "parent/parentEnfants";
    }

    @GetMapping("/enfants/new")
    public String newEnfantForm() {
        return "parent/parentEnfantNew";
    }

    @PostMapping("/enfants")
    public String createEnfant(@RequestParam("nom") String nom,
                               @RequestParam("prenom") String prenom,
                               @RequestParam("dateNaissance") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) java.time.LocalDate dateNaissance,
                               Principal principal,
                               RedirectAttributes redirectAttributes) {
        String email = principal.getName();
        parentService.createEnfantForParent(email, nom, prenom, Date.valueOf(dateNaissance));
        redirectAttributes.addFlashAttribute("message", "Enfant créé avec succès.");
        return "redirect:/parent/enfants";
    }

    @GetMapping("/activities")
    public String listActivities(Model model, Principal principal) {
        String email = principal.getName();
        List<Activity> activities = parentService.getAllActivities();
        Map<Long, List<Animation>> animationsByActivity = new LinkedHashMap<>();
        for (Activity activity : activities) {
            animationsByActivity.put(activity.getId(), parentService.getAnimationsForActivity(activity.getId()));
        }
        List<Enfant> enfants = parentService.getEnfantsForParent(email);
        model.addAttribute("activities", activities);
        model.addAttribute("animationsByActivity", animationsByActivity);
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
        redirectAttributes.addFlashAttribute("message", "Inscription réussie.");
        return "redirect:/parent/planning";
    }

    @GetMapping("/planning")
    public String planning(Model model, Principal principal) {
        String email = principal.getName();
        List<Inscription> inscriptions = parentService.getInscriptionsForParent(email);
        model.addAttribute("inscriptions", inscriptions);
        return "parent/parentPlanning";
    }
}
