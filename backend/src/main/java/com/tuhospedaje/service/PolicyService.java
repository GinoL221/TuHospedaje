package com.tuhospedaje.service;

import com.tuhospedaje.dto.policy.PolicyDTO;
import com.tuhospedaje.exception.ResourceNotFoundException;

import java.util.List;
import java.util.Optional;

public interface PolicyService {
    PolicyDTO save(PolicyDTO dto);

    PolicyDTO update(PolicyDTO dto) throws ResourceNotFoundException;

    Optional<PolicyDTO> delete(Long id) throws ResourceNotFoundException;

    List<PolicyDTO> findAll();

    Optional<PolicyDTO> findById(Long id);
}
