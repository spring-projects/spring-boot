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

package org.springframework.boot.sql.init.dependency;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.util.Instantiator;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.Ordered;
import org.springframework.core.env.Environment;
import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.CollectionUtils;
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

	@Override
	public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
		String name = DependsOnDatabaseInitializationPostProcessor.class.getName();
		if (!registry.containsBeanDefinition(name)) {
			BeanDefinitionBuilder builder = BeanDefinitionBuilder
					.genericBeanDefinition(DependsOnDatabaseInitializationPostProcessor.class);
			registry.registerBeanDefinition(name, builder.getBeanDefinition());
		}
	}

	/**
	 * {@link BeanFactoryPostProcessor} used to configure database initialization
	 * dependency relationships.
	 */
	static class DependsOnDatabaseInitializationPostProcessor
			implements BeanFactoryPostProcessor, EnvironmentAware, Ordered {

		private Environment environment;

		@Override
		public void setEnvironment(Environment environment) {
			this.environment = environment;
		}

		@Override
		public int getOrder() {
			return 0;
		}

		@Override
		public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
			InitializerBeanNames initializerBeanNames = detectInitializerBeanNames(beanFactory);
			if (initializerBeanNames.isEmpty()) {
				return;
			}
			Set<String> previousInitializerBeanNamesBatch = null;
			for (Set<String> initializerBeanNamesBatch : initializerBeanNames.batchedBeanNames()) {
				for (String initializerBeanName : initializerBeanNamesBatch) {
					BeanDefinition beanDefinition = getBeanDefinition(initializerBeanName, beanFactory);
					beanDefinition
							.setDependsOn(merge(beanDefinition.getDependsOn(), previousInitializerBeanNamesBatch));
				}
				previousInitializerBeanNamesBatch = initializerBeanNamesBatch;
			}
			for (String dependsOnInitializationBeanNames : detectDependsOnInitializationBeanNames(beanFactory)) {
				BeanDefinition beanDefinition = getBeanDefinition(dependsOnInitializationBeanNames, beanFactory);
				beanDefinition.setDependsOn(merge(beanDefinition.getDependsOn(), initializerBeanNames.beanNames()));
			}
		}

		private String[] merge(String[] source, Set<String> additional) {
			if (CollectionUtils.isEmpty(additional)) {
				return source;
			}
			Set<String> result = new LinkedHashSet<>((source != null) ? Arrays.asList(source) : Collections.emptySet());
			result.addAll(additional);
			return StringUtils.toStringArray(result);
		}

		private InitializerBeanNames detectInitializerBeanNames(ConfigurableListableBeanFactory beanFactory) {
			List<DatabaseInitializerDetector> detectors = getDetectors(beanFactory, DatabaseInitializerDetector.class);
			InitializerBeanNames initializerBeanNames = new InitializerBeanNames();
			for (DatabaseInitializerDetector detector : detectors) {
				for (String beanName : detector.detect(beanFactory)) {
					BeanDefinition beanDefinition = beanFactory.getBeanDefinition(beanName);
					beanDefinition.setAttribute(DatabaseInitializerDetector.class.getName(),
							detector.getClass().getName());
					initializerBeanNames.detected(detector, beanName);
				}
			}
			for (DatabaseInitializerDetector detector : detectors) {
				detector.detectionComplete(beanFactory, initializerBeanNames.beanNames());
			}
			return initializerBeanNames;
		}

		private Collection<String> detectDependsOnInitializationBeanNames(ConfigurableListableBeanFactory beanFactory) {
			List<DependsOnDatabaseInitializationDetector> detectors = getDetectors(beanFactory,
					DependsOnDatabaseInitializationDetector.class);
			Set<String> beanNames = new HashSet<>();
			for (DependsOnDatabaseInitializationDetector detector : detectors) {
				beanNames.addAll(detector.detect(beanFactory));
			}
			return beanNames;
		}

		private <T> List<T> getDetectors(ConfigurableListableBeanFactory beanFactory, Class<T> type) {
			List<String> names = SpringFactoriesLoader.loadFactoryNames(type, beanFactory.getBeanClassLoader());
			Instantiator<T> instantiator = new Instantiator<>(type,
					(availableParameters) -> availableParameters.add(Environment.class, this.environment));
			return instantiator.instantiate(beanFactory.getBeanClassLoader(), names);
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

		static class InitializerBeanNames {

			private final Map<DatabaseInitializerDetector, Set<String>> byDetectorBeanNames = new LinkedHashMap<>();

			private final Set<String> beanNames = new LinkedHashSet<>();

			private void detected(DatabaseInitializerDetector detector, String beanName) {
				this.byDetectorBeanNames.computeIfAbsent(detector, (key) -> new LinkedHashSet<>()).add(beanName);
				this.beanNames.add(beanName);
			}

			private boolean isEmpty() {
				return this.beanNames.isEmpty();
			}

			private Iterable<Set<String>> batchedBeanNames() {
				return this.byDetectorBeanNames.values();
			}

			private Set<String> beanNames() {
				return Collections.unmodifiableSet(this.beanNames);
			}

		}

	}

}
