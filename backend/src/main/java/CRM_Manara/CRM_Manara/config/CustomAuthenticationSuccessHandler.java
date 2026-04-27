package CRM_Manara.CRM_Manara.config;

import CRM_Manara.CRM_Manara.service.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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
    private final JwtService jwtService;

    public CustomAuthenticationSuccessHandler(@Value("${app.frontend.base-url}") String frontendBaseUrl,
                                              JwtService jwtService) {
        this.frontendBaseUrl = frontendBaseUrl.replaceAll("/+$", "");
        this.jwtService = jwtService;
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) throws IOException {
        String role = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .findFirst()
                .orElse("ROLE_PARENT");
        String token = jwtService.generateToken(authentication.getName(), role);
        String redirectUrl = frontendBaseUrl + "/oauth-success?token="
                + URLEncoder.encode(token, StandardCharsets.UTF_8);
        logger.info("OAuth success for {} -> {}", authentication == null ? "anonymous" : authentication.getName(), redirectUrl);
        response.sendRedirect(redirectUrl);
    }
}
