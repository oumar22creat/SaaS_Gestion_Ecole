package com.schoolsaas.messaging;

import com.schoolsaas.auth.AuthenticatedPrincipal;
import com.schoolsaas.common.ApiException;
import com.schoolsaas.common.ApiResponse;
import com.schoolsaas.messaging.dto.ConversationCreateRequest;
import com.schoolsaas.messaging.dto.ConversationResponse;
import com.schoolsaas.messaging.dto.MessageCreateRequest;
import com.schoolsaas.messaging.dto.MessageResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Messagerie interne — cahier-des-charges.md §15, ROADMAP.md 2.4. */
@RestController
@RequestMapping("/api/v1/conversations")
public class MessagingController {

    private final MessagingService messagingService;

    public MessagingController(MessagingService messagingService) {
        this.messagingService = messagingService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ConversationResponse> create(@Valid @RequestBody ConversationCreateRequest request) {
        if (request.announcement()) {
            requireAnnouncementRole();
        }
        Conversation conversation = messagingService.createConversation(request, currentUserId());
        return ApiResponse.of(toResponse(conversation));
    }

    @GetMapping
    public ApiResponse<List<ConversationResponse>> list() {
        List<ConversationResponse> data = messagingService.listForUser(currentUserId()).stream().map(this::toResponse).toList();
        return ApiResponse.of(data);
    }

    @GetMapping("/{id}")
    public ApiResponse<ConversationResponse> getById(@PathVariable Long id) {
        return ApiResponse.of(toResponse(messagingService.getConversationForParticipant(id, currentUserId())));
    }

    @PostMapping("/{id}/messages")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<MessageResponse> sendMessage(@PathVariable Long id, @Valid @RequestBody MessageCreateRequest request) {
        return ApiResponse.of(MessageResponse.from(messagingService.sendMessage(id, request, currentUserId())));
    }

    @GetMapping("/{id}/messages")
    public ApiResponse<List<MessageResponse>> listMessages(
            @PathVariable Long id, @RequestParam(required = false) String search, Pageable pageable) {
        if (search != null) {
            List<MessageResponse> data =
                    messagingService.searchMessages(id, currentUserId(), search).stream().map(MessageResponse::from).toList();
            return ApiResponse.of(data);
        }
        Page<Message> page = messagingService.listMessages(id, currentUserId(), pageable);
        List<MessageResponse> data = page.map(MessageResponse::from).getContent();
        return ApiResponse.of(data, new ApiResponse.PageMeta(page.getNumber() + 1, page.getSize(), page.getTotalElements()));
    }

    @PostMapping("/{id}/read")
    public void markRead(@PathVariable Long id) {
        messagingService.markRead(id, currentUserId());
    }

    private ConversationResponse toResponse(Conversation conversation) {
        Long userId = currentUserId();
        return ConversationResponse.from(
                conversation,
                messagingService.participantIdsOf(conversation.getId()),
                messagingService.unreadCountFor(conversation.getId(), userId));
    }

    private void requireAnnouncementRole() {
        String role = principal().role();
        if (!role.equals("ADMIN") && !role.equals("DIRECTION")) {
            throw ApiException.forbidden("ANNOUNCEMENT_NOT_ALLOWED", "Seuls Admin/Direction peuvent créer une annonce");
        }
    }

    private Long currentUserId() {
        return principal().subjectId();
    }

    private AuthenticatedPrincipal principal() {
        return (AuthenticatedPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}
