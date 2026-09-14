package com.schoolsaas.homework;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Implémentation par défaut : trace l'intention en log, n'envoie rien réellement (voir la javadoc de l'interface). */
@Component
public class LoggingHomeworkNotificationGateway implements HomeworkNotificationGateway {

    private static final Logger log = LoggerFactory.getLogger(LoggingHomeworkNotificationGateway.class);

    @Override
    public void notifyNewHomework(Long schoolClassId, Long lessonId) {
        log.info("Notification nouveau devoir (non envoyée, FCM pas encore câblé) : classe {} — séance {}", schoolClassId, lessonId);
    }
}
