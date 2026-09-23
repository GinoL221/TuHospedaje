package com.tuhospedaje.user;

import com.tuhospedaje.configuration.TestcontainersConfiguration;
import com.tuhospedaje.entity.User;
import com.tuhospedaje.enums.RoleEnum;
import com.tuhospedaje.exception.LastEnabledAdminException;
import com.tuhospedaje.repository.UserRepository;
import com.tuhospedaje.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
class UserAdminInvariantConcurrencyIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    private final List<Long> testUserIds = new ArrayList<>();

    @AfterEach
    void deleteTestUsers() {
        testUserIds.forEach(userId -> userRepository.findById(userId).ifPresent(userRepository::delete));
        testUserIds.clear();
    }

    @Test
    void concurrentDemotionsLeaveOneEnabledAdminAndRejectOneTransaction() throws Exception {
        User firstAdmin = saveTestAdmin("first-concurrent-admin@test.com");
        User secondAdmin = saveTestAdmin("second-concurrent-admin@test.com");
        assertThat(enabledAdmins()).hasSize(2);

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<Void> firstDemotion = executor.submit(() -> demoteWhenReleased(firstAdmin.getId(), ready, start));
            Future<Void> secondDemotion = executor.submit(() -> demoteWhenReleased(secondAdmin.getId(), ready, start));

            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            List<Throwable> outcomes = Arrays.asList(outcomeOf(firstDemotion), outcomeOf(secondDemotion));
            assertThat(outcomes).filteredOn(outcome -> outcome == null).hasSize(1);
            assertThat(outcomes).filteredOn(LastEnabledAdminException.class::isInstance).hasSize(1);
            assertThat(enabledAdmins()).hasSize(1);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void concurrentDisablesLeaveOneEnabledAdminAndRejectOneTransaction() throws Exception {
        User firstAdmin = saveTestAdmin("first-concurrent-disable-admin@test.com");
        User secondAdmin = saveTestAdmin("second-concurrent-disable-admin@test.com");
        assertThat(enabledAdmins()).hasSize(2);

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<Void> firstDisable = executor.submit(() -> disableWhenReleased(firstAdmin.getId(), ready, start));
            Future<Void> secondDisable = executor.submit(() -> disableWhenReleased(secondAdmin.getId(), ready, start));

            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            List<Throwable> outcomes = Arrays.asList(outcomeOf(firstDisable), outcomeOf(secondDisable));
            assertThat(outcomes).filteredOn(outcome -> outcome == null).hasSize(1);
            assertThat(outcomes).filteredOn(LastEnabledAdminException.class::isInstance).hasSize(1);
            assertThat(enabledAdmins()).hasSize(1);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void concurrentDisablesOfTheSameAdminApplyOnceWithoutRemovingTheOtherAdmin() throws Exception {
        User targetAdmin = saveTestAdmin("same-target-disable-admin@test.com");
        User otherAdmin = saveTestAdmin("same-target-disable-other-admin@test.com");
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<Void> firstDisable = executor.submit(() -> disableWhenReleased(targetAdmin.getId(), ready, start));
            Future<Void> secondDisable = executor.submit(() -> disableWhenReleased(targetAdmin.getId(), ready, start));

            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            assertThat(Arrays.asList(outcomeOf(firstDisable), outcomeOf(secondDisable)))
                    .allMatch(outcome -> outcome == null);
            assertThat(userRepository.findById(targetAdmin.getId()).orElseThrow().isEnabled()).isFalse();
            assertThat(userRepository.findById(otherAdmin.getId()).orElseThrow().isEnabled()).isTrue();
            assertThat(enabledAdmins()).hasSize(1);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void concurrentDemotionAndDisableOfTheSameAdminPreserveBothCommittedFields() throws Exception {
        User targetAdmin = saveTestAdmin("same-target-mixed-admin@test.com");
        User otherAdmin = saveTestAdmin("same-target-mixed-other-admin@test.com");
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<Void> demotion = executor.submit(() -> demoteWhenReleased(targetAdmin.getId(), ready, start));
            Future<Void> disable = executor.submit(() -> disableWhenReleased(targetAdmin.getId(), ready, start));

            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            assertThat(Arrays.asList(outcomeOf(demotion), outcomeOf(disable)))
                    .allMatch(outcome -> outcome == null);
            User finalTarget = userRepository.findById(targetAdmin.getId()).orElseThrow();
            assertThat(finalTarget.getRole()).isEqualTo(RoleEnum.USER);
            assertThat(finalTarget.isEnabled()).isFalse();
            assertThat(userRepository.findById(otherAdmin.getId()).orElseThrow().isEnabled()).isTrue();
            assertThat(enabledAdmins()).hasSize(1);
        } finally {
            executor.shutdownNow();
        }
    }

    private Void demoteWhenReleased(Long userId, CountDownLatch ready, CountDownLatch start) throws InterruptedException {
        ready.countDown();
        if (!start.await(10, TimeUnit.SECONDS)) {
            throw new IllegalStateException("Timed out waiting to start concurrent demotions");
        }
        userService.updateRole(userId, "USER");
        return null;
    }

    private Void disableWhenReleased(Long userId, CountDownLatch ready, CountDownLatch start) throws InterruptedException {
        ready.countDown();
        if (!start.await(10, TimeUnit.SECONDS)) {
            throw new IllegalStateException("Timed out waiting to start concurrent disables");
        }
        userService.setEnabled(userId, false);
        return null;
    }

    private Throwable outcomeOf(Future<Void> future) throws InterruptedException {
        try {
            future.get(10, TimeUnit.SECONDS);
            return null;
        } catch (ExecutionException ex) {
            return ex.getCause();
        } catch (java.util.concurrent.TimeoutException ex) {
            return ex;
        }
    }

    private List<User> enabledAdmins() {
        return userRepository.findAll().stream()
                .filter(user -> user.getRole() == RoleEnum.ADMIN)
                .filter(User::isEnabled)
                .toList();
    }

    private User saveTestAdmin(String email) {
        User saved = userRepository.save(admin(email));
        testUserIds.add(saved.getId());
        return saved;
    }

    private static User admin(String email) {
        return User.builder()
                .firstName("Concurrent")
                .lastName("Admin")
                .email(email)
                .password("123456")
                .role(RoleEnum.ADMIN)
                .enabled(true)
                .build();
    }
}
