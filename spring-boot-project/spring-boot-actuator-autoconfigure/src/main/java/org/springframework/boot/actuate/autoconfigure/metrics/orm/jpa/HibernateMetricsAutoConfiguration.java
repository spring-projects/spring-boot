/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.metrics.orm.jpa;

import java.util.Collections;
import java.util.Map;

import javax.persistence.EntityManagerFactory;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.jpa.HibernateMetrics;
import org.hibernate.SessionFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.metrics.MetricsAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.export.simple.SimpleMetricsExportAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for metrics on all available
 * Hibernate {@link EntityManagerFactory} instances that have statistics enabled.
 *
 * @author Rui Figueira
 * @author Stephane Nicoll
 * @since 2.1.0
 */
@Configuration
@AutoConfigureAfter({ MetricsAutoConfiguration.class, HibernateJpaAutoConfiguration.class,
		SimpleMetricsExportAutoConfiguration.class })
@ConditionalOnClass({ EntityManagerFactory.class, SessionFactory.class,
		MeterRegistry.class })
@ConditionalOnBean({ EntityManagerFactory.class, MeterRegistry.class })
public class HibernateMetricsAutoConfiguration {

	private static final String ENTITY_MANAGER_FACTORY_SUFFIX = "entityManagerFactory";

	private final MeterRegistry registry;

	public HibernateMetricsAutoConfiguration(MeterRegistry registry) {
		this.registry = registry;
	}

	@Autowired
	public void bindEntityManagerFactoriesToRegistry(
			Map<String, EntityManagerFactory> entityManagerFactories) {
		entityManagerFactories.forEach(this::bindEntityManagerFactoryToRegistry);
	}

	private void bindEntityManagerFactoryToRegistry(String beanName,
			EntityManagerFactory entityManagerFactory) {
		String entityManagerFactoryName = getEntityManagerFactoryName(beanName);
		new HibernateMetrics(entityManagerFactory, entityManagerFactoryName,
				Collections.emptyList()).bindTo(this.registry);
	}

	/**
	 * Get the name of an {@link EntityManagerFactory} based on its {@code beanName}.
	 * @param beanName the name of the {@link EntityManagerFactory} bean
	 * @return a name for the given entity manager factory
	 */
	private String getEntityManagerFactoryName(String beanName) {
		if (beanName.length() > ENTITY_MANAGER_FACTORY_SUFFIX.length() && StringUtils
				.endsWithIgnoreCase(beanName, ENTITY_MANAGER_FACTORY_SUFFIX)) {
			return beanName.substring(0,
					beanName.length() - ENTITY_MANAGER_FACTORY_SUFFIX.length());
		}
		return beanName;
	}

}
