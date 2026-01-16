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

package org.springframework.boot.jpa.validation.autoconfigure;

import jakarta.persistence.EntityManager;
import jakarta.validation.Validator;

import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.validation.beanvalidation.SpringConstraintValidatorFactory;

/**
 * {@link AutoConfiguration Auto-configuration} for JPA-based validation
 * constraints.
 * This configuration enables Spring dependency injection in constraint
 * validators,
 * allowing them to use {@code @PersistenceContext} and other Spring-managed
 * beans.
 *
 * <p>
 * This auto-configuration is activated when both JPA (EntityManager) and
 * Jakarta Validation (Validator) are present on the classpath.
 *
 * @author Mohammed Bensassi
 * @since 4.1.0
 * @see org.springframework.boot.jpa.validation.UniqueEntity
 * @see org.springframework.boot.jpa.validation.UniqueEntityValidator
 */
@AutoConfiguration
@ConditionalOnClass({ EntityManager.class, Validator.class })
@ConditionalOnBean(EntityManager.class)
public class UniqueEntityAutoConfiguration {

    /**
     * Creates a Validator with Spring-managed constraint validators.
     * This allows validators to use {@code @Autowired},
     * {@code @PersistenceContext},
     * and other Spring injection annotations.
     * 
     * @param beanFactory the autowire-capable bean factory
     * @return the configured validator
     */
    @Bean
    @ConditionalOnMissingBean
    public Validator jpaValidator(AutowireCapableBeanFactory beanFactory) {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.setConstraintValidatorFactory(new SpringConstraintValidatorFactory(beanFactory));
        return validator;
    }

}
