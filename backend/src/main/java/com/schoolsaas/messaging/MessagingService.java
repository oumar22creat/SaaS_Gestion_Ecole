package com.schoolsaas.messaging;

import com.schoolsaas.auth.Role;
import com.schoolsaas.auth.User;
import com.schoolsaas.auth.UserRepository;
import com.schoolsaas.common.ApiException;
import com.schoolsaas.document.DocumentRepository;
import com.schoolsaas.messaging.dto.ConversationCreateRequest;
import com.schoolsaas.messaging.dto.MessageCreateRequest;
import com.schoolsaas.notification.NotificationDispatcher;
import com.schoolsaas.notification.NotificationType;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Messagerie interne — cahier-des-charges.md §15, ROADMAP.md 2.4.
 *
 * <p>Règles de rôle (docs/ARCHITECTURE.md ADR-019, complété) : seul le personnel ouvre une
 * conversation ou y écrit ; seules l'administration et la direction diffusent une annonce, et
 * seules elles écrivent dans une annonce — sinon n'importe quel destinataire y répondrait à
 * tout l'établissement. Les comptes famille (PARENT/STUDENT) restent destinataires des
 * annonces et peuvent les LIRE. Vérifié ici, sur le rôle en base, et pas seulement par le
 * {@code @PreAuthorize} du contrôleur : la règle tient quel que soit l'appelant.
 */
@Service
public class MessagingService {

    /** Rôles du personnel : ceux qui ont l'écran Messagerie sur le web (nav-links.ts, STAFF). */
    static final Set<Role> STAFF_ROLES = EnumSet.of(
            Role.ADMIN, Role.DIRECTION, Role.TEACHER, Role.SECRETARY, Role.VIE_SCOLAIRE, Role.ACCOUNTANT);

    /** Rôles qui engagent l'établissement auprès de tous ses usagers. */
    static final Set<Role> ANNOUNCER_ROLES = EnumSet.of(Role.ADMIN, Role.DIRECTION);

    private final ConversationRepository conversationRepository;
    private final ConversationParticipantRepository participantRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final DocumentRepository documentRepository;
    private final NotificationDispatcher notificationDispatcher;

    public MessagingService(
            ConversationRepository conversationRepository,
            ConversationParticipantRepository participantRepository,
            MessageRepository messageRepository,
            UserRepository userRepository,
            DocumentRepository documentRepository,
            NotificationDispatcher notificationDispatcher) {
        this.conversationRepository = conversationRepository;
        this.participantRepository = participantRepository;
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
        this.documentRepository = documentRepository;
        this.notificationDispatcher = notificationDispatcher;
    }

    @Transactional
    public Conversation createConversation(ConversationCreateRequest request, Long creatorUserId) {
        Role creatorRole = roleOf(creatorUserId);
        if (!STAFF_ROLES.contains(creatorRole)) {
            throw ApiException.forbidden("MESSAGING_NOT_ALLOWED", "La messagerie est réservée au personnel de l'établissement");
        }
        if (request.announcement() && !ANNOUNCER_ROLES.contains(creatorRole)) {
            throw ApiException.forbidden("ANNOUNCEMENT_NOT_ALLOWED", "Seuls Admin/Direction peuvent créer une annonce");
        }
        List<Long> participantIds;
        if (request.announcement()) {
            participantIds = userRepository.findAllByActiveTrue().stream().map(User::getId).toList();
        } else {
            if (request.participantUserIds() == null || request.participantUserIds().isEmpty()) {
                throw ApiException.badRequest(
                        "PARTICIPANTS_REQUIRED", "Au moins un destinataire est requis", List.of());
            }
            for (Long userId : request.participantUserIds()) {
                if (userRepository.findById(userId).isEmpty()) {
                    throw ApiException.notFound("USER_NOT_FOUND", "Utilisateur introuvable : " + userId);
                }
            }
            participantIds = new java.util.ArrayList<>(request.participantUserIds());
            if (!participantIds.contains(creatorUserId)) {
                participantIds.add(creatorUserId);
            }
        }

        Conversation conversation = conversationRepository.save(new Conversation(request.title(), request.announcement(), creatorUserId));
        for (Long userId : participantIds) {
            participantRepository.save(new ConversationParticipant(conversation.getId(), userId));
        }
        return conversation;
    }

