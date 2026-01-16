/*
 * Copyright 2012-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.jpa.validation;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Table;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link UniqueEntityValidator}.
 *
 * @author Mohammed Bensassi
 */
@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@Transactional
class UniqueEntityValidatorTests {

    @Autowired
    private Validator validator;

    @PersistenceContext
    private EntityManager entityManager;

    @BeforeEach
    void setup() {
        // Create existing user in database
        TestUserEntity existingUser = new TestUserEntity();
        existingUser.setEmail("existing@example.com");
        existingUser.setUsername("existinguser");
        entityManager.persist(existingUser);
        entityManager.flush();
    }

    @Test
    void shouldPassWhenFieldIsUnique() {
        TestUserDTO dto = new TestUserDTO();
        dto.setEmail("newuser@example.com");
        dto.setUsername("newuser");

        Set<ConstraintViolation<TestUserDTO>> violations = validator.validate(dto);

        assertThat(violations).isEmpty();
    }

    @Test
    void shouldFailWhenEmailIsNotUnique() {
        TestUserDTO dto = new TestUserDTO();
        dto.setEmail("existing@example.com");  // Duplicate
        dto.setUsername("newuser");

        Set<ConstraintViolation<TestUserDTO>> violations = validator.validate(dto);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("Email already exists");
    }

    @Test
    void shouldAllowUpdateOfSameEntity() {
        // Get existing entity ID
        TestUserEntity existing = entityManager
                .createQuery("SELECT u FROM TestUserEntity u WHERE u.email = :email", TestUserEntity.class)
                .setParameter("email", "existing@example.com")
                .getSingleResult();

        TestUserDTO dto = new TestUserDTO();
        dto.setId(existing.getId());  // Same ID
        dto.setEmail("existing@example.com");  // Same email
        dto.setUsername("updatedname");

        Set<ConstraintViolation<TestUserDTO>> violations = validator.validate(dto);

        assertThat(violations).isEmpty();
    }

    @Test
    void shouldSkipValidationWhenFieldIsNull() {
        TestUserDTO dto = new TestUserDTO();
        dto.setEmail(null);  // Null value
        dto.setUsername("newuser");

        Set<ConstraintViolation<TestUserDTO>> violations = validator.validate(dto);

        assertThat(violations).isEmpty();
    }

    @Test
    void shouldPassWhenDtoIsNull() {
        Set<ConstraintViolation<TestUserDTO>> violations = validator.validate((TestUserDTO) null);

        // Validator should return true for null object (handled by other constraints)
        assertThat(violations).isEmpty();
    }

    // ========== Test Entities and DTOs ==========

    @UniqueEntity(
            entityClass = TestUserEntity.class,
            constraints = {
                    @UniqueConstraint(fields = "email", message = "Email already exists"),
                    @UniqueConstraint(fields = "username", message = "Username already taken")
            }
    )
    static class TestUserDTO {

        private Long id;

        private String email;

        private String username;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

    }

    @Entity
    @Table(name = "test_users")
    static class TestUserEntity {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        private String email;

        private String username;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

    }

    @Configuration
    @EnableAutoConfiguration
    static class TestConfiguration {

    }

}
