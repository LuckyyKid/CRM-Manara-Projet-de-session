package CRM_Manara.CRM_Manara.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    private static final Logger logger = LoggerFactory.getLogger(CustomAccessDeniedHandler.class);

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException, ServletException {
        logger.warn("Access denied on {} {} for user {}",
                request.getMethod(),
                request.getRequestURI(),
                request.getUserPrincipal() != null ? request.getUserPrincipal().getName() : "anonymous",
                accessDeniedException);

        request.setAttribute("jakarta.servlet.error.status_code", HttpServletResponse.SC_FORBIDDEN);
        request.setAttribute("jakarta.servlet.error.request_uri", request.getRequestURI());
        request.setAttribute("jakarta.servlet.error.exception", accessDeniedException);
        request.getRequestDispatcher("/error").forward(request, response);
    }
}
