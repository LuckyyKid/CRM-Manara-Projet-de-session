package CRM_Manara.CRM_Manara.Controller;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import java.io.IOException;

@Controller
public class AngularRedirectController {

    private final String frontendBaseUrl;

    public AngularRedirectController(@Value("${app.frontend.base-url}") String frontendBaseUrl) {
        this.frontendBaseUrl = frontendBaseUrl;
    }

    @GetMapping({"/", "/index", "/about", "/login", "/signUp", "/register"})
    public void redirectToAngular(HttpServletResponse response) throws IOException {
        response.sendRedirect(frontendBaseUrl);
    }
}
