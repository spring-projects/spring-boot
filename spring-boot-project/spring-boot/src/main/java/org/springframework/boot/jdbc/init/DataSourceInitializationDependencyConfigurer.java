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

package org.springframework.boot.jdbc.init;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.sql.DataSource;

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
 * Configures beans that depend upon DataSource initialization with
 * {@link BeanDefinition#getDependsOn()} dependencies upon beans that perform
 * {@link DataSource} initialization. Intended for {@link Import import} in configuration
 * classes that define {@code DataSource} initialization beans or that define beans that
 * require DataSource initialization to have completed before they are initialized.
 * <p>
 * Beans that initialize a {@link DataSource} are identified by
 * {@link DataSourceInitializerDetector DataSourceInitializerDetectors}. Beans that depend
 * upon DataSource initialization are identified by
 * {@link DependsOnDataSourceInitializationDetector
 * DependsOnDataSourceInitializationDetectors}.
 *
 * @author Andy Wilkinson
 * @since 2.5.0
 * @see DataSourceInitializerDetector
 * @see DependsOnDataSourceInitializationDetector
 * @see DependsOnDataSourceInitialization
 */
public class DataSourceInitializationDependencyConfigurer implements ImportBeanDefinitionRegistrar {

	private final Environment environment;

	DataSourceInitializationDependencyConfigurer(Environment environment) {
		this.environment = environment;
	}

	@Override
	public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
		if (registry.containsBeanDefinition(DependsOnDataSourceInitializationPostProcessor.class.getName())) {
			return;
		}
		registry.registerBeanDefinition(DependsOnDataSourceInitializationPostProcessor.class.getName(),
				BeanDefinitionBuilder
						.genericBeanDefinition(DependsOnDataSourceInitializationPostProcessor.class,
								() -> new DependsOnDataSourceInitializationPostProcessor(this.environment))
						.getBeanDefinition());
	}

	static class DependsOnDataSourceInitializationPostProcessor implements BeanFactoryPostProcessor {

		private final Environment environment;

		DependsOnDataSourceInitializationPostProcessor(Environment environment) {
			this.environment = environment;
		}

		@Override
		public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
			Set<String> detectedDataSourceInitializers = detectDataSourceInitializers(beanFactory);
			for (String dependentDefinitionName : detectDependsOnDataSourceInitialization(beanFactory,
					this.environment)) {
				BeanDefinition definition = getBeanDefinition(dependentDefinitionName, beanFactory);
				String[] dependencies = definition.getDependsOn();
				for (String dependencyName : detectedDataSourceInitializers) {
					dependencies = StringUtils.addStringToArray(dependencies, dependencyName);
				}
				definition.setDependsOn(dependencies);
			}
		}

		private Set<String> detectDataSourceInitializers(ConfigurableListableBeanFactory beanFactory) {
			List<DataSourceInitializerDetector> detectors = instantiateDetectors(beanFactory, this.environment,
					DataSourceInitializerDetector.class);
			Set<String> detected = new HashSet<>();
			for (DataSourceInitializerDetector detector : detectors) {
				for (String initializerName : detector.detect(beanFactory)) {
					detected.add(initializerName);
					beanFactory.getBeanDefinition(initializerName)
							.setAttribute(DataSourceInitializerDetector.class.getName(), detector.getClass().getName());
				}
			}
			detected = Collections.unmodifiableSet(detected);
			for (DataSourceInitializerDetector detector : detectors) {
				detector.detectionComplete(beanFactory, detected);
			}
			return detected;
		}

		private Collection<String> detectDependsOnDataSourceInitialization(ConfigurableListableBeanFactory beanFactory,
				Environment environment) {
			List<DependsOnDataSourceInitializationDetector> detectors = instantiateDetectors(beanFactory, environment,
					DependsOnDataSourceInitializationDetector.class);
			Set<String> dependentUponDataSourceInitialization = new HashSet<>();
			for (DependsOnDataSourceInitializationDetector detector : detectors) {
				dependentUponDataSourceInitialization.addAll(detector.detect(beanFactory));
			}
			return dependentUponDataSourceInitialization;
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
