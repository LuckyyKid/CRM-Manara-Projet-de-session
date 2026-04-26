package CRM_Manara.CRM_Manara.service;

import CRM_Manara.CRM_Manara.Model.Entity.Administrateurs;
import CRM_Manara.CRM_Manara.Model.Entity.Animateur;
import CRM_Manara.CRM_Manara.Model.Entity.ChatConversation;
import CRM_Manara.CRM_Manara.Model.Entity.ChatMessage;
import CRM_Manara.CRM_Manara.Model.Entity.Inscription;
import CRM_Manara.CRM_Manara.Model.Entity.Parent;
import CRM_Manara.CRM_Manara.Model.Entity.User;
import CRM_Manara.CRM_Manara.Model.Entity.Enum.SecurityRole;
import CRM_Manara.CRM_Manara.Repository.AdminRepo;
import CRM_Manara.CRM_Manara.Repository.AnimateurRepo;
import CRM_Manara.CRM_Manara.Repository.ChatConversationRepo;
import CRM_Manara.CRM_Manara.Repository.ChatMessageRepo;
import CRM_Manara.CRM_Manara.Repository.InscriptionRepo;
import CRM_Manara.CRM_Manara.Repository.ParentRepo;
import CRM_Manara.CRM_Manara.Repository.UserRepo;
import CRM_Manara.CRM_Manara.dto.ChatConversationDetailDto;
import CRM_Manara.CRM_Manara.dto.ChatConversationSummaryDto;
import CRM_Manara.CRM_Manara.dto.ChatMessageDto;
import CRM_Manara.CRM_Manara.dto.ChatParticipantDto;
import CRM_Manara.CRM_Manara.dto.SendChatMessageRequestDto;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ChatService {

    private static final long CHAT_EMAIL_COOLDOWN_MINUTES = 15;

    private final UserRepo userRepo;
    private final ParentRepo parentRepo;
    private final AnimateurRepo animateurRepo;
    private final AdminRepo adminRepo;
    private final InscriptionRepo inscriptionRepo;
    private final ChatConversationRepo chatConversationRepo;
    private final ChatMessageRepo chatMessageRepo;
    private final RealtimeService realtimeService;
    private final EmailService emailService;
    private final ConcurrentHashMap<String, Instant> chatEmailCooldowns = new ConcurrentHashMap<>();

    public ChatService(UserRepo userRepo,
                       ParentRepo parentRepo,
                       AnimateurRepo animateurRepo,
                       AdminRepo adminRepo,
                       InscriptionRepo inscriptionRepo,
                       ChatConversationRepo chatConversationRepo,
                       ChatMessageRepo chatMessageRepo,
                       RealtimeService realtimeService,
                       EmailService emailService) {
        this.userRepo = userRepo;
        this.parentRepo = parentRepo;
        this.animateurRepo = animateurRepo;
        this.adminRepo = adminRepo;
        this.inscriptionRepo = inscriptionRepo;
        this.chatConversationRepo = chatConversationRepo;
        this.chatMessageRepo = chatMessageRepo;
        this.realtimeService = realtimeService;
        this.emailService = emailService;
    }

    @Transactional(readOnly = true)
    public List<ChatParticipantDto> listAvailableContacts(String email) {
        User currentUser = requireUser(email);
        return allowedContactUsers(currentUser).stream()
                .map(this::toParticipantDto)
                .sorted(Comparator.comparing(ChatParticipantDto::displayName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ChatConversationSummaryDto> listConversations(String email) {
        User currentUser = requireUser(email);
        List<ChatConversation> conversations = chatConversationRepo.findAllForUser(currentUser.getId());
        List<Long> conversationIds = conversations.stream().map(ChatConversation::getId).toList();
        Map<Long, Long> unreadCounts = loadUnreadCounts(currentUser.getId(), conversationIds);

        return conversations.stream()
                .map(conversation -> new ChatConversationSummaryDto(
                        conversation.getId(),
                        toParticipantDto(otherParticipant(conversation, currentUser)),
                        conversation.getLastMessagePreview(),
                        conversation.getLastMessageAt(),
                        unreadCounts.getOrDefault(conversation.getId(), 0L)
                ))
                .toList();
    }

    @Transactional
    public ChatConversationDetailDto getConversation(Long conversationId, String email) {
        User currentUser = requireUser(email);
        ChatConversation conversation = requireConversation(conversationId, currentUser);
        markConversationAsRead(conversation, currentUser);
        List<ChatMessageDto> messages = chatMessageRepo.findByConversationIdOrderByCreatedAtAsc(conversationId).stream()
                .map(message -> toMessageDto(message, currentUser))
                .toList();
        return new ChatConversationDetailDto(
                conversation.getId(),
                toParticipantDto(otherParticipant(conversation, currentUser)),
                0,
                messages
        );
    }

    @Transactional
    public void markConversationAsRead(Long conversationId, String email) {
        User currentUser = requireUser(email);
        ChatConversation conversation = requireConversation(conversationId, currentUser);
        markConversationAsRead(conversation, currentUser);
    }

    @Transactional
    public ChatMessageDto sendMessage(String email, SendChatMessageRequestDto request) {
        User sender = requireUser(email);
        if (request == null || request.body() == null || request.body().isBlank()) {
            throw new IllegalArgumentException("Le message est obligatoire.");
        }

        User recipient = resolveRecipient(sender, request);
        if (recipient.getId().equals(sender.getId())) {
            throw new IllegalArgumentException("Vous ne pouvez pas vous ecrire a vous-meme.");
        }

        ChatConversation conversation = request.conversationId() == null
                ? findOrCreateConversation(sender, recipient)
                : requireConversation(request.conversationId(), sender);

        User actualRecipient = otherParticipant(conversation, sender);
        if (!actualRecipient.getId().equals(recipient.getId())) {
            throw new IllegalArgumentException("Le destinataire ne correspond pas a la conversation.");
        }

        ChatMessage message = new ChatMessage(conversation, sender, actualRecipient, request.body().trim());
        conversation.touchWithPreview(message.getBody());
        chatConversationRepo.save(conversation);
        ChatMessage savedMessage = chatMessageRepo.save(message);

        ChatMessageDto senderDto = toMessageDto(savedMessage, sender);
        ChatMessageDto recipientDto = toMessageDto(savedMessage, actualRecipient);

        realtimeService.sendToUser(sender.getEmail(), "chat-message", senderDto);
        realtimeService.sendToUser(actualRecipient.getEmail(), "chat-message", recipientDto);
        realtimeService.sendSidebarCounts(sender.getEmail());
        realtimeService.sendSidebarCounts(actualRecipient.getEmail());
        String recipientEmail = actualRecipient.getEmail();
        Instant lastSent = chatEmailCooldowns.get(recipientEmail);
        if (lastSent == null || lastSent.isBefore(Instant.now().minus(CHAT_EMAIL_COOLDOWN_MINUTES, ChronoUnit.MINUTES))) {
            emailService.sendChatMessageNotificationEmail(recipientEmail);
            chatEmailCooldowns.put(recipientEmail, Instant.now());
        }

        return senderDto;
    }

    private void markConversationAsRead(ChatConversation conversation, User currentUser) {
        List<ChatMessage> unreadMessages = chatMessageRepo
                .findByConversationIdAndRecipientIdAndReadStatusFalse(conversation.getId(), currentUser.getId());
        if (unreadMessages.isEmpty()) {
            return;
        }
        unreadMessages.forEach(message -> message.setReadStatus(true));
        chatMessageRepo.saveAll(unreadMessages);
        realtimeService.sendSidebarCounts(currentUser.getEmail());
    }

    private User resolveRecipient(User sender, SendChatMessageRequestDto request) {
        if (request.conversationId() != null) {
            ChatConversation conversation = requireConversation(request.conversationId(), sender);
            return otherParticipant(conversation, sender);
        }
        if (request.recipientUserId() == null) {
            throw new IllegalArgumentException("Le destinataire est obligatoire.");
        }
        return allowedContactUsers(sender).stream()
                .filter(user -> Objects.equals(user.getId(), request.recipientUserId()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Ce destinataire n'est pas accessible."));
    }

    private ChatConversation findOrCreateConversation(User firstUser, User secondUser) {
        User participantOne = firstUser.getId() < secondUser.getId() ? firstUser : secondUser;
        User participantTwo = firstUser.getId() < secondUser.getId() ? secondUser : firstUser;
        return chatConversationRepo.findByParticipantOneIdAndParticipantTwoId(participantOne.getId(), participantTwo.getId())
                .orElseGet(() -> chatConversationRepo.save(new ChatConversation(participantOne, participantTwo)));
    }

    private Map<Long, Long> loadUnreadCounts(Long userId, List<Long> conversationIds) {
        if (conversationIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, Long> unreadCounts = new LinkedHashMap<>();
        for (Object[] row : chatMessageRepo.countUnreadByConversationIds(userId, conversationIds)) {
            if (row.length < 2 || !(row[0] instanceof Long conversationId) || !(row[1] instanceof Number count)) {
                continue;
            }
            unreadCounts.put(conversationId, count.longValue());
        }
        return unreadCounts;
    }

    private ChatConversation requireConversation(Long conversationId, User currentUser) {
        ChatConversation conversation = chatConversationRepo.findDetailedById(conversationId)
                .orElseThrow(() -> new IllegalArgumentException("Conversation introuvable."));
        boolean ownsConversation = Objects.equals(conversation.getParticipantOne().getId(), currentUser.getId())
                || Objects.equals(conversation.getParticipantTwo().getId(), currentUser.getId());
        if (!ownsConversation) {
            throw new IllegalArgumentException("Conversation introuvable.");
        }
        return conversation;
    }

    private User otherParticipant(ChatConversation conversation, User currentUser) {
        return Objects.equals(conversation.getParticipantOne().getId(), currentUser.getId())
                ? conversation.getParticipantTwo()
                : conversation.getParticipantOne();
    }

    private List<User> allowedContactUsers(User currentUser) {
        return switch (currentUser.getRole()) {
            case ROLE_PARENT -> buildParentContacts(currentUser);
            case ROLE_ANIMATEUR -> buildAnimateurContacts(currentUser);
            case ROLE_ADMIN -> buildAdminContacts(currentUser);
        };
    }

    private List<User> buildParentContacts(User currentUser) {
        Parent parent = parentRepo.findByUser(currentUser)
                .orElseThrow(() -> new IllegalArgumentException("Parent introuvable."));
        Map<Long, User> contacts = new LinkedHashMap<>();
        for (Inscription inscription : inscriptionRepo.findByParentId(parent.getId())) {
            if (inscription.getAnimation() == null || inscription.getAnimation().getAnimateur() == null) {
                continue;
            }
            User animateurUser = inscription.getAnimation().getAnimateur().getUser();
            if (animateurUser != null && animateurUser.isEnabled()) {
                contacts.put(animateurUser.getId(), animateurUser);
            }
        }
        for (Administrateurs admin : adminRepo.findAll()) {
            User adminUser = admin.getUser();
            if (adminUser != null && adminUser.isEnabled()) {
                contacts.put(adminUser.getId(), adminUser);
            }
        }
        return new ArrayList<>(contacts.values());
    }

    private List<User> buildAnimateurContacts(User currentUser) {
        Animateur animateur = animateurRepo.findByUser(currentUser)
                .orElseThrow(() -> new IllegalArgumentException("Animateur introuvable."));
        Map<Long, User> contacts = new LinkedHashMap<>();
        for (Inscription inscription : inscriptionRepo.findByAnimateurId(animateur.getId())) {
            if (inscription.getEnfant() == null || inscription.getEnfant().getParent() == null) {
                continue;
            }
            User parentUser = inscription.getEnfant().getParent().getUser();
            if (parentUser != null && parentUser.isEnabled()) {
                contacts.put(parentUser.getId(), parentUser);
            }
        }
        return new ArrayList<>(contacts.values());
    }

    private List<User> buildAdminContacts(User currentUser) {
        return parentRepo.findAllWithUserAndEnfants().stream()
                .map(Parent::getUser)
                .filter(Objects::nonNull)
                .filter(User::isEnabled)
                .filter(user -> !Objects.equals(user.getId(), currentUser.getId()))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private ChatParticipantDto toParticipantDto(User user) {
        if (user == null) {
            return null;
        }
        if (user.getRole() == SecurityRole.ROLE_PARENT) {
            Parent parent = parentRepo.findByUser(user).orElse(null);
            String displayName = parent == null ? user.getEmail() : (parent.getPrenom() + " " + parent.getNom()).trim();
            return new ChatParticipantDto(user.getId(), parent == null ? null : parent.getId(), user.getRole().name(), displayName, user.getEmail());
        }
        if (user.getRole() == SecurityRole.ROLE_ANIMATEUR) {
            Animateur animateur = animateurRepo.findByUser(user).orElse(null);
            String displayName = animateur == null ? user.getEmail() : (animateur.getPrenom() + " " + animateur.getNom()).trim();
            return new ChatParticipantDto(user.getId(), animateur == null ? null : animateur.getId(), user.getRole().name(), displayName, user.getEmail());
        }
        Administrateurs admin = adminRepo.findByUser(user).orElse(null);
        String displayName = admin == null ? user.getEmail() : (admin.getPrenom() + " " + admin.getNom()).trim();
        return new ChatParticipantDto(user.getId(), admin == null ? null : admin.getId(), user.getRole().name(), displayName, user.getEmail());
    }

    private ChatMessageDto toMessageDto(ChatMessage message, User viewer) {
        return new ChatMessageDto(
                message.getId(),
                message.getConversation().getId(),
                toParticipantDto(message.getSender()),
                toParticipantDto(message.getRecipient()),
                message.getBody(),
                message.getCreatedAt(),
                Objects.equals(message.getSender().getId(), viewer.getId()),
                message.isReadStatus()
        );
    }

    private User requireUser(String email) {
        return userRepo.findByEmail(email)
                .filter(User::isEnabled)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable."));
    }
}
