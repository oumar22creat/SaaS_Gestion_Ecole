package com.schoolsaas.portal;

import com.schoolsaas.auth.Role;
import com.schoolsaas.auth.User;
import com.schoolsaas.auth.UserService;
import com.schoolsaas.auth.dto.UserResponse;
import com.schoolsaas.common.ApiException;
import com.schoolsaas.common.ApiResponse;
import com.schoolsaas.parent.Parent;
import com.schoolsaas.parent.ParentRepository;
import com.schoolsaas.student.Student;
import com.schoolsaas.student.StudentRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Ouverture d'un accès au portail pour une famille. L'action part toujours d'une fiche
 * existante (`students` ou `parents`), jamais d'un compte nu : c'est ce qui garantit qu'un
 * compte parent a des enfants à afficher et qu'un compte élève a un dossier.
 *
 * <p>Le nom n'est pas demandé : il est repris de la fiche, pour éviter qu'un même élève porte
 * deux orthographes selon l'écran où on le regarde.
 */
@RestController
@RequestMapping("/api/v1")
public class FamilyAccountController {

    /** Le mot de passe provisoire est communiqué hors ligne par l'établissement. */
    public record OpenAccessRequest(
            @NotBlank @Email @Size(max = 255) String email,
            @NotBlank @Size(min = 8, max = 100, message = "8 caractères minimum") String password) {
    }

    private final UserService userService;
    private final StudentRepository studentRepository;
    private final ParentRepository parentRepository;

    public FamilyAccountController(
            UserService userService,
            StudentRepository studentRepository,
            ParentRepository parentRepository) {
        this.userService = userService;
        this.studentRepository = studentRepository;
        this.parentRepository = parentRepository;
    }

    @PostMapping("/students/{studentId}/account")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'DIRECTION', 'SECRETARY')")
    @Transactional
    public ApiResponse<UserResponse> openStudentAccess(
            @PathVariable Long studentId, @Valid @RequestBody OpenAccessRequest request) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> ApiException.notFound("STUDENT_NOT_FOUND", "Élève introuvable"));
        if (student.getUserId() != null) {
            throw ApiException.conflict("ACCOUNT_ALREADY_OPEN", "Cet élève a déjà un accès");
        }
        User account = userService.createFamilyAccount(
                request.email(), request.password(), student.getFirstName(), student.getLastName(), Role.STUDENT);
        student.setUserId(account.getId());
        studentRepository.save(student);
        return ApiResponse.of(UserResponse.from(account));
    }

    @PostMapping("/parents/{parentId}/account")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'DIRECTION', 'SECRETARY')")
    @Transactional
    public ApiResponse<UserResponse> openParentAccess(
            @PathVariable Long parentId, @Valid @RequestBody OpenAccessRequest request) {
        Parent parent = parentRepository.findById(parentId)
                .orElseThrow(() -> ApiException.notFound("PARENT_NOT_FOUND", "Parent introuvable"));
        if (parent.getUserId() != null) {
            throw ApiException.conflict("ACCOUNT_ALREADY_OPEN", "Ce parent a déjà un accès");
        }
        User account = userService.createFamilyAccount(
                request.email(), request.password(), parent.getFirstName(), parent.getLastName(), Role.PARENT);
        parent.setUserId(account.getId());
        parentRepository.save(parent);
        return ApiResponse.of(UserResponse.from(account));
    }
}
