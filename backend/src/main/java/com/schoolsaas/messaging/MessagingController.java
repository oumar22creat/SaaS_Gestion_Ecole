package com.schoolsaas.messaging;

import com.schoolsaas.auth.AuthenticatedPrincipal;
import com.schoolsaas.auth.User;
import com.schoolsaas.common.ApiException;
import com.schoolsaas.common.PagedList;
import com.schoolsaas.common.ApiResponse;
import com.schoolsaas.messaging.dto.ConversationCreateRequest;
import com.schoolsaas.messaging.dto.ConversationResponse;
import com.schoolsaas.messaging.dto.MessageCreateRequest;
import com.schoolsaas.messaging.dto.MessageResponse;
import com.schoolsaas.messaging.dto.RecipientResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Messagerie interne — cahier-des-charges.md §15, ROADMAP.md 2.4.
 *
 * <p>Écriture (créer une conversation, envoyer un message) : personnel seulement. Lecture
 * (liste, fil, messages, accusé de lecture) : tout compte de l'établissement, comptes famille
 * compris, toujours borné aux conversations dont il est participant — c'est ce qui laisse un
 * parent lire les annonces qu'il reçoit. Annonces : ADMIN/DIRECTION (règle portée par
 * {@link MessagingService}). Le Super-Admin, hors tenant, n'a pas accès.
 */
@RestController
@RequestMapping("/api/v1/conversations")
@PreAuthorize(MessagingController.TENANT_ROLES)
public class MessagingController {

    static final String STAFF_ROLES =
            "hasAnyRole('ADMIN', 'DIRECTION', 'TEACHER', 'SECRETARY', 'VIE_SCOLAIRE', 'ACCOUNTANT')";

    static final String TENANT_ROLES =
            "hasAnyRole('ADMIN', 'DIRECTION', 'TEACHER', 'SECRETARY', 'VIE_SCOLAIRE', 'ACCOUNTANT', 'PARENT', 'STUDENT')";

    private final MessagingService messagingService;

    public MessagingController(MessagingService messagingService) {
        this.messagingService = messagingService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(STAFF_ROLES)
    public ApiResponse<ConversationResponse> create(@Valid @RequestBody ConversationCreateRequest request) {
        if (request.announcement()) {
            requireAnnouncementRole();
        }
        Conversation conversation = messagingService.createConversation(request, currentUserId());
        return ApiResponse.of(toResponse(conversation));
    }

    @GetMapping
    public ApiResponse<List<ConversationResponse>> list(Pageable pageable) {
        // Les fils s'accumulent année après année sans que rien ne les archive.
        return PagedList.response(PagedList.of(
                messagingService.listForUser(currentUserId()).stream().map(this::toResponse).toList(),
                pageable));
    }

    /** Taille de page maximale de l'annuaire (le sélecteur web en demande 200). */
    static final int MAX_RECIPIENTS_PAGE_SIZE = 200;

    /**
     * Annuaire des destinataires possibles (ADR-019) : personnel seulement, sans e-mail ni
     * téléphone. Recherche optionnelle {@code search} sur le nom, pagination {@code page}/
     * {@code size} (tri fixe par nom, taille plafonnée). Chemin littéral : prioritaire sur
     * {@code /{id}}.
     */
    @GetMapping("/recipients")
    @PreAuthorize(STAFF_ROLES)
    public ApiResponse<List<RecipientResponse>> recipients(
            @RequestParam(required = false) String search, Pageable pageable) {
        Pageable bounded = PageRequest.of(
                pageable.isPaged() ? pageable.getPageNumber() : 0,
                pageable.isPaged() ? Math.min(pageable.getPageSize(), MAX_RECIPIENTS_PAGE_SIZE) : MAX_RECIPIENTS_PAGE_SIZE);
        Page<User> page = messagingService.listRecipients(currentUserId(), search, bounded);
        return ApiResponse.of(
                page.map(RecipientResponse::from).getContent(),
                new ApiResponse.PageMeta(page.getNumber() + 1, page.getSize(), page.getTotalElements()));
    }

    @GetMapping("/{id}")
    public ApiResponse<ConversationResponse> getById(@PathVariable Long id) {
        return ApiResponse.of(toResponse(messagingService.getConversationForParticipant(id, currentUserId())));
    }

    @PostMapping("/{id}/messages")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(STAFF_ROLES)
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
