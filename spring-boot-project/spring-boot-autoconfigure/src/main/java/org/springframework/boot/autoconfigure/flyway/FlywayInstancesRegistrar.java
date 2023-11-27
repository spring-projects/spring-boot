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

package org.springframework.boot.autoconfigure.flyway;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Stream;

import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.callback.Callback;
import org.flywaydb.core.api.migration.JavaMigration;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.Assert;

class FlywayInstancesRegistrar implements ImportBeanDefinitionRegistrar {
	private final ListableBeanFactory listableBeanFactory;
	private final Binder binder;

	public FlywayInstancesRegistrar(BeanFactory beanFactory, Environment environment) {
		this.listableBeanFactory = (ListableBeanFactory) beanFactory;
		this.binder = Binder.get(environment);
	}

	@Override
	public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
		FlywayInstancesProperties instancesProperties = new FlywayInstancesProperties();
		this.binder.bind("spring.flyway", Bindable.ofInstance(instancesProperties));

		if (instancesProperties.getInstances().isEmpty()) {
			registry.registerBeanDefinition("flyway",
					BeanDefinitionBuilder.genericBeanDefinition(Flyway.class, createFlywaySupplier(null, instancesProperties)).getBeanDefinition());

			if (this.listableBeanFactory.getBeanNamesForType(FlywayMigrationInitializer.class).length == 0) {
				registry.registerBeanDefinition("flywayMigrationInitializer",
						BeanDefinitionBuilder.genericBeanDefinition(FlywayMigrationInitializer.class,
								createFlywayMigrationInitializerSupplier(null, "flyway")).getBeanDefinition());
			}
		} else {
			for (String flywayInstanceName : instancesProperties.getInstances().keySet()) {
				FlywayProperties properties = bindFlywayInstanceProperties(flywayInstanceName);

				if (properties.isEnabled()) {
					String flywayBeanName = "flyway-" + flywayInstanceName;

					registry.registerBeanDefinition(flywayBeanName,
							BeanDefinitionBuilder.genericBeanDefinition(Flyway.class, createFlywaySupplier(flywayInstanceName, properties)).getBeanDefinition());

					registry.registerBeanDefinition("flywayMigrationInitializer-" + flywayInstanceName,
							BeanDefinitionBuilder.genericBeanDefinition(FlywayMigrationInitializer.class,
									createFlywayMigrationInitializerSupplier(flywayInstanceName, flywayBeanName)).getBeanDefinition());
				}
			}
		}
	}

	private FlywayProperties bindFlywayInstanceProperties(String flywayInstanceName) {
		FlywayProperties properties = new FlywayProperties();
		this.binder.bind("spring.flyway", Bindable.ofInstance(properties));
		this.binder.bind("spring.flyway.instances." + flywayInstanceName, Bindable.ofInstance(properties));
		return properties;
	}

	private Supplier<Flyway> createFlywaySupplier(String flywayInstanceName, FlywayProperties properties) {
		return () -> {
			ObjectProvider<FlywayConnectionDetails> connectionDetails = findBeans(flywayInstanceName, FlywayConnectionDetails.class, null, true, true);
			ObjectProvider<DataSource> flywayDataSource = findBeans(flywayInstanceName, DataSource.class, FlywayDataSource.class, true, true);
			ObjectProvider<FlywayConfigurationCustomizer> flywayConfigurationCustomizers = findBeans(flywayInstanceName, FlywayConfigurationCustomizer.class, null, false, true);
			ObjectProvider<Callback> callbacks = findBeans(flywayInstanceName, Callback.class, null, false, true);
			ObjectProvider<JavaMigration> javaMigrations = findBeans(flywayInstanceName, JavaMigration.class, null, true, false);

			FlywayFactory flywayFactory = this.listableBeanFactory.getBean(FlywayFactory.class);
			return flywayFactory.createFlyway(properties, connectionDetails, flywayDataSource, flywayConfigurationCustomizers, callbacks, javaMigrations);
		};
	}

	private Supplier<FlywayMigrationInitializer> createFlywayMigrationInitializerSupplier(String flywayInstanceName, String flywayBeanName) {
		return () -> {
			ObjectProvider<FlywayMigrationStrategy> flywayMigrationStrategy = findBeans(flywayInstanceName, FlywayMigrationStrategy.class, null, true, true);
			return new FlywayMigrationInitializer(this.listableBeanFactory.getBean(flywayBeanName, Flyway.class), flywayMigrationStrategy.getIfAvailable());
		};
	}

	private <T> ObjectProvider<T> findBeans(String flywayInstanceName, Class<T> type, Class<? extends Annotation> additionalAnnotation, boolean preferInstanceBeans, boolean allowGeneralBeans) {
		if (flywayInstanceName == null) {
			return this.listableBeanFactory.getBeanProvider(type);
		}

		Map<String, T> generalBeans = new HashMap<>();
		Map<String, T> instanceBeans = new HashMap<>();

		for (Map.Entry<String, T> beanWithName : this.listableBeanFactory.getBeansOfType(type).entrySet()) {
			if (additionalAnnotation != null && this.listableBeanFactory.findAnnotationOnBean(beanWithName.getKey(), additionalAnnotation) == null) {
				continue;
			}

			FlywayInstance flywayInstanceSpecific = this.listableBeanFactory.findAnnotationOnBean(beanWithName.getKey(), FlywayInstance.class);
			if (flywayInstanceSpecific == null) {
				Assert.isTrue(allowGeneralBeans, "Undefined instance binding for bean " + beanWithName.getKey() + " of type " +  type.getName());

				generalBeans.put(beanWithName.getKey(), beanWithName.getValue());
			} else if (flywayInstanceSpecific.value().equals(flywayInstanceName)) {
				instanceBeans.put(beanWithName.getKey(), beanWithName.getValue());
			}
		}

		Stream<Map<String, T>> stream;
		if (preferInstanceBeans && !instanceBeans.isEmpty()) {
			stream = Stream.of(instanceBeans);
		} else {
			stream = Stream.of(generalBeans, instanceBeans);
		}

		StaticListableBeanFactory staticListableBeanFactory = new StaticListableBeanFactory();
		stream.flatMap(map -> map.entrySet().stream()).forEach(beanWithName -> staticListableBeanFactory.addBean(beanWithName.getKey(), beanWithName.getValue()));
		return staticListableBeanFactory.getBeanProvider(type);
	}

}
