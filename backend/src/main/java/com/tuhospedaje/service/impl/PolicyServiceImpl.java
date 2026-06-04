package com.tuhospedaje.service.impl;

import com.tuhospedaje.dto.policy.PolicyDTO;
import com.tuhospedaje.entity.Policy;
import com.tuhospedaje.exception.ResourceNotFoundException;
import com.tuhospedaje.repository.PolicyRepository;
import com.tuhospedaje.service.PolicyService;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
public class PolicyServiceImpl implements PolicyService {

    private final PolicyRepository policyRepository;

    public PolicyServiceImpl(PolicyRepository policyRepository) {
        this.policyRepository = policyRepository;
    }

    @Override
    public PolicyDTO save(PolicyDTO dto) {
        if (policyRepository.existsByName(dto.getName())) {
            throw new IllegalArgumentException("Ya existe una política con el nombre: " + dto.getName());
        }
        Policy policy = dto.toEntity();
        Policy saved = policyRepository.save(policy);
        return PolicyDTO.fromEntity(saved);
    }

    @Override
    public PolicyDTO update(PolicyDTO dto) throws ResourceNotFoundException {
        Policy policy = policyRepository.findById(dto.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Política no encontrada con ID: " + dto.getId()));

        if (!policy.getName().equals(dto.getName()) && policyRepository.existsByName(dto.getName())) {
            throw new IllegalArgumentException("Ya existe una política con el nombre: " + dto.getName());
        }

        policy.setName(dto.getName());
        policy.setDescription(dto.getDescription());
        policy.setIcon(dto.getIcon());

        Policy updated = policyRepository.save(policy);
        return PolicyDTO.fromEntity(updated);
    }

    @Override
    public Optional<PolicyDTO> delete(Long id) throws ResourceNotFoundException {
        Policy policy = policyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Política no encontrada con ID: " + id));
        policyRepository.deleteById(id);
        return Optional.of(PolicyDTO.fromEntity(policy));
    }

    @Override
    public List<PolicyDTO> findAll() {
        return policyRepository.findAll()
                .stream()
                .sorted(Comparator.comparing(Policy::getName, String.CASE_INSENSITIVE_ORDER))
                .map(PolicyDTO::fromEntity)
                .toList();
    }

    @Override
    public Optional<PolicyDTO> findById(Long id) {
        return policyRepository.findById(id).map(PolicyDTO::fromEntity);
    }
}
