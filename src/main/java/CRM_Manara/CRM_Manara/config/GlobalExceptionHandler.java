package CRM_Manara.CRM_Manara.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(DataIntegrityViolationException.class)
    public String handleDataIntegrity(DataIntegrityViolationException ex,
                                      HttpServletRequest request,
                                      RedirectAttributes redirectAttributes) {
        logger.warn("Data integrity violation on {} {}",
                request.getMethod(),
                request.getRequestURI(),
                ex);

        String referer = request.getHeader("Referer");
        redirectAttributes.addFlashAttribute("message",
                "Action impossible : des donnees liees existent encore. Supprimez-les d'abord.");
        if (referer != null && !referer.isBlank()) {
            return "redirect:" + referer;
        }
        return "redirect:/";
    }

    @ExceptionHandler(Exception.class)
    public ModelAndView handleUnexpectedException(Exception ex,
                                                  HttpServletRequest request,
                                                  HttpServletResponse response) {
        logger.error("Unhandled exception on {} {}", request.getMethod(), request.getRequestURI(), ex);

        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);

        ModelAndView modelAndView = new ModelAndView("error/friendly-error");
        modelAndView.addObject("statusCode", 500);
        modelAndView.addObject("errorTitle", "Une operation est temporairement indisponible");
        modelAndView.addObject("errorMessage",
                "La demande n'a pas pu etre finalisee pour le moment. Vous pouvez reessayer dans quelques instants.");
        modelAndView.addObject("primaryActionLabel", "Retour a l'accueil");
        modelAndView.addObject("primaryActionHref", "/");
        modelAndView.addObject("secondaryActionLabel", "Se connecter");
        modelAndView.addObject("secondaryActionHref", "/login");
        return modelAndView;
    }
}
