package com.schoolsaas.homework;

/**
 * Notification en cas de nouveau devoir (cahier-des-charges.md §13/§16). Même pattern que
 * {@code com.schoolsaas.attendance.ParentNotificationGateway} — sera consolidé dans le
 * registre centralisé de notifications de ROADMAP.md 2.5 (généralisation prévue, pas faite
 * ici pour ne pas anticiper ce module).
 */
public interface HomeworkNotificationGateway {

    void notifyNewHomework(Long schoolClassId, Long lessonId);
}
