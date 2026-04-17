package CRM_Manara.CRM_Manara.config;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(DataIntegrityViolationException.class)
    public Object handleDataIntegrity(DataIntegrityViolationException ex,
                                      HttpServletRequest request,
                                      RedirectAttributes redirectAttributes) {
        logger.warn("Contrainte de donnees sur {}", request.getRequestURI(), ex);
        boolean deleteRequest = "DELETE".equalsIgnoreCase(request.getMethod());
        return buildResponse(
                request,
                redirectAttributes,
                HttpStatus.CONFLICT,
                "Action impossible",
                deleteRequest
                        ? "Des donnees liees existent encore. Supprimez-les d'abord."
                        : "Les donnees envoyees ne respectent pas une contrainte de la base. Verifiez les champs obligatoires ou les valeurs deja existantes."
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public Object handleIllegalArgument(IllegalArgumentException ex,
                                        HttpServletRequest request,
                                        RedirectAttributes redirectAttributes) {
        return buildResponse(
                request,
                redirectAttributes,
                HttpStatus.BAD_REQUEST,
                "Demande invalide",
                ex.getMessage() == null || ex.getMessage().isBlank()
                        ? "La demande ne peut pas etre traitee avec les informations fournies."
                        : ex.getMessage()
        );
    }

    @ExceptionHandler(IllegalStateException.class)
    public Object handleIllegalState(IllegalStateException ex,
                                     HttpServletRequest request,
                                     RedirectAttributes redirectAttributes) {
        return buildResponse(
                request,
                redirectAttributes,
                HttpStatus.CONFLICT,
                "Action indisponible",
                ex.getMessage() == null || ex.getMessage().isBlank()
                        ? "Cette action ne peut pas etre appliquee pour le moment."
                        : ex.getMessage()
        );
    }

    @ExceptionHandler(ResponseStatusException.class)
    public Object handleResponseStatus(ResponseStatusException ex,
                                       HttpServletRequest request,
                                       RedirectAttributes redirectAttributes) {
        HttpStatus status = HttpStatus.resolve(ex.getStatusCode().value());
        String message = ex.getReason();
        return buildResponse(
                request,
                redirectAttributes,
                status == null ? HttpStatus.INTERNAL_SERVER_ERROR : status,
                "Action impossible",
                message == null || message.isBlank()
                        ? "La demande ne peut pas etre traitee avec les informations fournies."
                        : message
        );
    }

    @ExceptionHandler(Exception.class)
    public Object handleUnexpected(Exception ex,
                                   HttpServletRequest request,
                                   RedirectAttributes redirectAttributes) {
        logger.error("Erreur non geree sur {}", request.getRequestURI(), ex);
        return buildResponse(
                request,
                redirectAttributes,
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Erreur technique",
                "Une erreur inattendue est survenue. Reessayez dans quelques instants."
        );
    }

    private Object buildResponse(HttpServletRequest request,
                                 RedirectAttributes redirectAttributes,
                                 HttpStatus status,
                                 String title,
                                 String message) {
        if (expectsJson(request)) {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("success", false);
            body.put("message", message);
            body.put("title", title);
            body.put("status", status.value());
            return ResponseEntity.status(status).body(body);
        }

        String referer = request.getHeader("Referer");
        if (referer != null && !referer.isBlank() && !referer.contains("/error")) {
            redirectAttributes.addFlashAttribute("error", message);
            return "redirect:" + referer;
        }

        ModelAndView modelAndView = new ModelAndView("error");
        modelAndView.addObject("errorStatus", status.value());
        modelAndView.addObject("errorTitle", title);
        modelAndView.addObject("errorMessage", message);
        return modelAndView;
    }

    private boolean expectsJson(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String requestedWith = request.getHeader("X-Requested-With");
        String accept = request.getHeader("Accept");

        return (uri != null && uri.startsWith("/api/"))
                || "XMLHttpRequest".equalsIgnoreCase(requestedWith)
                || (accept != null && accept.contains("application/json"));
    }
}
