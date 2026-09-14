package com.schoolsaas.messaging;

import com.schoolsaas.auth.User;
import com.schoolsaas.auth.UserRepository;
import com.schoolsaas.common.ApiException;
import com.schoolsaas.document.DocumentRepository;
import com.schoolsaas.messaging.dto.ConversationCreateRequest;
import com.schoolsaas.messaging.dto.MessageCreateRequest;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Messagerie interne — cahier-des-charges.md §15, ROADMAP.md 2.4. */
@Service
public class MessagingService {

    private final ConversationRepository conversationRepository;
    private final ConversationParticipantRepository participantRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final DocumentRepository documentRepository;
    private final MessageNotificationGateway notificationGateway;

    public MessagingService(
            ConversationRepository conversationRepository,
            ConversationParticipantRepository participantRepository,
            MessageRepository messageRepository,
            UserRepository userRepository,
            DocumentRepository documentRepository,
            MessageNotificationGateway notificationGateway) {
        this.conversationRepository = conversationRepository;
        this.participantRepository = participantRepository;
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
        this.documentRepository = documentRepository;
        this.notificationGateway = notificationGateway;
    }

    @Transactional
    public Conversation createConversation(ConversationCreateRequest request, Long creatorUserId) {
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
        requireParticipant(conversationId, senderId);
        if (request.attachmentDocumentId() != null && documentRepository.findById(request.attachmentDocumentId()).isEmpty()) {
            throw ApiException.notFound("DOCUMENT_NOT_FOUND", "Document introuvable");
        }
        Message message = messageRepository.save(new Message(conversationId, senderId, request.content(), request.attachmentDocumentId()));

        List<Long> recipients = participantIdsOf(conversationId).stream().filter(id -> !id.equals(senderId)).toList();
        notificationGateway.notifyNewMessage(conversationId, message.getId(), recipients);
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

    private void requireParticipant(Long conversationId, Long userId) {
        if (participantRepository.findByConversationIdAndUserId(conversationId, userId).isEmpty()) {
            throw ApiException.forbidden("NOT_A_PARTICIPANT", "Vous ne participez pas à cette conversation");
        }
    }
}
