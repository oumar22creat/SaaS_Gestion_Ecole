package com.schoolsaas.sms;

import com.schoolsaas.notification.NotificationType;
import com.schoolsaas.parent.Parent;
import com.schoolsaas.parent.ParentRepository;
import com.schoolsaas.parent.StudentParent;
import com.schoolsaas.parent.StudentParentRepository;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Envoi de SMS aux familles (cahier-des-charges.md §16).
 *
 * <p>Le SMS est le canal qui atteint réellement les parents : tous n'ont pas de smartphone,
 * et l'application mobile suppose une connexion. C'est donc lui, et non la notification push,
 * qui porte la promesse de communication du produit.
 */
@Service
public class SmsService {

    private static final Logger log = LoggerFactory.getLogger(SmsService.class);

    private final SmsGateway smsGateway;
    private final SmsMessageRepository smsMessageRepository;
    private final StudentParentRepository studentParentRepository;
    private final ParentRepository parentRepository;
    private final SmsProperties properties;

    public SmsService(
            SmsGateway smsGateway,
            SmsMessageRepository smsMessageRepository,
            StudentParentRepository studentParentRepository,
            ParentRepository parentRepository,
            SmsProperties properties) {
        this.smsGateway = smsGateway;
        this.smsMessageRepository = smsMessageRepository;
        this.studentParentRepository = studentParentRepository;
        this.parentRepository = parentRepository;
        this.properties = properties;
    }

    /**
     * Prévient les responsables d'un élève. Le contact principal d'abord : c'est celui que la
     * famille a désigné, et sur une fratrie nombreuse cela évite d'envoyer le même message à
     * tout le monde.
     *
     * <p>Sans numéro exploitable, rien n'est envoyé et rien n'est tracé — inscrire un échec
     * pour une donnée absente encombrerait le journal sans rien apprendre.
     */
    public void notifyGuardians(Long studentId, NotificationType type, String body) {
        List<String> numbers = guardianNumbers(studentId);
        if (numbers.isEmpty()) {
            log.debug("Aucun numéro de téléphone pour l'élève {} : SMS non envoyé", studentId);
            return;
        }
        numbers.forEach(number -> send(number, body, type));
    }

    /**
     * Envoie un SMS et en garde la trace, quelle qu'en soit l'issue.
     *
     * <p>La transaction est séparée de celle de l'appelant : un refus du fournisseur ne doit
     * pas annuler l'appel qui vient d'être validé, et la trace de l'échec doit survivre même
     * si la transaction appelante échoue ensuite pour une autre raison.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public SmsMessage send(String rawRecipient, String body, NotificationType type) {
        String recipient = normalise(rawRecipient);
        SmsMessage message = smsMessageRepository.save(
                new SmsMessage(recipient, body, type, smsGateway.providerName()));
        try {
            String reference = smsGateway.send(recipient, body);
            message.markSent(reference);
        } catch (RuntimeException e) {
            // Un SMS perdu ne doit jamais faire échouer l'opération métier qui l'a déclenché :
            // une feuille d'appel validée reste validée même si l'opérateur est injoignable.
            log.error("Échec d'envoi du SMS à {}", recipient, e);
            message.markFailed(e.getMessage());
        }
        return smsMessageRepository.save(message);
    }

    /** Numéros des responsables, contact principal en tête. */
    public List<String> guardianNumbers(Long studentId) {
        List<StudentParent> links = studentParentRepository.findAllByStudentId(studentId);
        if (links.isEmpty()) {
            return List.of();
        }
        Map<Long, Parent> parentsById = parentRepository
                .findAllById(links.stream().map(StudentParent::getParentId).toList()).stream()
                .collect(Collectors.toMap(Parent::getId, Function.identity()));

        return links.stream()
                .sorted(Comparator.comparing(StudentParent::isPrimaryContact).reversed())
                .map(link -> parentsById.get(link.getParentId()))
                .filter(parent -> parent != null && parent.getPhone() != null && !parent.getPhone().isBlank())
                .map(Parent::getPhone)
                .map(this::normalise)
                .distinct()
                .toList();
    }

    /**
     * Les numéros sont saisis au guichet comme on les dicte — « 76 00 00 00 », « 00223... » —
     * alors que les opérateurs attendent un format international. Sans cette normalisation, la
     * moitié des messages seraient refusés pour un numéro pourtant valide.
     *
     * <p>Une seule forme, appliquée à la lecture comme à l'envoi : deux variantes dont l'une
     * ignorait l'indicatif configuré avaient déjà produit des numéros divergents selon le
     * chemin emprunté.
     */
    private String normalise(String rawRecipient) {
        String defaultCountryCode = properties.defaultCountryCode();
        String digits = rawRecipient.replaceAll("[^0-9+]", "");
        if (digits.startsWith("+")) {
            return digits;
        }
        if (digits.startsWith("00")) {
            return "+" + digits.substring(2);
        }
        if (defaultCountryCode != null && !defaultCountryCode.isBlank()) {
            String prefix = defaultCountryCode.startsWith("+") ? defaultCountryCode : "+" + defaultCountryCode;
            return prefix + digits;
        }
        return digits;
    }
}
