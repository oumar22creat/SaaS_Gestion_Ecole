package com.schoolsaas.student;

import com.schoolsaas.tenant.TenantContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.Year;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Attribue les matricules d'élèves : {@code AAAA-NNNN}, où AAAA est l'année d'inscription et
 * NNNN un numéro d'ordre propre à l'établissement, reparti à 1 chaque année.
 *
 * <p>Le format porte l'année parce qu'un matricule sert d'abord à des humains : il situe la
 * promotion d'un coup d'œil, et reste court puisqu'il ne cumule pas les années.
 *
 * <p>L'incrément est confié à PostgreSQL en une seule instruction. Un {@code MAX(...) + 1}
 * lu puis réécrit par l'application laisserait deux secrétaires inscrivant un élève au même
 * instant produire le même numéro — l'une des deux insertions échouant alors sur la
 * contrainte d'unicité, sous les yeux de l'utilisateur.
 */
@Component
public class StudentNumberGenerator {

    /** Quatre chiffres : au-delà de 9 999 inscriptions dans l'année, le numéro s'allonge. */
    private static final String FORMAT = "%d-%04d";

    /**
     * Nombre d'essais avant d'abandonner. Le compteur avance seul, mais un établissement ayant
     * importé son historique peut déjà porter un matricule de la même forme ; on saute alors
     * les valeurs occupées plutôt que d'échouer.
     */
    private static final int ESSAIS_MAX = 50;

    @PersistenceContext
    private EntityManager entityManager;

    private final StudentRepository studentRepository;

    public StudentNumberGenerator(StudentRepository studentRepository) {
        this.studentRepository = studentRepository;
    }

    /**
     * Réserve le prochain matricule libre de l'établissement courant.
     *
     * <p>Volontairement dans la transaction de l'appelant, et surtout PAS en
     * {@code REQUIRES_NEW}. {@code TenantSessionConfigurer} pose {@code app.tenant_id} avec
     * {@code set_config(..., false)}, donc au niveau de la SESSION : la variable reste
     * attachée à la connexion du pool. Une transaction séparée emprunte une autre connexion,
     * porteuse du tenant d'une requête précédente — le compteur incrémenté était alors celui
     * d'un autre établissement, et l'élève recevait un matricule pris ailleurs. Constaté en
     * test : la première élève de la deuxième école recevait 2026-0002.
     *
     * <p>Conséquence assumée : si l'inscription échoue après la réservation, le numéro est
     * rendu avec la transaction. Mieux vaut cela qu'un trou — et infiniment mieux qu'un
     * compteur partagé entre écoles.
     */
    @Transactional
    public String next() {
        Long schoolId = TenantContext.get();
        if (schoolId == null) {
            throw new IllegalStateException("Aucun établissement dans le contexte : matricule impossible à attribuer");
        }
        int year = Year.now().getValue();

        for (int essai = 0; essai < ESSAIS_MAX; essai++) {
            int rang = ((Number) entityManager
                    .createNativeQuery("""
                            INSERT INTO student_number_counters (school_id, year, last_value)
                            VALUES (:school, :year, 1)
                            ON CONFLICT (school_id, year)
                            DO UPDATE SET last_value = student_number_counters.last_value + 1
                            RETURNING last_value
                            """)
                    .setParameter("school", schoolId)
                    .setParameter("year", year)
                    .getSingleResult()).intValue();

            String matricule = FORMAT.formatted(year, rang);
            if (!studentRepository.existsByStudentNumber(matricule)) {
                return matricule;
            }
        }
        throw new IllegalStateException("Aucun matricule libre trouvé après " + ESSAIS_MAX + " essais");
    }
}
