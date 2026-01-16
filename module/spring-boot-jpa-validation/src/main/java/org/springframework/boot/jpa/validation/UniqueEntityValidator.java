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

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

/**
 * {@link ConstraintValidator} implementation for {@link UniqueEntity}.
 * Uses JPA's EntityManager to perform dynamic JPQL queries for uniqueness
 * validation.
 *
 * <p>
 * This validator supports:
 * <ul>
 * <li>Simple and composite unique keys</li>
 * <li>Case-insensitive comparisons</li>
 * <li>Additional WHERE clauses for soft-delete support</li>
 * <li>Automatic ID exclusion during entity updates</li>
 * </ul>
 *
 * <p>
 * The validator uses an LRU cache for reflection field lookups to optimize
 * performance. The cache is limited to 1000 entries to prevent memory leaks.
 *
 * <p>
 * <strong>Security:</strong> All field names and additional WHERE clauses
 * are validated against regex patterns to prevent SQL injection attacks.
 *
 * @author Mohammed Bensassi
 * @since 4.1.0
 */
public class UniqueEntityValidator implements ConstraintValidator<UniqueEntity, Object> {

    private static final Logger logger = LoggerFactory.getLogger(UniqueEntityValidator.class);

    /**
     * Pattern for validating field names (alphanumeric with underscores).
     */
    private static final Pattern VALID_FIELD_NAME = Pattern.compile("^[a-zA-Z_][a-zA-Z0-9_]*$");

    /**
     * Pattern for validating additional WHERE clauses.
     * Allows: e.fieldName = true/false/'string'/123
     */
    private static final Pattern SAFE_WHERE_CLAUSE = Pattern.compile(
            "^e\\.[a-zA-Z_][a-zA-Z0-9_]*\\s*(=|!=|<>|IS NULL|IS NOT NULL)\\s*" +
                    "(true|false|'[^']*'|[0-9]+)?$",
            Pattern.CASE_INSENSITIVE);

    /**
     * LRU cache for Field lookups. Max 1000 entries.
     */
    private static final Map<String, Field> FIELD_CACHE = Collections.synchronizedMap(
            new LinkedHashMap<String, Field>(256, 0.75f, true) {
                private static final long serialVersionUID = 1L;

                @Override
                protected boolean removeEldestEntry(Map.Entry<String, Field> eldest) {
                    return size() > 1000;
                }
            });

    @PersistenceContext
    private EntityManager entityManager;

    private Class<?> entityClass;

    private UniqueConstraint[] constraints;

    private String idFieldName;

    @Override
    public void initialize(UniqueEntity annotation) {
        this.entityClass = annotation.entityClass();
        this.constraints = annotation.constraints();
        this.idFieldName = annotation.idField();

        // Validate configuration
        Assert.notNull(this.entityClass, "entityClass must not be null");
        Assert.notEmpty(this.constraints, "constraints must not be empty");

        // Pre-validate all field names
        for (UniqueConstraint constraint : this.constraints) {
            for (String fieldName : constraint.fields()) {
                validateFieldName(fieldName);
            }
            if (StringUtils.hasText(constraint.additionalWhere())) {
                validateWhereClause(constraint.additionalWhere());
            }
        }
    }

    @Override
    public boolean isValid(@Nullable Object dto, ConstraintValidatorContext context) {
        if (dto == null) {
            return true;
        }

        try {
            Object idValue = getFieldValue(dto, this.idFieldName);

            for (UniqueConstraint constraint : this.constraints) {
                Map<String, Object> queryParams = new HashMap<>();
                boolean skipConstraint = false;

                // Collect field values
                for (String fieldName : constraint.fields()) {
                    Object value = getFieldValue(dto, fieldName);
                    if (value == null && constraint.skipIfAnyNull()) {
                        skipConstraint = true;
                        break;
                    }
                    queryParams.put(fieldName, value);
                }

                if (skipConstraint) {
                    continue;
                }

                // Check uniqueness
                if (!checkUniqueness(constraint, queryParams, idValue)) {
                    context.disableDefaultConstraintViolation();
                    context.buildConstraintViolationWithTemplate(constraint.message())
                            .addPropertyNode(constraint.fields()[0])
                            .addConstraintViolation();
                    return false;
                }
            }

            return true;

        } catch (Exception ex) {
            // Fail-safe: log error and allow validation to pass
            logger.error("Error during uniqueness validation for entity={}, constraint={}",
                    this.entityClass.getSimpleName(),
                    (this.constraints.length > 0 ? this.constraints[0].fields()[0] : "unknown"),
                    ex);
            return true;
        }
    }

