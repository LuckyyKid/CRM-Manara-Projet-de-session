package CRM_Manara.CRM_Manara.dto;

import CRM_Manara.CRM_Manara.Model.Entity.Activity;
import CRM_Manara.CRM_Manara.Model.Entity.AdminNotification;
import CRM_Manara.CRM_Manara.Model.Entity.Administrateurs;
import CRM_Manara.CRM_Manara.Model.Entity.Animateur;
import CRM_Manara.CRM_Manara.Model.Entity.AnimateurNotification;
import CRM_Manara.CRM_Manara.Model.Entity.Animation;
import CRM_Manara.CRM_Manara.Model.Entity.Enfant;
import CRM_Manara.CRM_Manara.Model.Entity.Inscription;
import CRM_Manara.CRM_Manara.Model.Entity.Parent;
import CRM_Manara.CRM_Manara.Model.Entity.ParentNotification;
import CRM_Manara.CRM_Manara.Model.Entity.User;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Component
public class ApiDtoMapper {

    public UserDto toUserDto(User user) {
        if (user == null) {
            return null;
        }
        return new UserDto(
                user.getId(),
                user.getEmail(),
                user.getRole() == null ? null : user.getRole().name(),
                user.isEnabled(),
                user.getAvatarUrl()
        );
    }

    public ParentSummaryDto toParentSummaryDto(Parent parent) {
        if (parent == null) {
            return null;
        }
        return new ParentSummaryDto(
                parent.getId(),
                parent.getNom(),
                parent.getPrenom(),
                parent.getUser() == null ? null : parent.getUser().getEmail(),
                parent.getUser() != null && parent.getUser().isEnabled()
        );
    }

    public EnfantSummaryDto toEnfantSummaryDto(Enfant enfant) {
        if (enfant == null) {
            return null;
        }
        return new EnfantSummaryDto(
                enfant.getId(),
                enfant.getNom(),
                enfant.getPrenom(),
                toLocalDate(enfant.getDate_de_naissance()),
                enfant.isActive()
        );
    }

    public ParentDto toParentDto(Parent parent) {
        if (parent == null) {
            return null;
        }
        List<EnfantSummaryDto> enfants = parent.getEnfants() == null
                ? Collections.emptyList()
                : parent.getEnfants().stream()
                .sorted(Comparator.comparing(Enfant::getPrenom, Comparator.nullsLast(String::compareToIgnoreCase)))
                .map(this::toEnfantSummaryDto)
                .toList();

        return new ParentDto(
                parent.getId(),
                parent.getNom(),
                parent.getPrenom(),
                parent.getAdresse(),
                toUserDto(parent.getUser()),
                enfants
        );
    }

    public EnfantDto toEnfantDto(Enfant enfant) {
        if (enfant == null) {
            return null;
        }
        return new EnfantDto(
                enfant.getId(),
                enfant.getNom(),
                enfant.getPrenom(),
                toLocalDate(enfant.getDate_de_naissance()),
                enfant.isActive(),
                toParentSummaryDto(enfant.getParent())
        );
    }

    public AnimateurSummaryDto toAnimateurSummaryDto(Animateur animateur) {
        if (animateur == null) {
            return null;
        }
        return new AnimateurSummaryDto(
                animateur.getId(),
                animateur.getNom(),
                animateur.getPrenom(),
                animateur.getUser() == null ? null : animateur.getUser().getEmail(),
                animateur.getUser() != null && animateur.getUser().isEnabled()
        );
    }

    public AnimateurDto toAnimateurDto(Animateur animateur) {
        if (animateur == null) {
            return null;
        }
        return new AnimateurDto(
                animateur.getId(),
                animateur.getNom(),
                animateur.getPrenom(),
                toUserDto(animateur.getUser())
        );
    }

    public AdminDto toAdminDto(Administrateurs admin) {
        if (admin == null) {
            return null;
        }
        return new AdminDto(
                admin.getId(),
                admin.getNom(),
                admin.getPrenom(),
                admin.getRole() == null ? null : admin.getRole().name(),
                admin.getStatus() == null ? null : admin.getStatus().name(),
                toUserDto(admin.getUser())
        );
    }

