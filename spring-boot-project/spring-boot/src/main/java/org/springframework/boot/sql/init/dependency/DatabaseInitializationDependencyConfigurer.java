/*
 * Copyright 2012-2021 the original author or authors.
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

package org.springframework.boot.sql.init.dependency;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.util.Instantiator;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.env.Environment;
import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.StringUtils;

/**
 * Configures beans that depend upon SQL database initialization with
 * {@link BeanDefinition#getDependsOn() dependencies} upon beans that perform database
 * initialization. Intended for {@link Import import} in configuration classes that define
 * database initialization beans or that define beans that require database initialization
 * to have completed before they are initialized.
 * <p>
 * Beans that initialize a database are identified by {@link DatabaseInitializerDetector
 * DatabaseInitializerDetectors}. Beans that depend upon database initialization are
 * identified by {@link DependsOnDatabaseInitializationDetector
 * DependsOnDatabaseInitializationDetectors}.
 *
 * @author Andy Wilkinson
 * @since 2.5.0
 * @see DatabaseInitializerDetector
 * @see DependsOnDatabaseInitializationDetector
 * @see DependsOnDatabaseInitialization
 */
public class DatabaseInitializationDependencyConfigurer implements ImportBeanDefinitionRegistrar {

	private final Environment environment;

	DatabaseInitializationDependencyConfigurer(Environment environment) {
		this.environment = environment;
	}

	@Override
	public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
		if (registry.containsBeanDefinition(DependsOnDatabaseInitializationPostProcessor.class.getName())) {
			return;
		}
		registry.registerBeanDefinition(DependsOnDatabaseInitializationPostProcessor.class.getName(),
				BeanDefinitionBuilder
						.genericBeanDefinition(DependsOnDatabaseInitializationPostProcessor.class,
								() -> new DependsOnDatabaseInitializationPostProcessor(this.environment))
						.getBeanDefinition());
	}

	static class DependsOnDatabaseInitializationPostProcessor implements BeanFactoryPostProcessor {

		private final Environment environment;

		DependsOnDatabaseInitializationPostProcessor(Environment environment) {
			this.environment = environment;
		}

		@Override
		public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
			Set<String> detectedDatabaseInitializers = detectDatabaseInitializers(beanFactory);
			if (detectedDatabaseInitializers.isEmpty()) {
				return;
			}
			for (String dependentDefinitionName : detectDependsOnDatabaseInitialization(beanFactory,
					this.environment)) {
				BeanDefinition definition = getBeanDefinition(dependentDefinitionName, beanFactory);
				String[] dependencies = definition.getDependsOn();
				for (String dependencyName : detectedDatabaseInitializers) {
					dependencies = StringUtils.addStringToArray(dependencies, dependencyName);
				}
				definition.setDependsOn(dependencies);
			}
		}

		private Set<String> detectDatabaseInitializers(ConfigurableListableBeanFactory beanFactory) {
			List<DatabaseInitializerDetector> detectors = instantiateDetectors(beanFactory, this.environment,
					DatabaseInitializerDetector.class);
			Set<String> detected = new HashSet<>();
			for (DatabaseInitializerDetector detector : detectors) {
				for (String initializerName : detector.detect(beanFactory)) {
					detected.add(initializerName);
					beanFactory.getBeanDefinition(initializerName)
							.setAttribute(DatabaseInitializerDetector.class.getName(), detector.getClass().getName());
				}
			}
			detected = Collections.unmodifiableSet(detected);
			for (DatabaseInitializerDetector detector : detectors) {
				detector.detectionComplete(beanFactory, detected);
			}
			return detected;
		}

		private Collection<String> detectDependsOnDatabaseInitialization(ConfigurableListableBeanFactory beanFactory,
				Environment environment) {
			List<DependsOnDatabaseInitializationDetector> detectors = instantiateDetectors(beanFactory, environment,
					DependsOnDatabaseInitializationDetector.class);
			Set<String> dependentUponDatabaseInitialization = new HashSet<>();
			for (DependsOnDatabaseInitializationDetector detector : detectors) {
				dependentUponDatabaseInitialization.addAll(detector.detect(beanFactory));
			}
			return dependentUponDatabaseInitialization;
		}

		private <T> List<T> instantiateDetectors(ConfigurableListableBeanFactory beanFactory, Environment environment,
				Class<T> detectorType) {
			List<String> detectorNames = SpringFactoriesLoader.loadFactoryNames(detectorType,
					beanFactory.getBeanClassLoader());
			Instantiator<T> instantiator = new Instantiator<>(detectorType,
					(availableParameters) -> availableParameters.add(Environment.class, environment));
			List<T> detectors = instantiator.instantiate(detectorNames);
			return detectors;
		}

		private static BeanDefinition getBeanDefinition(String beanName, ConfigurableListableBeanFactory beanFactory) {
			try {
				return beanFactory.getBeanDefinition(beanName);
			}
			catch (NoSuchBeanDefinitionException ex) {
				BeanFactory parentBeanFactory = beanFactory.getParentBeanFactory();
				if (parentBeanFactory instanceof ConfigurableListableBeanFactory) {
					return getBeanDefinition(beanName, (ConfigurableListableBeanFactory) parentBeanFactory);
				}
				throw ex;
			}
		}

	}

}
