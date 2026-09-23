package com.schoolsaas.tenant;

import com.schoolsaas.common.ApiException;
import com.schoolsaas.document.StorageGateway;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * Logo de l'établissement, tel qu'il apparaît sur les documents officiels.
 *
 * <p>Le logo est téléversé et stocké, jamais récupéré depuis l'URL saisie dans les réglages :
 * les PDF sont fabriqués par le serveur, et aller chercher une adresse fournie par un
 * administrateur en ferait un relais vers n'importe quelle machine du réseau interne — le
 * contenu récupéré repartant ensuite dans un PDF téléchargeable.
 */
@Service
public class TenantLogoService {

    /**
     * PNG et JPEG seulement : ce sont les deux formats que le générateur de PDF sait poser
     * sur une page. Un SVG serait accepté par le navigateur mais donnerait un document vide.
     */
    private static final List<String> ACCEPTED_TYPES = List.of("image/png", "image/jpeg");

    /** Un logo d'en-tête n'a aucune raison de peser plus : au-delà, c'est une photo mal préparée. */
    private static final long MAX_SIZE_BYTES = 2L * 1024 * 1024;

    private final TenantRepository tenantRepository;
    private final StorageGateway storageGateway;

    public TenantLogoService(TenantRepository tenantRepository, StorageGateway storageGateway) {
        this.tenantRepository = tenantRepository;
        this.storageGateway = storageGateway;
    }

    @Transactional
    public Tenant upload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw ApiException.unprocessable("EMPTY_LOGO", "Aucun fichier reçu");
        }
        if (file.getSize() > MAX_SIZE_BYTES) {
            throw ApiException.unprocessable("LOGO_TOO_LARGE", "Le logo ne doit pas dépasser 2 Mo");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ACCEPTED_TYPES.contains(contentType.toLowerCase())) {
            throw ApiException.unprocessable("UNSUPPORTED_LOGO_FORMAT", "Formats acceptés : PNG ou JPEG");
        }

        byte[] content = read(file);
        // Le type déclaré par le navigateur ne prouve rien : on vérifie la signature du
        // fichier. Sans cela, un fichier quelconque renommé en .png ferait échouer la
        // génération de TOUS les bulletins, longtemps après le téléversement.
        if (!isPng(content) && !isJpeg(content)) {
            throw ApiException.unprocessable("UNSUPPORTED_LOGO_FORMAT", "Ce fichier n'est pas une image PNG ou JPEG");
        }

        Tenant tenant = currentTenant();
        String previousKey = tenant.getLogoStorageKey();
        tenant.setLogoStorageKey(storageGateway.store(content, "logo"));
        tenantRepository.save(tenant);

        // L'ancien logo n'a plus d'usage : le garder ferait grossir le stockage à chaque
        // changement de charte, sans que rien ne le nettoie jamais.
        if (previousKey != null) {
            storageGateway.delete(previousKey);
        }
        return tenant;
    }

    @Transactional
    public Tenant remove() {
        Tenant tenant = currentTenant();
        String key = tenant.getLogoStorageKey();
        tenant.setLogoStorageKey(null);
        tenantRepository.save(tenant);
        if (key != null) {
            storageGateway.delete(key);
        }
        return tenant;
    }

    /**
     * Contenu du logo, vide si l'établissement n'en a pas.
     *
     * <p>Un fichier manquant est traité comme une absence de logo : un document officiel doit
     * pouvoir être imprimé même si le stockage a perdu l'image.
     */
    public Optional<byte[]> content(Tenant tenant) {
        if (tenant.getLogoStorageKey() == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(storageGateway.retrieve(tenant.getLogoStorageKey()));
        } catch (RuntimeException e) {
            return Optional.empty();
        }
    }

    private Tenant currentTenant() {
        return tenantRepository.findById(com.schoolsaas.tenant.TenantContext.get())
                .orElseThrow(() -> ApiException.notFound("TENANT_NOT_FOUND", "Établissement introuvable"));
    }

    private static byte[] read(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (java.io.IOException e) {
            throw ApiException.unprocessable("LOGO_READ_FAILED", "Impossible de lire le fichier");
        }
    }

    private static boolean isPng(byte[] content) {
        return content.length > 8
                && (content[0] & 0xFF) == 0x89 && content[1] == 'P' && content[2] == 'N' && content[3] == 'G';
    }

    private static boolean isJpeg(byte[] content) {
        return content.length > 3
                && (content[0] & 0xFF) == 0xFF && (content[1] & 0xFF) == 0xD8 && (content[2] & 0xFF) == 0xFF;
    }
}