    public ActivitySummaryDto toActivitySummaryDto(Activity activity) {
        if (activity == null) {
            return null;
        }
        return new ActivitySummaryDto(
                activity.getId(),
                activity.getActivyName(),
                activity.getAgeMin(),
                activity.getAgeMax(),
                activity.getCapacity(),
                activity.getStatus() == null ? null : activity.getStatus().name(),
                activity.getType() == null ? null : activity.getType().name()
        );
    }

    public ActivityDto toActivityDto(Activity activity) {
        if (activity == null) {
            return null;
        }
        return new ActivityDto(
                activity.getId(),
                activity.getActivyName(),
                activity.getDescription(),
                activity.getImageUrl(),
                activity.getAgeMin(),
                activity.getAgeMax(),
                activity.getCapacity(),
                activity.getStatus() == null ? null : activity.getStatus().name(),
                activity.getType() == null ? null : activity.getType().name()
        );
    }

    public AnimationSummaryDto toAnimationSummaryDto(Animation animation) {
        if (animation == null) {
            return null;
        }
        return new AnimationSummaryDto(
                animation.getId(),
                animation.getStartTime(),
                animation.getEndTime(),
                animation.getRole() == null ? null : animation.getRole().name(),
                animation.getStatusAnimation() == null ? null : animation.getStatusAnimation().name(),
                toActivitySummaryDto(animation.getActivity()),
                toAnimateurSummaryDto(animation.getAnimateur())
        );
    }

    public AnimationDto toAnimationDto(Animation animation) {
        if (animation == null) {
            return null;
        }
        return new AnimationDto(
                animation.getId(),
                animation.getStartTime(),
                animation.getEndTime(),
                animation.getRole() == null ? null : animation.getRole().name(),
                animation.getStatusAnimation() == null ? null : animation.getStatusAnimation().name(),
                toActivityDto(animation.getActivity()),
                toAnimateurSummaryDto(animation.getAnimateur())
        );
    }

    public InscriptionDto toInscriptionDto(Inscription inscription) {
        if (inscription == null) {
            return null;
        }
        return new InscriptionDto(
                inscription.getId(),
                inscription.getStatusInscription() == null ? null : inscription.getStatusInscription().name(),
                inscription.getPresenceStatus() == null ? null : inscription.getPresenceStatus().name(),
                inscription.getIncidentNote(),
                toEnfantSummaryDto(inscription.getEnfant()),
                toAnimationSummaryDto(inscription.getAnimation())
        );
    }

    public AnimationCapacityDto toAnimationCapacityDto(Long animationId, Map<String, Object> snapshot) {
        if (snapshot == null) {
            return new AnimationCapacityDto(animationId, 0, 0, 0, 0, 0, 0, false, 0);
        }
        return new AnimationCapacityDto(
                animationId,
                asInt(snapshot.get("approved")),
                asInt(snapshot.get("pending")),
                asInt(snapshot.get("capacity")),
                asInt(snapshot.get("remaining")),
                asInt(snapshot.get("waitlist")),
                asInt(snapshot.get("fillRate")),
                asBoolean(snapshot.get("full")),
                asInt(snapshot.get("waitlistPosition"))
        );
    }

    public ParentNotificationDto toParentNotificationDto(ParentNotification notification) {
        if (notification == null) {
            return null;
        }
        return new ParentNotificationDto(
                notification.getId(),
                notification.getCategory(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getCreatedAt(),
                notification.isReadStatus(),
                notification.isArchivedStatus()
        );
    }

    public AnimateurNotificationDto toAnimateurNotificationDto(AnimateurNotification notification) {
        if (notification == null) {
            return null;
        }
        return new AnimateurNotificationDto(
                notification.getId(),
                notification.getCategory(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getCreatedAt(),
                notification.isReadStatus(),
                notification.isArchivedStatus()
        );
    }

    public AdminNotificationDto toAdminNotificationDto(AdminNotification notification) {
        if (notification == null) {
            return null;
        }
        return new AdminNotificationDto(
                notification.getId(),
                notification.getSource(),
                notification.getType(),
                notification.getMessage(),
                notification.getCreatedAt()
        );
    }

    private int asInt(Object value) {
        return value instanceof Number number ? number.intValue() : 0;
    }

    private boolean asBoolean(Object value) {
        return value instanceof Boolean bool && bool;
    }

    private LocalDate toLocalDate(Date date) {
        if (date == null) {
            return null;
        }
        if (date instanceof java.sql.Date sqlDate) {
            return sqlDate.toLocalDate();
        }
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }
}
