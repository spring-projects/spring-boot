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

package org.springframework.boot.jpa.test.autoconfigure;

import jakarta.persistence.EntityManagerFactory;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuration for {@link TestEntityManager}.
 *
 * @author Phillip Webb
 * @since 4.0.0
 * @see AutoConfigureTestEntityManager
 */
@AutoConfiguration(afterName = "org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration")
@ConditionalOnClass({ EntityManagerFactory.class })
public final class TestEntityManagerAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	TestEntityManager testEntityManager(EntityManagerFactory entityManagerFactory) {
		return new TestEntityManager(entityManagerFactory);
	}

}
