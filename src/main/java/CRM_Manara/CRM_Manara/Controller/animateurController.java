package CRM_Manara.CRM_Manara.Controller;

import CRM_Manara.CRM_Manara.Model.Entity.Animateur;
import CRM_Manara.CRM_Manara.Model.Entity.Animation;
import CRM_Manara.CRM_Manara.Model.Entity.Inscription;
import CRM_Manara.CRM_Manara.Model.Entity.Enum.PresenceStatus;
import CRM_Manara.CRM_Manara.Model.Entity.Service.AnimateurService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.List;

@Controller
@RequestMapping("/animateur")
public class animateurController {

    @Autowired
    private AnimateurService animateurService;

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
        return "animateur/animateurDashboard";
    }

    @GetMapping("/inscriptions")
    public String inscriptions(Model model, Principal principal) {
        String email = principal.getName();
        Animateur animateur = animateurService.getAnimateurByEmail(email);
        List<Inscription> inscriptions = animateurService.getInscriptionsForAnimateur(animateur.getId());
        model.addAttribute("animateur", animateur);
        model.addAttribute("inscriptions", inscriptions);
        model.addAttribute("presenceStatuses", PresenceStatus.values());
        return "animateur/animateurInscriptions";
    }

    @PostMapping("/inscriptions/{id}/presence")
    public String updatePresence(@PathVariable("id") Long id,
                                 @RequestParam("presenceStatus") PresenceStatus presenceStatus,
                                 @RequestParam(name = "incidentNote", required = false) String incidentNote,
                                 Principal principal,
                                 RedirectAttributes redirectAttributes) {
        String email = principal.getName();
        animateurService.updatePresence(id, email, presenceStatus, incidentNote);
        redirectAttributes.addFlashAttribute("message", "Présence mise à jour.");
        return "redirect:/animateur/inscriptions";
    }
}
