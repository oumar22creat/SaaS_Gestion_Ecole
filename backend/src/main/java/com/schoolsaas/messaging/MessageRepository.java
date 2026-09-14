package com.schoolsaas.messaging;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MessageRepository extends JpaRepository<Message, Long> {

    Page<Message> findAllByConversationIdOrderByCreatedAtDesc(Long conversationId, Pageable pageable);

    List<Message> findAllByConversationIdAndContentContainingIgnoreCaseOrderByCreatedAtDesc(Long conversationId, String search);

    long countByConversationIdAndCreatedAtAfter(Long conversationId, java.time.Instant after);
}
