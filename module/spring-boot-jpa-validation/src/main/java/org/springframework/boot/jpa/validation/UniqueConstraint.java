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

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Defines a uniqueness constraint for one or more fields in an entity.
 * Used within {@link UniqueEntity} to specify individual uniqueness rules.
 *
 * <p>
 * Example usage for a single field:
 * 
 * <pre class="code">
 * &#64;UniqueConstraint(
 *     fields = "email",
 *     message = "This email is already in use"
 * )
 * </pre>
 *
 * <p>
 * Example usage for composite key:
 * 
 * <pre class="code">
 * &#64;UniqueConstraint(
 *     fields = {"organizationId", "employeeCode"},
 *     message = "This employee code already exists in this organization"
 * )
 * </pre>
 *
 * @author Mohammed Bensassi
 * @since 4.1.0
 * @see UniqueEntity
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({})
public @interface UniqueConstraint {

    /**
     * The field name(s) that form the unique constraint.
     * For composite keys, specify multiple field names.
     * 
     * @return the field names
     */
    String[] fields();

    /**
     * The error message to display when validation fails.
     * Supports i18n via property keys (e.g., "{validation.unique.email}").
     * 
     * @return the error message
     */
    String message();

    /**
     * If true, comparison is case-insensitive (uses SQL LOWER function).
     * Applicable only for String fields.
     * 
     * @return whether to ignore case
     */
    boolean ignoreCase() default false;

    /**
     * Additional JPQL WHERE clause to filter records.
     * Useful for soft delete scenarios (e.g., "e.deleted = false").
     * <p>
     * <strong>Security Note:</strong> Only simple conditions are allowed.
     * Complex expressions are rejected to prevent SQL injection.
     * 
     * @return the additional WHERE clause
     */
    String additionalWhere() default "";

    /**
     * If true, skip validation when any of the field values is null.
     * 
     * @return whether to skip when null
     */
    boolean skipIfAnyNull() default true;

}
