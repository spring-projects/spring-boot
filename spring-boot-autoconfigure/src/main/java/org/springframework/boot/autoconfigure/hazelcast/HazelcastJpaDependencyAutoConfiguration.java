/*
 * Copyright 2012-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.autoconfigure.hazelcast;

import javax.persistence.EntityManagerFactory;

import com.hazelcast.core.HazelcastInstance;

import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.AllNestedConditions;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.data.jpa.EntityManagerFactoryDependsOnPostProcessor;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.jpa.AbstractEntityManagerFactoryBean;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;

/**
 * Additional configuration to ensure that {@link EntityManagerFactory} beans depend-on
 * the {@code hazelcastInstance} bean.
 *
 * @author Stephane Nicoll
 */
@Configuration
@ConditionalOnClass({ HazelcastInstance.class,
		LocalContainerEntityManagerFactoryBean.class })
@AutoConfigureAfter({ HazelcastAutoConfiguration.class,
		HibernateJpaAutoConfiguration.class })
class HazelcastJpaDependencyAutoConfiguration {

	@Bean
	@Conditional(OnHazelcastAndJpaCondition.class)
	public static HazelcastInstanceJpaDependencyPostProcessor hazelcastInstanceJpaDependencyPostProcessor() {
		return new HazelcastInstanceJpaDependencyPostProcessor();
	}

	private static class HazelcastInstanceJpaDependencyPostProcessor
			extends EntityManagerFactoryDependsOnPostProcessor {

		HazelcastInstanceJpaDependencyPostProcessor() {
			super("hazelcastInstance");
		}

	}

	static class OnHazelcastAndJpaCondition extends AllNestedConditions {

		OnHazelcastAndJpaCondition() {
			super(ConfigurationPhase.REGISTER_BEAN);
		}

		@ConditionalOnBean(name = "hazelcastInstance")
		static class HasHazelcastInstance {

		}

		@ConditionalOnBean(AbstractEntityManagerFactoryBean.class)
		static class HasJpa {

		}

	}

}
