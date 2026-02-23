package CRM_Manara.CRM_Manara.Controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class parentDashboardController {
    @GetMapping("/parentDashboard")
    public String parentDashboard(){
        return "parentDashboard";
    }
}
