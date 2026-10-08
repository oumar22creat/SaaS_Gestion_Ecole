package com.schoolsaas.student;

import com.schoolsaas.common.ApiException;
import com.schoolsaas.document.StorageGateway;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * Portrait de l'élève, utilisé par la carte d'identité scolaire.
 *
 * <p>Mêmes garde-fous que le logo de l'établissement, et pour la même raison : le fichier
 * finit dans un PDF fabriqué par le serveur. Un fichier quelconque renommé en {@code .png}
 * passerait le contrôle du navigateur puis ferait échouer l'impression de toutes les cartes
 * d'une classe, des semaines après le téléversement, sans que personne ne sache pourquoi.
 */
@Service
public class StudentPhotoService {

    private static final List<String> TYPES_ACCEPTES = List.of("image/png", "image/jpeg");

    /**
     * 3 Mo : un portrait d'identité pris au téléphone y tient largement. Plus haut, une
     * classe de cinquante élèves immobiliserait le serveur le temps d'assembler les cartes.
     */
    private static final long TAILLE_MAX = 3L * 1024 * 1024;

    private final StudentRepository studentRepository;
    private final StorageGateway storageGateway;

    public StudentPhotoService(StudentRepository studentRepository, StorageGateway storageGateway) {
        this.studentRepository = studentRepository;
        this.storageGateway = storageGateway;
    }

    @Transactional
    public Student televerser(Long studentId, MultipartFile fichier) {
        if (fichier == null || fichier.isEmpty()) {
            throw ApiException.unprocessable("EMPTY_PHOTO", "Aucun fichier reçu");
        }
        if (fichier.getSize() > TAILLE_MAX) {
            throw ApiException.unprocessable("PHOTO_TOO_LARGE", "La photo ne doit pas dépasser 3 Mo");
        }
        String type = fichier.getContentType();
        if (type == null || !TYPES_ACCEPTES.contains(type.toLowerCase())) {
            throw ApiException.unprocessable("UNSUPPORTED_PHOTO_FORMAT", "Formats acceptés : PNG ou JPEG");
        }

        byte[] contenu = lire(fichier);
        if (!estPng(contenu) && !estJpeg(contenu)) {
            throw ApiException.unprocessable(
                    "UNSUPPORTED_PHOTO_FORMAT", "Ce fichier n'est pas une image PNG ou JPEG");
        }

        Student eleve = trouver(studentId);
        String ancienneCle = eleve.getPhotoStorageKey();
        eleve.setPhotoStorageKey(storageGateway.store(contenu, "photo-eleve"));
        studentRepository.save(eleve);

        // L'ancien portrait n'a plus d'usage. Le garder ferait grossir le stockage à chaque
        // rentrée, sans que rien ne le nettoie jamais.
        if (ancienneCle != null) {
            storageGateway.delete(ancienneCle);
        }
        return eleve;
    }

    @Transactional
    public Student retirer(Long studentId) {
        Student eleve = trouver(studentId);
        String cle = eleve.getPhotoStorageKey();
        eleve.setPhotoStorageKey(null);
        studentRepository.save(eleve);
        if (cle != null) {
            storageGateway.delete(cle);
        }
        return eleve;
    }

    /**
     * Contenu du portrait, vide s'il n'y en a pas.
     *
     * <p>Un fichier introuvable est traité comme une absence de photo : la carte d'un élève
     * s'imprime avec un cadre vide plutôt que de faire échouer la liasse de toute la classe.
     */
    public Optional<byte[]> contenu(Student eleve) {
        if (eleve.getPhotoStorageKey() == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(storageGateway.retrieve(eleve.getPhotoStorageKey()));
        } catch (RuntimeException e) {
            return Optional.empty();
        }
    }

    /** Les portraits de plusieurs élèves d'un coup, pour l'impression des cartes d'une classe. */
    public Map<Long, byte[]> contenus(List<Student> eleves) {
        return eleves.stream()
                .map(eleve -> Map.entry(eleve.getId(), contenu(eleve)))
                .filter(entree -> entree.getValue().isPresent())
                .collect(java.util.stream.Collectors.toMap(Map.Entry::getKey, entree -> entree.getValue().get()));
    }

    private Student trouver(Long id) {
        return studentRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("STUDENT_NOT_FOUND", "Élève introuvable"));
    }

    private static byte[] lire(MultipartFile fichier) {
        try {
            return fichier.getBytes();
        } catch (java.io.IOException e) {
            throw ApiException.unprocessable("PHOTO_READ_FAILED", "Impossible de lire le fichier");
        }
    }

    private static boolean estPng(byte[] contenu) {
        return contenu.length > 8
                && (contenu[0] & 0xFF) == 0x89 && contenu[1] == 'P' && contenu[2] == 'N' && contenu[3] == 'G';
    }

    private static boolean estJpeg(byte[] contenu) {
        return contenu.length > 3
                && (contenu[0] & 0xFF) == 0xFF && (contenu[1] & 0xFF) == 0xD8 && (contenu[2] & 0xFF) == 0xFF;
    }
}
