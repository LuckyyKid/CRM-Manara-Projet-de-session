package CRM_Manara.CRM_Manara.service;

import CRM_Manara.CRM_Manara.Model.Entity.Animateur;
import CRM_Manara.CRM_Manara.Model.Entity.AppointmentSlot;
import CRM_Manara.CRM_Manara.Model.Entity.Booking;
import CRM_Manara.CRM_Manara.Model.Entity.Enfant;
import CRM_Manara.CRM_Manara.Model.Entity.Parent;
import CRM_Manara.CRM_Manara.Model.Entity.User;
import CRM_Manara.CRM_Manara.Model.Entity.Enum.SecurityRole;
import CRM_Manara.CRM_Manara.Repository.AnimateurRepo;
import CRM_Manara.CRM_Manara.Repository.AppointmentSlotRepo;
import CRM_Manara.CRM_Manara.Repository.BookingRepo;
import CRM_Manara.CRM_Manara.Repository.ParentRepo;
import CRM_Manara.CRM_Manara.Repository.UserRepo;
import CRM_Manara.CRM_Manara.dto.AppointmentSlotCreateDto;
import CRM_Manara.CRM_Manara.dto.AppointmentSlotDto;
import CRM_Manara.CRM_Manara.dto.BookingDto;
import CRM_Manara.CRM_Manara.dto.BookingRequestDto;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AppointmentSlotService {

    private final UserRepo userRepo;
    private final ParentRepo parentRepo;
    private final AnimateurRepo animateurRepo;
    private final AppointmentSlotRepo appointmentSlotRepo;
    private final BookingRepo bookingRepo;
    private final AnimateurNotificationService animateurNotificationService;
    private final ParentNotificationService parentNotificationService;

    public AppointmentSlotService(UserRepo userRepo,
                                  ParentRepo parentRepo,
                                  AnimateurRepo animateurRepo,
                                  AppointmentSlotRepo appointmentSlotRepo,
                                  BookingRepo bookingRepo,
                                  AnimateurNotificationService animateurNotificationService,
                                  ParentNotificationService parentNotificationService) {
        this.userRepo = userRepo;
        this.parentRepo = parentRepo;
        this.animateurRepo = animateurRepo;
        this.appointmentSlotRepo = appointmentSlotRepo;
        this.bookingRepo = bookingRepo;
        this.animateurNotificationService = animateurNotificationService;
        this.parentNotificationService = parentNotificationService;
    }

    @Transactional(readOnly = true)
    public List<AppointmentSlotDto> listOwnSlots(String email) {
        Animateur animateur = requireAnimateur(email);
        return appointmentSlotRepo.findByAnimateurIdOrderByStartTimeAsc(animateur.getId()).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public AppointmentSlotDto createOwnSlot(String email, AppointmentSlotCreateDto request) {
        Animateur animateur = requireAnimateur(email);
        AppointmentSlot slot = buildSlot(animateur, request);
        assertNoOverlap(animateur, slot.getStartTime(), slot.getEndTime(), null);
        slot = appointmentSlotRepo.save(slot);
        return toDto(slot);
    }

    @Transactional
    public AppointmentSlotDto updateOwnSlot(String email, Long slotId, AppointmentSlotCreateDto request) {
        Animateur animateur = requireAnimateur(email);
        AppointmentSlot slot = requireOwnedSlot(animateur, slotId);
        if ("BOOKED".equals(slot.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Un creneau reserve ne peut pas etre modifie.");
        }

        AppointmentSlot updated = buildSlot(animateur, request);
        assertNoOverlap(animateur, updated.getStartTime(), updated.getEndTime(), slot.getId());
        slot.setStartTime(updated.getStartTime());
        slot.setEndTime(updated.getEndTime());
        slot.setStatus(updated.getStatus());
        slot.setParent(null);
        slot.setBookedAt(null);
        return toDto(appointmentSlotRepo.save(slot));
    }

    @Transactional
    public void deleteOwnSlot(String email, Long slotId) {
        Animateur animateur = requireAnimateur(email);
        AppointmentSlot slot = requireOwnedSlot(animateur, slotId);
        if ("BOOKED".equalsIgnoreCase(slot.getStatus()) || bookingRepo.existsBySlotIdAndStatus(slotId, "CONFIRMED")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Un creneau reserve ne peut pas etre supprime.");
        }
        detachHistoricalBookings(slotId);
        appointmentSlotRepo.delete(slot);
    }

    @Transactional
    public AppointmentSlotDto rescheduleOwnBookedSlot(String email, Long slotId, AppointmentSlotCreateDto request) {
        Animateur animateur = requireAnimateur(email);
        AppointmentSlot slot = requireOwnedSlot(animateur, slotId);
        if (!"BOOKED".equalsIgnoreCase(slot.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Seul un rendez-vous reserve peut etre reporte.");
        }

        LocalDateTime startTime = parseDateTime(request == null ? null : request.startTime(), "La date de debut est invalide.");
        LocalDateTime endTime = parseDateTime(request == null ? null : request.endTime(), "La date de fin est invalide.");
        validateTimes(startTime, endTime);
        assertNoOverlap(animateur, startTime, endTime, slot.getId());

        slot.setStartTime(startTime);
        slot.setEndTime(endTime);
        AppointmentSlot savedSlot = appointmentSlotRepo.save(slot);

        Booking activeBooking = bookingRepo.findFirstBySlotIdAndStatusOrderByCreatedAtDesc(slotId, "CONFIRMED")
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reservation introuvable."));
        activeBooking.reschedule();
        bookingRepo.save(activeBooking);

        Booking updatedBooking = bookingRepo.save(new Booking(
                savedSlot,
                savedSlot.getAnimateur(),
                savedSlot.getParent(),
                savedSlot.getStartTime().toLocalDate(),
                savedSlot.getStartTime(),
                savedSlot.getEndTime(),
                "CONFIRMED"
        ));

        String animateurName = (animateur.getPrenom() + " " + animateur.getNom()).trim();
        String parentName = savedSlot.getParent() == null
                ? "le parent"
                : (savedSlot.getParent().getPrenom() + " " + savedSlot.getParent().getNom()).trim();
        String scheduleLabel = savedSlot.getStartTime().toLocalDate()
                + " a "
                + savedSlot.getStartTime().toLocalTime().withSecond(0).withNano(0);

        if (savedSlot.getParent() != null) {
            parentNotificationService.createForParent(
                    savedSlot.getParent(),
                    "APPOINTMENT",
                    "Rendez-vous reporte",
                    "Votre rendez-vous avec " + animateurName + " a ete reporte au " + scheduleLabel + "."
            );
        }
        animateurNotificationService.createForAnimateur(
                animateur,
                "APPOINTMENT",
                "Rendez-vous reporte",
                "Le rendez-vous avec " + parentName + " a ete reporte au " + scheduleLabel + "."
        );

        return toDto(savedSlot);
    }

    @Transactional(readOnly = true)
    public List<AppointmentSlotDto> listAvailabilityForViewer(String email, Long animateurUserId) {
        User viewer = requireUser(email);
        if (viewer.getRole() == SecurityRole.ROLE_ANIMATEUR
                && viewer.getId() != null
                && viewer.getId().equals(animateurUserId)) {
            return listOwnSlots(email);
        }
        if (viewer.getRole() == SecurityRole.ROLE_PARENT) {
            Animateur animateur = findAnimateurByUserId(animateurUserId);
            return appointmentSlotRepo.findByAnimateurIdOrderByStartTimeAsc(animateur.getId()).stream()
                    .filter(slot -> slot.getStartTime() != null && slot.getStartTime().isAfter(LocalDateTime.now()))
                    .map(this::toPublicDto)
                    .toList();
        }
        return listAvailableSlotsForAnimateur(email, animateurUserId);
    }

    @Transactional(readOnly = true)
    public List<AppointmentSlotDto> listAvailableSlotsForAnimateur(String email, Long animateurUserId) {
        requireParent(email);
        Animateur animateur = findAnimateurByUserId(animateurUserId);
        return appointmentSlotRepo.findByAnimateurIdAndStatusOrderByStartTimeAsc(animateur.getId(), "AVAILABLE").stream()
                .filter(slot -> slot.getStartTime() != null && slot.getStartTime().isAfter(LocalDateTime.now()))
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public AppointmentSlotDto reserveSlot(String email, Long slotId) {
        Parent parent = requireParent(email);
        AppointmentSlot slot = appointmentSlotRepo.findById(slotId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Creneau introuvable."));

        if (!"AVAILABLE".equals(slot.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ce creneau n'est plus disponible.");
        }
        if (slot.getStartTime() == null || !slot.getStartTime().isAfter(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ce creneau n'est plus reservable.");
        }

        slot.reserve(parent);
        AppointmentSlot saved = appointmentSlotRepo.save(slot);
        bookingRepo.save(new Booking(
                saved,
                saved.getAnimateur(),
                parent,
                saved.getStartTime().toLocalDate(),
                saved.getStartTime(),
                saved.getEndTime(),
                "CONFIRMED"
        ));

        String parentName = (parent.getPrenom() + " " + parent.getNom()).trim();
        String animateurName = saved.getAnimateur() == null ? "l'animateur" : (saved.getAnimateur().getPrenom() + " " + saved.getAnimateur().getNom()).trim();

        if (saved.getAnimateur() != null) {
            animateurNotificationService.createForAnimateur(
                    saved.getAnimateur(),
                    "APPOINTMENT",
                    "Nouveau rendez-vous reserve",
                    parentName + " a reserve un appel avec vous pour le "
                            + saved.getStartTime().toLocalDate()
                            + " a "
                            + saved.getStartTime().toLocalTime().withSecond(0).withNano(0)
                            + "."
            );
        }
        parentNotificationService.createForParent(
                parent,
                "APPOINTMENT",
                "Rendez-vous reserve",
                "Votre appel avec " + animateurName + " est reserve pour le "
                        + saved.getStartTime().toLocalDate()
                        + " a "
                        + saved.getStartTime().toLocalTime().withSecond(0).withNano(0)
                        + "."
        );

        return toDto(saved);
    }

    @Transactional
    public AppointmentSlotDto reserveSlot(String email, BookingRequestDto request) {
        if (request == null || request.slotId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le creneau a reserver est requis.");
        }
        return reserveSlot(email, request.slotId());
    }

    @Transactional(readOnly = true)
    public List<BookingDto> listBookingsForAnimateur(String email, Long animateurUserId) {
        Animateur animateur = requireAnimateur(email);
        Long animateurId = animateur.getId();
        if (animateurId == null || !animateurId.equals(animateurUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acces refuse.");
        }
        return bookingRepo.findForAnimateur(animateurId).stream()
                .map(this::toBookingDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<BookingDto> listBookingsForParent(String email, Long parentUserId) {
        Parent parent = requireParent(email);
        Long parentId = parent.getId();
        if (parentId == null || !parentId.equals(parentUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acces refuse.");
        }
        return bookingRepo.findForParent(parentId).stream()
                .map(this::toBookingDto)
                .toList();
    }

    @Transactional
    public BookingDto cancelBooking(String email, Long bookingId) {
        User user = requireUser(email);
        Booking booking = bookingRepo.findById(bookingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Rendez-vous introuvable."));

        boolean allowed = switch (user.getRole()) {
            case ROLE_PARENT -> booking.getParent() != null
                    && booking.getParent().getUser() != null
                    && user.getId().equals(booking.getParent().getUser().getId());
            case ROLE_ANIMATEUR -> booking.getAnimateur() != null
                    && booking.getAnimateur().getUser() != null
                    && user.getId().equals(booking.getAnimateur().getUser().getId());
            default -> false;
        };
        if (!allowed) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acces refuse.");
        }
        if (!"CONFIRMED".equalsIgnoreCase(booking.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ce rendez-vous est deja annule.");
        }

        booking.cancel();
        Booking savedBooking = bookingRepo.save(booking);

        AppointmentSlot slot = booking.getSlot();
        if (slot != null && "BOOKED".equalsIgnoreCase(slot.getStatus())) {
            slot.clearReservation();
            appointmentSlotRepo.save(slot);
        }

        String animateurName = booking.getAnimateur() == null
                ? "l'animateur"
                : (booking.getAnimateur().getPrenom() + " " + booking.getAnimateur().getNom()).trim();
        String parentName = booking.getParent() == null
                ? "le parent"
                : (booking.getParent().getPrenom() + " " + booking.getParent().getNom()).trim();
        String scheduleLabel = booking.getStartTime().toLocalDate() + " a "
                + booking.getStartTime().toLocalTime().withSecond(0).withNano(0);
        boolean cancelledByParent = user.getRole() == SecurityRole.ROLE_PARENT;

        if (booking.getAnimateur() != null) {
            animateurNotificationService.createForAnimateur(
                    booking.getAnimateur(),
                    "APPOINTMENT",
                    "Rendez-vous annule",
                    (cancelledByParent ? parentName : "Vous") + " a annule le rendez-vous prevu le " + scheduleLabel + "."
            );
        }
        if (booking.getParent() != null) {
            parentNotificationService.createForParent(
                    booking.getParent(),
                    "APPOINTMENT",
                    "Rendez-vous annule",
                    "Le rendez-vous avec " + animateurName + " prevu le " + scheduleLabel + " a ete annule."
            );
        }

        return toBookingDto(savedBooking);
    }

    @Transactional
    public BookingDto rescheduleBooking(String email, Long bookingId, BookingRequestDto request) {
        User user = requireUser(email);
        if (request == null || request.slotId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le nouveau creneau est requis.");
        }

        Booking booking = bookingRepo.findById(bookingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Rendez-vous introuvable."));
        boolean allowed = switch (user.getRole()) {
            case ROLE_PARENT -> booking.getParent() != null
                    && booking.getParent().getUser() != null
                    && user.getId().equals(booking.getParent().getUser().getId());
            case ROLE_ANIMATEUR -> booking.getAnimateur() != null
                    && booking.getAnimateur().getUser() != null
                    && user.getId().equals(booking.getAnimateur().getUser().getId());
            default -> false;
        };
        if (!allowed) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acces refuse.");
        }
        if (!"CONFIRMED".equalsIgnoreCase(booking.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Seul un rendez-vous confirme peut etre deplace.");
        }

        AppointmentSlot currentSlot = booking.getSlot();
        AppointmentSlot newSlot = appointmentSlotRepo.findById(request.slotId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Nouveau creneau introuvable."));

        if (!"AVAILABLE".equalsIgnoreCase(newSlot.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ce nouveau creneau n'est pas disponible.");
        }
        if (newSlot.getStartTime() == null || !newSlot.getStartTime().isAfter(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ce nouveau creneau n'est plus reservable.");
        }
        if (booking.getAnimateur() == null
                || newSlot.getAnimateur() == null
                || !booking.getAnimateur().getId().equals(newSlot.getAnimateur().getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le nouveau creneau doit appartenir au meme animateur.");
        }

        booking.reschedule();
        bookingRepo.save(booking);

        if (currentSlot != null && "BOOKED".equalsIgnoreCase(currentSlot.getStatus())) {
            currentSlot.clearReservation();
            appointmentSlotRepo.save(currentSlot);
        }

        Parent parent = booking.getParent();
        if (parent == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le parent lie a ce rendez-vous est introuvable.");
        }
        newSlot.reserve(parent);
        AppointmentSlot savedSlot = appointmentSlotRepo.save(newSlot);
        Booking newBooking = bookingRepo.save(new Booking(
                savedSlot,
                savedSlot.getAnimateur(),
                parent,
                savedSlot.getStartTime().toLocalDate(),
                savedSlot.getStartTime(),
                savedSlot.getEndTime(),
                "CONFIRMED"
        ));

        String parentName = (parent.getPrenom() + " " + parent.getNom()).trim();
        String animateurName = savedSlot.getAnimateur() == null
                ? "l'animateur"
                : (savedSlot.getAnimateur().getPrenom() + " " + savedSlot.getAnimateur().getNom()).trim();
        boolean movedByParent = user.getRole() == SecurityRole.ROLE_PARENT;

        if (savedSlot.getAnimateur() != null) {
            animateurNotificationService.createForAnimateur(
                    savedSlot.getAnimateur(),
                    "APPOINTMENT",
                    "Rendez-vous deplace",
                    (movedByParent ? parentName : "Vous") + " a deplace le rendez-vous au "
                            + savedSlot.getStartTime().toLocalDate()
                            + " a "
                            + savedSlot.getStartTime().toLocalTime().withSecond(0).withNano(0)
                            + "."
            );
        }
        parentNotificationService.createForParent(
                parent,
                "APPOINTMENT",
                "Rendez-vous deplace",
                "Votre rendez-vous avec " + animateurName + " a ete deplace au "
                        + savedSlot.getStartTime().toLocalDate()
                        + " a "
                        + savedSlot.getStartTime().toLocalTime().withSecond(0).withNano(0)
                        + "."
        );

        return toBookingDto(newBooking);
    }

    private AppointmentSlotDto toDto(AppointmentSlot slot) {
        String animateurName = slot.getAnimateur() == null
                ? null
                : (slot.getAnimateur().getPrenom() + " " + slot.getAnimateur().getNom()).trim();
        Long animateurUserId = slot.getAnimateur() != null && slot.getAnimateur().getUser() != null
                ? slot.getAnimateur().getUser().getId()
                : null;
        String parentName = slot.getParent() == null
                ? null
                : (slot.getParent().getPrenom() + " " + slot.getParent().getNom()).trim();
        Long parentUserId = slot.getParent() != null && slot.getParent().getUser() != null
                ? slot.getParent().getUser().getId()
                : null;
        return new AppointmentSlotDto(
                slot.getId(),
                animateurUserId,
                animateurName,
                parentUserId,
                parentName,
                slot.getStartTime(),
                slot.getEndTime(),
                slot.getStatus(),
                slot.getBookedAt()
        );
    }

    private AppointmentSlotDto toPublicDto(AppointmentSlot slot) {
        AppointmentSlotDto dto = toDto(slot);
        return new AppointmentSlotDto(
                dto.id(),
                dto.animateurUserId(),
                dto.animateurName(),
                null,
                null,
                dto.startTime(),
                dto.endTime(),
                dto.status(),
                dto.bookedAt()
        );
    }

    private BookingDto toBookingDto(Booking booking) {
        Long animateurUserId = booking.getAnimateur() != null && booking.getAnimateur().getUser() != null
                ? booking.getAnimateur().getUser().getId()
                : null;
        String animateurName = booking.getAnimateur() == null
                ? null
                : (booking.getAnimateur().getPrenom() + " " + booking.getAnimateur().getNom()).trim();
        Long parentUserId = booking.getParent() != null && booking.getParent().getUser() != null
                ? booking.getParent().getUser().getId()
                : null;
        String parentName = booking.getParent() == null
                ? null
                : (booking.getParent().getPrenom() + " " + booking.getParent().getNom()).trim();
        String childName = deriveChildName(booking.getParent());
        Long slotId = booking.getSlot() == null ? null : booking.getSlot().getId();
        return new BookingDto(
                booking.getId(),
                slotId,
                animateurUserId,
                animateurName,
                parentUserId,
                parentName,
                childName,
                booking.getDate(),
                booking.getStartTime(),
                booking.getEndTime(),
                booking.getStatus(),
                booking.getCreatedAt(),
                booking.getUpdatedAt(),
                booking.getCancelledAt()
        );
    }

    private AppointmentSlot buildSlot(Animateur animateur, AppointmentSlotCreateDto request) {
        LocalDateTime startTime = parseDateTime(request == null ? null : request.startTime(), "La date de debut est invalide.");
        LocalDateTime endTime = parseDateTime(request == null ? null : request.endTime(), "La date de fin est invalide.");
        validateTimes(startTime, endTime);
        String status = normalizeCreatableStatus(request == null ? null : request.status());
        return new AppointmentSlot(animateur, startTime, endTime, status);
    }

    private AppointmentSlot requireOwnedSlot(Animateur animateur, Long slotId) {
        AppointmentSlot slot = appointmentSlotRepo.findById(slotId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Creneau introuvable."));
        if (slot.getAnimateur() == null || !slot.getAnimateur().getId().equals(animateur.getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Creneau introuvable.");
        }
        return slot;
    }

    private void assertNoOverlap(Animateur animateur, LocalDateTime startTime, LocalDateTime endTime, Long excludedSlotId) {
        boolean overlaps = appointmentSlotRepo.findByAnimateurIdOrderByStartTimeAsc(animateur.getId()).stream()
                .filter(existing -> existing.getId() != null && (excludedSlotId == null || !existing.getId().equals(excludedSlotId)))
                .anyMatch(existing -> existing.getStartTime() != null
                        && existing.getEndTime() != null
                        && startTime.isBefore(existing.getEndTime())
                        && endTime.isAfter(existing.getStartTime()));
        if (overlaps) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le creneau chevauche deja une autre disponibilite.");
        }
    }

    private Animateur requireAnimateur(String email) {
        User user = requireUser(email);
        if (user.getRole() != SecurityRole.ROLE_ANIMATEUR) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acces refuse.");
        }
        return animateurRepo.findByUser(user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Animateur introuvable."));
    }

    private Parent requireParent(String email) {
        User user = requireUser(email);
        if (user.getRole() != SecurityRole.ROLE_PARENT) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acces refuse.");
        }
        return parentRepo.findByUser(user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Parent introuvable."));
    }

    private Animateur findAnimateurByUserId(Long animateurUserId) {
        if (animateurUserId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Animateur requis.");
        }
        User user = userRepo.findById(animateurUserId)
                .filter(User::isEnabled)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Animateur introuvable."));
        if (user.getRole() != SecurityRole.ROLE_ANIMATEUR) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le contact cible n'est pas un animateur.");
        }
        return animateurRepo.findByUser(user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Animateur introuvable."));
    }

    private User requireUser(String email) {
        return userRepo.findByEmail(email)
                .filter(User::isEnabled)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur introuvable."));
    }

    private LocalDateTime parseDateTime(String raw, String message) {
        try {
            if (raw == null || raw.isBlank()) {
                throw new IllegalArgumentException();
            }
            return LocalDateTime.parse(raw.trim());
        } catch (Exception exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message, exception);
        }
    }

    private void validateTimes(LocalDateTime startTime, LocalDateTime endTime) {
        if (!endTime.isAfter(startTime)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La fin doit etre apres le debut.");
        }
        if (!startTime.isAfter(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le creneau doit etre dans le futur.");
        }
    }

    private String normalizeCreatableStatus(String rawStatus) {
        String normalized = rawStatus == null ? "AVAILABLE" : rawStatus.trim().toUpperCase();
        if (!"AVAILABLE".equals(normalized) && !"BLOCKED".equals(normalized)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le statut du creneau est invalide.");
        }
        return normalized;
    }

    private String deriveChildName(Parent parent) {
        if (parent == null || parent.getEnfants() == null || parent.getEnfants().isEmpty()) {
            return null;
        }
        List<Enfant> activeChildren = parent.getEnfants().stream()
                .filter(Enfant::isActive)
                .toList();
        if (activeChildren.size() == 1) {
            Enfant child = activeChildren.get(0);
            return (child.getPrenom() + " " + child.getNom()).trim();
        }
        if (activeChildren.isEmpty() && parent.getEnfants().size() == 1) {
            Enfant child = parent.getEnfants().get(0);
            return (child.getPrenom() + " " + child.getNom()).trim();
        }
        return null;
    }

    private void detachHistoricalBookings(Long slotId) {
        List<Booking> historicalBookings = bookingRepo.findBySlotId(slotId).stream()
                .filter(booking -> !"CONFIRMED".equalsIgnoreCase(booking.getStatus()))
                .toList();
        if (historicalBookings.isEmpty()) {
            return;
        }
        for (Booking booking : historicalBookings) {
            booking.detachSlot();
        }
        bookingRepo.saveAll(historicalBookings);
    }
}
