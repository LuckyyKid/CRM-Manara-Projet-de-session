package CRM_Manara.CRM_Manara.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;


    @Component
    public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

        @Override
        public void onAuthenticationSuccess(
                HttpServletRequest request,
                HttpServletResponse response,
                Authentication authentication) throws IOException {

            for (var authority : authentication.getAuthorities()) {

                if (authority.getAuthority().equals("ROLE_ADMIN")) {

                    System.out.println("REDIRECTION VERS ADMIN LANCÉE !");
                    response.sendRedirect("/admin/adminDashboard");
                    return;
                }

                if (authority.getAuthority().equals("ROLE_PARENT")) {
                    System.out.println("REDIRECTION VERS PARENT LANCÉE !");
                    response.sendRedirect("/parent/dashboard");
                    return;
                }

                if (authority.getAuthority().equals("ROLE_ANIMATEUR")) {
                    response.sendRedirect("/animateur/dashboard");
                    return;
                }
            }

            response.sendRedirect("/");
        }
    }


