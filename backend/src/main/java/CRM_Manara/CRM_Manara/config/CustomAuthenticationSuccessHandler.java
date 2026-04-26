package CRM_Manara.CRM_Manara.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private static final Logger logger = LoggerFactory.getLogger(CustomAuthenticationSuccessHandler.class);

    private final String frontendBaseUrl;

    public CustomAuthenticationSuccessHandler(@Value("${app.frontend.base-url}") String frontendBaseUrl) {
        this.frontendBaseUrl = frontendBaseUrl.replaceAll("/+$", "");
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) throws IOException {
        String dashboardPath = dashboardPath(authentication);
        String redirectUrl = frontendBaseUrl + "/login?oauthSuccess&redirectTo=" + dashboardPath;
        logger.info("OAuth success for {} -> {}", authentication == null ? "anonymous" : authentication.getName(), redirectUrl);
        response.sendRedirect(redirectUrl);
    }

    private String dashboardPath(Authentication authentication) {
        if (hasAuthority(authentication, "ROLE_ADMIN")) {
            return "/admin/dashboard";
        }
        if (hasAuthority(authentication, "ROLE_ANIMATEUR")) {
            return "/animateur/dashboard";
        }
        if (hasAuthority(authentication, "ROLE_PARENT")) {
            return "/parent/dashboard";
        }
        return "/me/dashboard";
    }

    private boolean hasAuthority(Authentication authentication, String authority) {
        return authentication != null
                && authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(authority::equals);
    }
}
