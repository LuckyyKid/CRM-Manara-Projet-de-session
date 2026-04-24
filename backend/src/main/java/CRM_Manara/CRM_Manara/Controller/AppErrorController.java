package CRM_Manara.CRM_Manara.Controller;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.webmvc.error.ErrorController;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class AppErrorController implements ErrorController {

    private static final Logger logger = LoggerFactory.getLogger(AppErrorController.class);

    @RequestMapping("/error")
    public String handleError(HttpServletRequest request, Model model) {
        int statusCode = resolveStatusCode(request);
        String requestUri = resolveRequestUri(request);
        Throwable throwable = (Throwable) request.getAttribute(RequestDispatcher.ERROR_EXCEPTION);

        if (statusCode >= 500) {
            logger.error("HTTP {} on {}", statusCode, requestUri, throwable);
        } else if (statusCode >= 400) {
            logger.warn("HTTP {} on {}", statusCode, requestUri, throwable);
        } else {
            logger.info("Forwarded to /error for {}", requestUri, throwable);
        }

        model.addAttribute("statusCode", statusCode);
        model.addAttribute("errorTitle", resolveTitle(statusCode));
        model.addAttribute("errorMessage", resolveMessage(statusCode));
        model.addAttribute("primaryActionLabel", "Retour a l'accueil");
        model.addAttribute("primaryActionHref", "/");
        model.addAttribute("secondaryActionLabel", "Aller a la connexion");
        model.addAttribute("secondaryActionHref", "/login");
        return "error/friendly-error";
    }

    private int resolveStatusCode(HttpServletRequest request) {
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        if (status instanceof Integer code) {
            return code;
        }
        if (status instanceof String value) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException ignored) {
                return 500;
            }
        }
        return 500;
    }

    private String resolveRequestUri(HttpServletRequest request) {
        Object uri = request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);
        if (uri instanceof String value && !value.isBlank()) {
            return value;
        }
        return request.getRequestURI();
    }

    private String resolveTitle(int statusCode) {
        return switch (statusCode) {
            case 403 -> "Cette zone n'est pas accessible pour le moment";
            case 404 -> "Cette page n'est plus disponible ici";
            case 405 -> "Cette action ne peut pas etre effectuee ainsi";
            default -> "Une operation est temporairement indisponible";
        };
    }

    private String resolveMessage(int statusCode) {
        return switch (statusCode) {
            case 403 -> "Votre session ne permet pas d'ouvrir ce contenu. Revenez vers un espace autorise.";
            case 404 -> "Le lien utilise ne mene a aucun contenu actif. Vous pouvez repartir depuis l'accueil.";
            case 405 -> "La demande a bien ete recue, mais ce format d'action n'est pas accepte par cette page.";
            default -> "La demande n'a pas pu etre finalisee pour le moment. Vous pouvez reessayer dans quelques instants.";
        };
    }
}
