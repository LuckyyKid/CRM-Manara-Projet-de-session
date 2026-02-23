package CRM_Manara.CRM_Manara.Controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SignUpController {

    @GetMapping("/signUp")
    public String signUp(){
        return "signUp";
    }


}
