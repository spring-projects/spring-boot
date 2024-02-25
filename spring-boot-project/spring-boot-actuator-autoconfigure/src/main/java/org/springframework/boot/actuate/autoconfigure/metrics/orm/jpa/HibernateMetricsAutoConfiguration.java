/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.metrics.orm.jpa;

import java.util.Collections;
import java.util.Map;

import io.micrometer.core.instrument.MeterRegistry;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceException;
import org.hibernate.SessionFactory;
import org.hibernate.stat.HibernateMetrics;

import org.springframework.beans.factory.SmartInitializingSingleton;
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
@Configuration(proxyBeanMethods = false)
@AutoConfigureAfter({ MetricsAutoConfiguration.class, HibernateJpaAutoConfiguration.class,
		SimpleMetricsExportAutoConfiguration.class })
@ConditionalOnClass({ EntityManagerFactory.class, SessionFactory.class, HibernateMetrics.class, MeterRegistry.class })
@ConditionalOnBean({ EntityManagerFactory.class, MeterRegistry.class })
public class HibernateMetricsAutoConfiguration implements SmartInitializingSingleton {

	private static final String ENTITY_MANAGER_FACTORY_SUFFIX = "entityManagerFactory";

	private final Map<String, EntityManagerFactory> entityManagerFactories;

	private final MeterRegistry meterRegistry;

	/**
	 * Constructs a new HibernateMetricsAutoConfiguration with the specified
	 * entityManagerFactories and meterRegistry.
	 * @param entityManagerFactories a map of entity manager factories
	 * @param meterRegistry the meter registry to be used for metrics
	 */
	public HibernateMetricsAutoConfiguration(Map<String, EntityManagerFactory> entityManagerFactories,
			MeterRegistry meterRegistry) {
		this.entityManagerFactories = entityManagerFactories;
		this.meterRegistry = meterRegistry;
	}

	/**
	 * Binds the EntityManagerFactories to the MeterRegistry after all singletons have
	 * been instantiated.
	 *
	 * @since 1.0
	 */
	@Override
	public void afterSingletonsInstantiated() {
		bindEntityManagerFactoriesToRegistry(this.entityManagerFactories, this.meterRegistry);
	}

	/**
	 * Binds the provided EntityManagerFactories to the given MeterRegistry.
	 * @param entityManagerFactories a map of EntityManagerFactory instances, where the
	 * key is the name of the factory
	 * @param registry the MeterRegistry to bind the EntityManagerFactories to
	 */
	public void bindEntityManagerFactoriesToRegistry(Map<String, EntityManagerFactory> entityManagerFactories,
			MeterRegistry registry) {
		entityManagerFactories.forEach((name, factory) -> bindEntityManagerFactoryToRegistry(name, factory, registry));
	}

	/**
	 * Binds the EntityManagerFactory to the MeterRegistry by creating a HibernateMetrics
	 * instance and binding it to the registry.
	 * @param beanName the name of the bean associated with the EntityManagerFactory
	 * @param entityManagerFactory the EntityManagerFactory to bind
	 * @param registry the MeterRegistry to bind the HibernateMetrics instance to
	 */
	private void bindEntityManagerFactoryToRegistry(String beanName, EntityManagerFactory entityManagerFactory,
			MeterRegistry registry) {
		String entityManagerFactoryName = getEntityManagerFactoryName(beanName);
		try {
			new HibernateMetrics(entityManagerFactory.unwrap(SessionFactory.class), entityManagerFactoryName,
					Collections.emptyList())
				.bindTo(registry);
		}
		catch (PersistenceException ex) {
			// Continue
		}
	}

	/**
	 * Get the name of an {@link EntityManagerFactory} based on its {@code beanName}.
	 * @param beanName the name of the {@link EntityManagerFactory} bean
	 * @return a name for the given entity manager factory
	 */
	private String getEntityManagerFactoryName(String beanName) {
		if (beanName.length() > ENTITY_MANAGER_FACTORY_SUFFIX.length()
				&& StringUtils.endsWithIgnoreCase(beanName, ENTITY_MANAGER_FACTORY_SUFFIX)) {
			return beanName.substring(0, beanName.length() - ENTITY_MANAGER_FACTORY_SUFFIX.length());
		}
		return beanName;
	}

}
