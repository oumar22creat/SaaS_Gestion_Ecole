package com.schoolsaas.notification;

import com.schoolsaas.auth.AuthenticatedPrincipal;
import com.schoolsaas.notification.dto.DeviceTokenRegistrationRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Appareils de l'utilisateur courant, pour les notifications push.
 *
 * <p>Chacun n'enregistre que ses propres appareils : l'identifiant du compte vient du jeton
 * d'authentification, jamais du corps de la requête. Le laisser au client permettrait de
 * détourner les notifications d'autrui vers son propre téléphone.
 */
@RestController
@RequestMapping("/api/v1/device-tokens")
public class DeviceTokenController {

    private final DeviceTokenService deviceTokenService;

    public DeviceTokenController(DeviceTokenService deviceTokenService) {
        this.deviceTokenService = deviceTokenService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void register(@Valid @RequestBody DeviceTokenRegistrationRequest request) {
        deviceTokenService.enregistrer(currentUserId(), request.token(), request.platform());
    }

    @DeleteMapping("/{token}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unregister(@PathVariable String token) {
        deviceTokenService.retirer(token);
    }

    private Long currentUserId() {
        return ((AuthenticatedPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal())
                .subjectId();
    }
}