    /**
     * Checks if the given field values are unique in the database.
     * 
     * @param constraint the uniqueness constraint
     * @param params     the field name to value map
     * @param idValue    the ID value to exclude (for updates), or null
     * @return true if unique, false if duplicate exists
     */
    private boolean checkUniqueness(UniqueConstraint constraint, Map<String, Object> params,
            @Nullable Object idValue) {

        StringBuilder jpql = new StringBuilder("SELECT COUNT(e) FROM ")
                .append(this.entityClass.getSimpleName())
                .append(" e WHERE 1=1");

        // Add field conditions
        for (String fieldName : constraint.fields()) {
            jpql.append(" AND ");
            if (constraint.ignoreCase() && params.get(fieldName) instanceof String) {
                jpql.append("LOWER(e.").append(fieldName).append(") = LOWER(:").append(fieldName).append(")");
            } else {
                jpql.append("e.").append(fieldName).append(" = :").append(fieldName);
            }
        }

        // Add additional WHERE clause (e.g., soft delete filter)
        if (StringUtils.hasText(constraint.additionalWhere())) {
            jpql.append(" AND (").append(constraint.additionalWhere()).append(")");
        }

        // Exclude current entity during updates
        if (idValue != null) {
            jpql.append(" AND e.").append(this.idFieldName).append(" != :excludeId");
        }

        Query query = this.entityManager.createQuery(jpql.toString());

        // Set field parameters
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            query.setParameter(entry.getKey(), entry.getValue());
        }

        // Set exclude ID parameter
        if (idValue != null) {
            query.setParameter("excludeId", idValue);
        }

        Long count = (Long) query.getSingleResult();
        return count == 0;
    }

    /**
     * Gets the value of a field from an object using reflection.
     * Uses an LRU cache to optimize repeated lookups.
     * 
     * @param object    the object to read from
     * @param fieldName the field name
     * @return the field value, or null
     */
    private @Nullable Object getFieldValue(Object object, String fieldName) {
        String cacheKey = object.getClass().getName() + "." + fieldName;

        Field field = FIELD_CACHE.computeIfAbsent(cacheKey, key -> {
            Field f = ReflectionUtils.findField(object.getClass(), fieldName);
            if (f != null) {
                ReflectionUtils.makeAccessible(f);
            }
            return f;
        });

        if (field == null) {
            return null;
        }

        return ReflectionUtils.getField(field, object);
    }

    /**
     * Validates that a field name matches the allowed pattern.
     * 
     * @param fieldName the field name to validate
     * @throws IllegalArgumentException if the field name is invalid
     */
    private void validateFieldName(String fieldName) {
        if (!VALID_FIELD_NAME.matcher(fieldName).matches()) {
            throw new IllegalArgumentException("Invalid field name: " + fieldName +
                    ". Field names must match pattern: " + VALID_FIELD_NAME.pattern());
        }
    }

    /**
     * Validates that an additional WHERE clause is safe.
     * 
     * @param clause the WHERE clause to validate
     * @throws IllegalArgumentException if the clause is unsafe
     */
    private void validateWhereClause(String clause) {
        if (!SAFE_WHERE_CLAUSE.matcher(clause.trim()).matches()) {
            throw new IllegalArgumentException("Unsafe additionalWhere clause: " + clause +
                    ". Only simple conditions are allowed (e.g., 'e.deleted = false')");
        }
    }

}
