package CRM_Manara.CRM_Manara.Controller;

import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Repository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin")
public class adminController {

    @GetMapping("/adminDashboard")
    public String adminDashboard() {
        return "adminDashboard";
    }
}
