package com.tuhospedaje.policy;

import com.tuhospedaje.dto.policy.PolicyDTO;
import com.tuhospedaje.entity.Policy;
import com.tuhospedaje.exception.ResourceNotFoundException;
import com.tuhospedaje.repository.PolicyRepository;
import com.tuhospedaje.service.impl.PolicyServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PolicyServiceImplTest {

    @Mock
    private PolicyRepository policyRepository;

    @InjectMocks
    private PolicyServiceImpl policyService;

    @Test
    void shouldCreatePolicySuccessfully() {
        PolicyDTO dto = new PolicyDTO();
        dto.setName("Check-in");
        dto.setDescription("A partir de las 14:00");
        dto.setIcon("fa-solid fa-clock");

        Policy savedEntity = new Policy();
        savedEntity.setId(1L);
        savedEntity.setName("Check-in");
        savedEntity.setDescription("A partir de las 14:00");
        savedEntity.setIcon("fa-solid fa-clock");

        when(policyRepository.existsByName("Check-in")).thenReturn(false);
        when(policyRepository.save(any(Policy.class))).thenReturn(savedEntity);

        PolicyDTO response = policyService.save(dto);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getName()).isEqualTo("Check-in");
        assertThat(response.getDescription()).isEqualTo("A partir de las 14:00");
        assertThat(response.getIcon()).isEqualTo("fa-solid fa-clock");
    }

    @Test
    void shouldThrowWhenCreatePolicyNameAlreadyExists() {
        PolicyDTO dto = new PolicyDTO();
        dto.setName("Check-in");
        dto.setDescription("A partir de las 14:00");
        dto.setIcon("fa-solid fa-clock");

        when(policyRepository.existsByName("Check-in")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> policyService.save(dto));
    }

    @Test
    void shouldReturnAllPoliciesSortedByName() {
        Policy policyOne = new Policy();
        policyOne.setId(1L);
        policyOne.setName("Mascotas");
        policyOne.setDescription("Mascotas pequeñas permitidas");
        policyOne.setIcon("fa-solid fa-dog");

        Policy policyTwo = new Policy();
        policyTwo.setId(2L);
        policyTwo.setName("Cancelación");
        policyTwo.setDescription("Cancelación gratis");
        policyTwo.setIcon("fa-solid fa-ban");

        when(policyRepository.findAll()).thenReturn(List.of(policyOne, policyTwo));

        List<PolicyDTO> response = policyService.findAll();

        assertThat(response).hasSize(2);
        assertThat(response.get(0).getName()).isEqualTo("Cancelación");
        assertThat(response.get(1).getName()).isEqualTo("Mascotas");
    }

    @Test
    void shouldReturnPolicyById() {
        Policy policy = new Policy();
        policy.setId(9L);
        policy.setName("Fiestas");
        policy.setDescription("No se permiten fiestas");
        policy.setIcon("fa-solid fa-gift");

        when(policyRepository.findById(9L)).thenReturn(Optional.of(policy));

        Optional<PolicyDTO> response = policyService.findById(9L);

        assertThat(response).isPresent();
        assertThat(response.get().getId()).isEqualTo(9L);
        assertThat(response.get().getName()).isEqualTo("Fiestas");
    }

    @Test
    void shouldReturnEmptyWhenPolicyByIdDoesNotExist() {
        when(policyRepository.findById(999L)).thenReturn(Optional.empty());

        Optional<PolicyDTO> response = policyService.findById(999L);

        assertThat(response).isEmpty();
    }

    @Test
    void shouldUpdatePolicySuccessfully() {
        Policy existing = new Policy();
        existing.setId(1L);
        existing.setName("Check-in");
        existing.setDescription("A partir de las 14:00");
        existing.setIcon("fa-solid fa-clock");

        PolicyDTO input = new PolicyDTO();
        input.setId(1L);
        input.setName("Check-in actualizado");
        input.setDescription("A partir de las 13:00");
        input.setIcon("fa-solid fa-clock");

        when(policyRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(policyRepository.existsByName("Check-in actualizado")).thenReturn(false);
        when(policyRepository.save(any(Policy.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PolicyDTO response = policyService.update(input);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getName()).isEqualTo("Check-in actualizado");
        assertThat(response.getDescription()).isEqualTo("A partir de las 13:00");
    }

    @Test
    void shouldAllowUpdateWhenKeepingOwnName() {
        Policy existing = new Policy();
        existing.setId(1L);
        existing.setName("Check-in");
        existing.setDescription("A partir de las 14:00");
        existing.setIcon("fa-solid fa-clock");

        PolicyDTO input = new PolicyDTO();
        input.setId(1L);
        input.setName("Check-in");
        input.setDescription("Descripción actualizada");
        input.setIcon("fa-solid fa-clock");

        when(policyRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(policyRepository.save(any(Policy.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PolicyDTO response = policyService.update(input);

        assertThat(response.getName()).isEqualTo("Check-in");
        assertThat(response.getDescription()).isEqualTo("Descripción actualizada");
    }

    @Test
    void shouldThrowWhenUpdatePolicyDoesNotExist() {
        PolicyDTO input = new PolicyDTO();
        input.setId(777L);
        input.setName("No existe");
        input.setDescription("none");
        input.setIcon("none");

        when(policyRepository.findById(777L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> policyService.update(input));
    }

    @Test
    void shouldThrowWhenUpdatePolicyNameAlreadyExists() {
        Policy existing = new Policy();
        existing.setId(1L);
        existing.setName("Check-in");
        existing.setDescription("A partir de las 14:00");
        existing.setIcon("fa-solid fa-clock");

        PolicyDTO input = new PolicyDTO();
        input.setId(1L);
        input.setName("Check-out");
        input.setDescription("Hasta las 11:00");
        input.setIcon("fa-solid fa-clock");

        when(policyRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(policyRepository.existsByName("Check-out")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> policyService.update(input));
    }

    @Test
    void shouldDeletePolicySuccessfully() {
        Policy policy = new Policy();
        policy.setId(8L);
        policy.setName("Temporal");
        policy.setDescription("Temporal");
        policy.setIcon("temp");

        when(policyRepository.findById(8L)).thenReturn(Optional.of(policy));

        Optional<PolicyDTO> deleted = policyService.delete(8L);

        assertThat(deleted).isPresent();
        assertThat(deleted.get().getId()).isEqualTo(8L);
    }

    @Test
    void shouldThrowWhenDeletePolicyDoesNotExist() {
        when(policyRepository.findById(404L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> policyService.delete(404L));
    }
}
