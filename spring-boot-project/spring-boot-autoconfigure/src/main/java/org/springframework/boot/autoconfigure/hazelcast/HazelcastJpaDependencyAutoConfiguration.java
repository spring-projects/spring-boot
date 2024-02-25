/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot.autoconfigure.hazelcast;

import com.hazelcast.core.HazelcastInstance;
import jakarta.persistence.EntityManagerFactory;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.AllNestedConditions;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.hazelcast.HazelcastJpaDependencyAutoConfiguration.HazelcastInstanceEntityManagerFactoryDependsOnPostProcessor;
import org.springframework.boot.autoconfigure.orm.jpa.EntityManagerFactoryDependsOnPostProcessor;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Import;
import org.springframework.orm.jpa.AbstractEntityManagerFactoryBean;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;

/**
 * Additional configuration to ensure that {@link EntityManagerFactory} beans depend on
 * the {@code hazelcastInstance} bean.
 *
 * @author Stephane Nicoll
 * @since 1.3.2
 */
@AutoConfiguration(after = { HazelcastAutoConfiguration.class, HibernateJpaAutoConfiguration.class })
@ConditionalOnClass({ HazelcastInstance.class, LocalContainerEntityManagerFactoryBean.class })
@Import(HazelcastInstanceEntityManagerFactoryDependsOnPostProcessor.class)
public class HazelcastJpaDependencyAutoConfiguration {

	/**
	 * HazelcastInstanceEntityManagerFactoryDependsOnPostProcessor class.
	 */
	@Conditional(OnHazelcastAndJpaCondition.class)
	static class HazelcastInstanceEntityManagerFactoryDependsOnPostProcessor
			extends EntityManagerFactoryDependsOnPostProcessor {

		/**
		 * Constructs a new HazelcastInstanceEntityManagerFactoryDependsOnPostProcessor
		 * with the specified Hazelcast instance dependency.
		 * @param hazelcastInstance the Hazelcast instance to depend on
		 */
		HazelcastInstanceEntityManagerFactoryDependsOnPostProcessor() {
			super("hazelcastInstance");
		}

	}

	/**
	 * OnHazelcastAndJpaCondition class.
	 */
	static class OnHazelcastAndJpaCondition extends AllNestedConditions {

		/**
		 * Constructor for the OnHazelcastAndJpaCondition class.
		 *
		 * Initializes a new instance of the class with the specified configuration phase.
		 * @param configurationPhase The configuration phase for the condition.
		 */
		OnHazelcastAndJpaCondition() {
			super(ConfigurationPhase.REGISTER_BEAN);
		}

		/**
		 * HasHazelcastInstance class.
		 */
		@ConditionalOnBean(name = "hazelcastInstance")
		static class HasHazelcastInstance {

		}

		/**
		 * HasJpa class.
		 */
		@ConditionalOnBean(AbstractEntityManagerFactoryBean.class)
		static class HasJpa {

		}

	}

}
