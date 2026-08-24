package com.sayarti.backend.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.sayarti.backend.AbstractIntegrationTest;
import com.sayarti.backend.user.entity.User;
import com.sayarti.backend.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class JpaAuditingIntegrationTest extends AbstractIntegrationTest {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void auditTimestampsAreCreatedAndUpdatedAutomaticallyWithUtcSqlServerMapping()
            throws InterruptedException {
        User user = new User("Audit", "Test", "audit@example.com", "password-hash");

        assertThat(user.getCreatedAt()).isNull();
        assertThat(user.getUpdatedAt()).isNull();

        // The entities use application-assigned UUIDs, so Spring Data saves new instances via
        // EntityManager.merge(). Auditing is applied to the managed instance returned by save.
        user = userRepository.saveAndFlush(user);
        var createdAt = user.getCreatedAt();
        var firstUpdatedAt = user.getUpdatedAt();

        assertThat(createdAt).isNotNull();
        assertThat(firstUpdatedAt).isNotNull();
        assertThat(firstUpdatedAt).isEqualTo(createdAt);

        OffsetDateTime databaseCreatedAt = (OffsetDateTime) entityManager.createNativeQuery(
                        "SELECT created_at FROM users WHERE id = :id", OffsetDateTime.class)
                .setParameter("id", user.getId())
                .getSingleResult();
        assertThat(databaseCreatedAt.getOffset()).isEqualTo(ZoneOffset.UTC);
        assertThat(databaseCreatedAt.toInstant()).isEqualTo(createdAt);

        Thread.sleep(10);
        user.updateProfile("Audited", null);
        userRepository.saveAndFlush(user);

        assertThat(user.getCreatedAt()).isEqualTo(createdAt);
        assertThat(user.getUpdatedAt()).isAfter(firstUpdatedAt);
    }
}
