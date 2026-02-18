package CRM_Manara.CRM_Manara.Controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/parent")
public class parentController {

    @GetMapping("/parentDashboard")
    public String parentpage() {

        return "parentDashboard";

    }
}
