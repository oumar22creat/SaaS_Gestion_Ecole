package com.schoolsaas.parent;

import com.schoolsaas.common.ApiException;
import com.schoolsaas.parent.dto.ParentRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ParentService {

    private final ParentRepository parentRepository;

    public ParentService(ParentRepository parentRepository) {
        this.parentRepository = parentRepository;
    }

    @Transactional
    public Parent create(ParentRequest request) {
        return parentRepository.save(new Parent(request.firstName(), request.lastName(), request.email(), request.phone()));
    }

    public Parent getById(Long id) {
        return parentRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("PARENT_NOT_FOUND", "Parent introuvable"));
    }

    /**
     * Liste paginée, filtrée par un terme de recherche libre quand il est fourni. Un terme
     * vide est traité comme absent : « rechercher rien » doit tout renvoyer, pas rien.
     */
    public Page<Parent> list(Pageable pageable, String search) {
        if (search == null || search.isBlank()) {
            return list(pageable);
        }
        return parentRepository.search(search.trim(), pageable);
    }

    public Page<Parent> list(Pageable pageable) {
        return parentRepository.findAll(pageable);
    }

    @Transactional
    public Parent update(Long id, ParentRequest request) {
        Parent parent = getById(id);
        parent.setFirstName(request.firstName());
        parent.setLastName(request.lastName());
        parent.setEmail(request.email());
        parent.setPhone(request.phone());
        return parent;
    }

    @Transactional
    public void delete(Long id) {
        parentRepository.delete(getById(id));
    }
}
