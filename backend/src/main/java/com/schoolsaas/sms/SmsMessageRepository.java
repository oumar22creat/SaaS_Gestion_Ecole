package com.schoolsaas.sms;

import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SmsMessageRepository extends JpaRepository<SmsMessage, Long> {

    List<SmsMessage> findAllByCreatedAtBetweenOrderByCreatedAtDesc(Instant from, Instant to);
}