    public Conversation getConversationForParticipant(Long conversationId, Long callerUserId) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> ApiException.notFound("CONVERSATION_NOT_FOUND", "Conversation introuvable"));
        requireParticipant(conversationId, callerUserId);
        return conversation;
    }

    public List<Conversation> listForUser(Long userId) {
        return participantRepository.findAllByUserId(userId).stream()
                .map(p -> conversationRepository.findById(p.getConversationId()).orElseThrow())
                .toList();
    }

    public List<Long> participantIdsOf(Long conversationId) {
        return participantRepository.findAllByConversationId(conversationId).stream()
                .map(ConversationParticipant::getUserId)
                .toList();
    }

    public long unreadCountFor(Long conversationId, Long userId) {
        ConversationParticipant participant = participantRepository.findByConversationIdAndUserId(conversationId, userId)
                .orElseThrow(() -> ApiException.forbidden("NOT_A_PARTICIPANT", "Vous ne participez pas à cette conversation"));
        Instant since = participant.getLastReadAt() != null ? participant.getLastReadAt() : Instant.EPOCH;
        return messageRepository.countByConversationIdAndCreatedAtAfter(conversationId, since);
    }

    @Transactional
    public Message sendMessage(Long conversationId, MessageCreateRequest request, Long senderId) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> ApiException.notFound("CONVERSATION_NOT_FOUND", "Conversation introuvable"));
        requireParticipant(conversationId, senderId);
        Role senderRole = roleOf(senderId);
        if (!STAFF_ROLES.contains(senderRole)) {
            throw ApiException.forbidden("MESSAGING_NOT_ALLOWED", "La messagerie est réservée au personnel de l'établissement");
        }
        if (conversation.isAnnouncement() && !ANNOUNCER_ROLES.contains(senderRole)) {
            // Une annonce compte tous les utilisateurs actifs pour participants : y répondre,
            // c'est écrire à tout l'établissement, familles comprises.
            throw ApiException.forbidden("ANNOUNCEMENT_NOT_ALLOWED", "Seuls Admin/Direction peuvent écrire dans une annonce");
        }
        if (request.attachmentDocumentId() != null && documentRepository.findById(request.attachmentDocumentId()).isEmpty()) {
            throw ApiException.notFound("DOCUMENT_NOT_FOUND", "Document introuvable");
        }
        Message message = messageRepository.save(new Message(conversationId, senderId, request.content(), request.attachmentDocumentId()));

        List<Long> recipients = participantIdsOf(conversationId).stream().filter(id -> !id.equals(senderId)).toList();
        NotificationType type = conversation.isAnnouncement() ? NotificationType.ANNOUNCEMENT : NotificationType.NEW_MESSAGE;
        notificationDispatcher.dispatch(
                type, recipients, conversation.isAnnouncement() ? "Nouvelle annonce" : "Nouveau message",
                "Conversation " + conversationId + " — message " + message.getId());
        return message;
    }

    public Page<Message> listMessages(Long conversationId, Long callerUserId, Pageable pageable) {
        requireParticipant(conversationId, callerUserId);
        return messageRepository.findAllByConversationIdOrderByCreatedAtDesc(conversationId, pageable);
    }

    public List<Message> searchMessages(Long conversationId, Long callerUserId, String search) {
        requireParticipant(conversationId, callerUserId);
        return messageRepository.findAllByConversationIdAndContentContainingIgnoreCaseOrderByCreatedAtDesc(conversationId, search);
    }

    @Transactional
    public void markRead(Long conversationId, Long callerUserId) {
        ConversationParticipant participant = participantRepository.findByConversationIdAndUserId(conversationId, callerUserId)
                .orElseThrow(() -> ApiException.forbidden("NOT_A_PARTICIPANT", "Vous ne participez pas à cette conversation"));
        participant.markRead(Instant.now());
    }

    /** Rôle relu en base (compte du tenant courant) ; un compte introuvable n'a aucun droit. */
    private Role roleOf(Long userId) {
        return userRepository.findById(userId)
                .map(User::getRole)
                .orElseThrow(() -> ApiException.forbidden("MESSAGING_NOT_ALLOWED", "Compte introuvable"));
    }

    private void requireParticipant(Long conversationId, Long userId) {
        if (participantRepository.findByConversationIdAndUserId(conversationId, userId).isEmpty()) {
            throw ApiException.forbidden("NOT_A_PARTICIPANT", "Vous ne participez pas à cette conversation");
        }
    }
}
