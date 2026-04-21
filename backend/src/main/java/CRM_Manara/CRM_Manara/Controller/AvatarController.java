package CRM_Manara.CRM_Manara.Controller;

import CRM_Manara.CRM_Manara.service.AvatarService;
import java.net.MalformedURLException;
import java.nio.file.Path;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AvatarController {
    private static final Path AVATAR_STORAGE = AvatarService.avatarStoragePath();

    @GetMapping("/avatars/{filename:.+}")
    public ResponseEntity<Resource> avatar(@org.springframework.web.bind.annotation.PathVariable("filename") String filename)
            throws MalformedURLException {
        Path file = AVATAR_STORAGE.resolve(filename).normalize();
        if (!file.startsWith(AVATAR_STORAGE)) {
            return ResponseEntity.notFound().build();
        }

        Resource resource = new UrlResource(file.toUri());
        if (!resource.exists() || !resource.isReadable()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate")
                .contentType(MediaType.parseMediaType(contentType(filename)))
                .body(resource);
    }

    private String contentType(String filename) {
        String lower = filename.toLowerCase();
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
            return MediaType.IMAGE_JPEG_VALUE;
        }
        if (lower.endsWith(".gif")) {
            return MediaType.IMAGE_GIF_VALUE;
        }
        if (lower.endsWith(".webp")) {
            return "image/webp";
        }
        if (lower.endsWith(".svg")) {
            return "image/svg+xml";
        }
        return MediaType.IMAGE_PNG_VALUE;
    }
}
