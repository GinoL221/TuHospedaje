package com.tuhospedaje.service.impl;

import com.tuhospedaje.dto.policy.PolicyDTO;
import com.tuhospedaje.entity.Policy;
import com.tuhospedaje.exception.DuplicateResourceException;
import com.tuhospedaje.exception.ResourceNotFoundException;
import com.tuhospedaje.exception.UniqueConstraintViolationSupport;
import com.tuhospedaje.repository.PolicyRepository;
import com.tuhospedaje.service.PolicyService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
public class PolicyServiceImpl implements PolicyService {

    private static final String POLICY_NAME_CONSTRAINT = "UK_policies_name";

    private final PolicyRepository policyRepository;

    public PolicyServiceImpl(PolicyRepository policyRepository) {
        this.policyRepository = policyRepository;
    }

    @Override
    @Transactional
    public PolicyDTO save(PolicyDTO dto) {
        if (policyRepository.existsByName(dto.getName())) {
            throw new DuplicateResourceException("error.policy.duplicate_name", dto.getName());
        }
        Policy policy = dto.toEntity();
        return persistAndTranslateDuplicate(policy, dto.getName());
    }

    @Override
    @Transactional
    public PolicyDTO update(PolicyDTO dto) throws ResourceNotFoundException {
        Policy policy = policyRepository.findById(dto.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Política no encontrada con ID: " + dto.getId()));

        if (!policy.getName().equals(dto.getName()) && policyRepository.existsByName(dto.getName())) {
            throw new DuplicateResourceException("error.policy.duplicate_name", dto.getName());
        }

        policy.setName(dto.getName());
        policy.setDescription(dto.getDescription());
        policy.setIcon(dto.getIcon());

        return persistAndTranslateDuplicate(policy, dto.getName());
    }

    private PolicyDTO persistAndTranslateDuplicate(Policy policy, String candidateName) {
        try {
            Policy saved = policyRepository.saveAndFlush(policy);
            return PolicyDTO.fromEntity(saved);
        } catch (DataIntegrityViolationException ex) {
            if (UniqueConstraintViolationSupport.matchesConstraint(ex, POLICY_NAME_CONSTRAINT)) {
                throw new DuplicateResourceException("error.policy.duplicate_name", candidateName);
            }
            throw ex;
        }
    }

    @Override
    @Transactional
    public Optional<PolicyDTO> delete(Long id) throws ResourceNotFoundException {
        Policy policy = policyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Política no encontrada con ID: " + id));
        policyRepository.deleteById(id);
        return Optional.of(PolicyDTO.fromEntity(policy));
    }

    @Override
    @Transactional(readOnly = true)
    public List<PolicyDTO> findAll() {
        return policyRepository.findAll()
                .stream()
                .sorted(Comparator.comparing(Policy::getName, String.CASE_INSENSITIVE_ORDER))
                .map(PolicyDTO::fromEntity)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PolicyDTO> findById(Long id) {
        return policyRepository.findById(id).map(PolicyDTO::fromEntity);
    }
}
