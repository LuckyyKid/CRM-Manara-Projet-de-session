package CRM_Manara.CRM_Manara.service;

import CRM_Manara.CRM_Manara.Model.Entity.Animateur;
import CRM_Manara.CRM_Manara.Model.Entity.Animation;
import CRM_Manara.CRM_Manara.Model.Entity.Enum.statusInscription;
import CRM_Manara.CRM_Manara.Model.Entity.Inscription;
import CRM_Manara.CRM_Manara.Model.Entity.Enum.PresenceStatus;
import CRM_Manara.CRM_Manara.Repository.AnimateurRepo;
import CRM_Manara.CRM_Manara.Repository.AnimationRepo;
import CRM_Manara.CRM_Manara.Repository.InscriptionRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.stream.Collectors;

@Service
public class AnimateurService {

    @Autowired
    private AnimateurRepo animateurRepo;

    @Autowired
    private AnimationRepo animationRepo;

    @Autowired
    private InscriptionRepo inscriptionRepo;

    @Autowired
    private ParentNotificationService parentNotificationService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private AdminNotificationService adminNotificationService;

    @Transactional(readOnly = true)
    public Animateur getAnimateurByEmail(String email) {
        return animateurRepo.findByUserEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Animateur introuvable pour cet email"));
    }

    @Transactional(readOnly = true)
    public List<Animation> getAnimationsForAnimateur(Long animateurId) {
        return animationRepo.findByAnimateurId(animateurId).stream()
                .filter(animation -> animation.getStartTime() != null && animation.getEndTime() != null)
                .sorted(Comparator.comparing(Animation::getStartTime))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<Animation> getUpcomingAnimationsForAnimateur(Long animateurId, int limit) {
        LocalDateTime now = LocalDateTime.now();
        return animationRepo.findByAnimateurId(animateurId).stream()
                .filter(a -> isOngoingOrUpcoming(a, now))
                .sorted(Comparator.comparing(Animation::getStartTime))
                .limit(limit)
                .collect(Collectors.toList());
    }

    private boolean isOngoingOrUpcoming(Animation animation, LocalDateTime now) {
        LocalDateTime reference = animation.getEndTime() != null ? animation.getEndTime() : animation.getStartTime();
        return reference != null && !reference.isBefore(now);
    }


    @Transactional(readOnly = true)
    public long countInscriptionsForAnimation(Long animationId) {
        return inscriptionRepo.countByAnimationId(animationId);
    }

    @Transactional(readOnly = true)
    public List<Inscription> getInscriptionsForAnimateur(Long animateurId) {
        return inscriptionRepo.findByAnimateurId(animateurId).stream()
                .filter(inscription -> inscription.getStatusInscription() == statusInscription.APPROUVEE
                        || inscription.getStatusInscription() == statusInscription.ACTIF)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Animation getAnimationForAnimateur(Long animationId, String animateurEmail) {
        Animateur animateur = getAnimateurByEmail(animateurEmail);
        return animationRepo.findByAnimateurId(animateur.getId()).stream()
                .filter(animation -> animation.getId().equals(animationId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Animation introuvable pour cet animateur"));
    }

    @Transactional(readOnly = true)
    public List<Inscription> getInscriptionsForAnimation(Long animationId, String animateurEmail) {
        Animation animation = getAnimationForAnimateur(animationId, animateurEmail);
        return inscriptionRepo.findByAnimationId(animation.getId()).stream()
                .filter(inscription -> inscription.getStatusInscription() == statusInscription.APPROUVEE
                        || inscription.getStatusInscription() == statusInscription.ACTIF)
                .sorted(Comparator.comparing(inscription -> inscription.getEnfant().getPrenom()))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<Animation> getAnimationsForAnimateurWeek(String animateurEmail, LocalDate weekStart, LocalDate weekEnd) {
        Animateur animateur = getAnimateurByEmail(animateurEmail);
        return getAnimationsForAnimateur(animateur.getId()).stream()
                .filter(animation -> {
                    LocalDate date = animation.getStartTime().toLocalDate();
                    return !date.isBefore(weekStart) && !date.isAfter(weekEnd);
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public void updatePresence(Long inscriptionId, String animateurEmail, PresenceStatus presenceStatus, String incidentNote) {
        Animateur animateur = getAnimateurByEmail(animateurEmail);
        Inscription inscription = inscriptionRepo.findByIdAndAnimateurId(inscriptionId, animateur.getId());
        if (inscription == null) {
            throw new IllegalArgumentException("Inscription introuvable pour cet animateur");
        }
        if (inscription.getStatusInscription() != statusInscription.APPROUVEE
                && inscription.getStatusInscription() != statusInscription.ACTIF) {
            throw new IllegalArgumentException("Cette inscription n'a pas encore ete approuvee par l'administration.");
        }
        inscription.setPresenceStatus(presenceStatus);
        if (incidentNote != null && !incidentNote.isBlank()) {
            inscription.setIncidentNote(incidentNote);
        } else {
            inscription.setIncidentNote(null);
        }
        inscriptionRepo.save(inscription);

        if (inscription.getEnfant() != null && inscription.getEnfant().getParent() != null) {
            String message = "La présence de " + inscription.getEnfant().getPrenom() + " pour "
                    + inscription.getAnimation().getActivity().getActivyName()
                    + " a été mise à jour à " + presenceStatus + ".";
            if (inscription.getIncidentNote() != null && !inscription.getIncidentNote().isBlank()) {
                message += " Note: " + inscription.getIncidentNote();
            }

            parentNotificationService.createForParent(
                    inscription.getEnfant().getParent(),
                    "PRESENCE",
                    "Présence mise à jour",
                    message
            );
            adminNotificationService.create(
                    "ANIMATEUR",
                    "PRESENCE",
                    "Présence mise à jour par " + animateur.getPrenom() + " " + animateur.getNom()
                            + " pour " + inscription.getEnfant().getPrenom() + " " + inscription.getEnfant().getNom()
                            + " dans " + inscription.getAnimation().getActivity().getActivyName()
                            + ": " + presenceStatus
                            + (hasIncidentNote(inscription) ? " | Note: " + inscription.getIncidentNote() : "") + "."
            );

            boolean hasIncidentNote = inscription.getIncidentNote() != null && !inscription.getIncidentNote().isBlank();
            if ((presenceStatus == PresenceStatus.ABSENT || hasIncidentNote)
                    && inscription.getEnfant().getParent().getUser() != null) {
                emailService.sendPresenceUpdate(inscription.getEnfant().getParent().getUser().getEmail(), inscription);
            }
        }
    }

    private boolean hasIncidentNote(Inscription inscription) {
        return inscription.getIncidentNote() != null && !inscription.getIncidentNote().isBlank();
    }
}
