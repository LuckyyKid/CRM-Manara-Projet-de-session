package CRM_Manara.CRM_Manara.service;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Service
public class RequestLanguageService {

    public String currentLanguage() {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return "fr";
        }

        HttpServletRequest request = attributes.getRequest();
        String headerLanguage = normalize(request.getHeader("X-Language"));
        if (headerLanguage != null) {
            return headerLanguage;
        }

        String acceptLanguage = request.getHeader("Accept-Language");
        if (acceptLanguage != null && acceptLanguage.toLowerCase().startsWith("en")) {
            return "en";
        }

        return "fr";
    }

    public boolean isEnglish() {
        return "en".equals(currentLanguage());
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim().toLowerCase();
        if (normalized.startsWith("en")) {
            return "en";
        }
        if (normalized.startsWith("fr")) {
            return "fr";
        }
        return null;
    }
}
