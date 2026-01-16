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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/**
 * Validates that field values are unique within the database for the target
 * entity.
 * This annotation should be placed on DTOs or entity classes to validate
 * uniqueness
 * before persistence.
 *
 * <p>
 * The annotation uses JPA's EntityManager to perform dynamic queries and
 * supports:
 * <ul>
 * <li>Simple unique fields (email, username, etc.)</li>
 * <li>Composite unique keys (organizationId + employeeCode)</li>
 * <li>Case-insensitive comparisons</li>
 * <li>Soft delete filtering via additional WHERE clauses</li>
 * <li>Automatic ID exclusion during updates</li>
 * </ul>
 *
 * <p>
 * Example usage on a DTO:
 * 
 * <pre class="code">
 * &#64;UniqueEntity(entityClass = User.class, constraints = {
 *         &#64;UniqueConstraint(fields = "email", message = "Email already exists"),
 *         &#64;UniqueConstraint(fields = "username", message = "Username taken")
 * })
 * public class UserDTO {
 *     private Long id;
 *     private String email;
 *     private String username;
 * }
 * </pre>
 *
 * <p>
 * <strong>Warning:</strong> This validation provides a UX layer only.
 * You must still have UNIQUE constraints at the database level to handle
 * race conditions.
 *
 * @author Mohammed Bensassi
 * @since 4.1.0
 * @see UniqueConstraint
 * @see UniqueEntityValidator
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Repeatable(UniqueEntities.class)
@Constraint(validatedBy = UniqueEntityValidator.class)
public @interface UniqueEntity {

    /**
     * The JPA entity class to query for uniqueness checks.
     * 
     * @return the entity class
     */
    Class<?> entityClass();

    /**
     * The list of uniqueness constraints to validate.
     * 
     * @return the constraints
     */
    UniqueConstraint[] constraints();

    /**
     * The name of the ID field in the DTO/Entity.
     * Used to exclude the current entity during updates.
     * 
     * @return the ID field name
     */
    String idField() default "id";

    /**
     * Default error message (typically overridden by constraint-level messages).
     * 
     * @return the default message
     */
    String message() default "{org.springframework.boot.jpa.validation.UniqueEntity.message}";

    /**
     * Validation groups.
     * 
     * @return the groups
     */
    Class<?>[] groups() default {};

    /**
     * Payload for extensibility purposes.
     * 
     * @return the payload
     */
    Class<? extends Payload>[] payload() default {};

}
