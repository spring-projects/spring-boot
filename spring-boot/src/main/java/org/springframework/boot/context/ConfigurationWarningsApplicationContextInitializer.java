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

package org.springframework.boot.context;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * {@link ApplicationContextInitializer} to report warnings for common misconfiguration
 * mistakes.
 *
 * @author Phillip Webb
 * @since 1.2.0
 */
public class ConfigurationWarningsApplicationContextInitializer
		implements ApplicationContextInitializer<ConfigurableApplicationContext> {

	private static Log logger = LogFactory
			.getLog(ConfigurationWarningsApplicationContextInitializer.class);

	@Override
	public void initialize(ConfigurableApplicationContext context) {
		context.addBeanFactoryPostProcessor(
				new ConfigurationWarningsPostProcessor(getChecks()));
	}

	/**
	 * Returns the checks that should be applied.
	 * @return the checks to apply
	 */
	protected Check[] getChecks() {
		return new Check[] { new ComponentScanDefaultPackageCheck() };
	}

	/**
	 * {@link BeanDefinitionRegistryPostProcessor} to report warnings.
	 */
	protected final static class ConfigurationWarningsPostProcessor
			implements PriorityOrdered, BeanDefinitionRegistryPostProcessor {

		private Check[] checks;

		public ConfigurationWarningsPostProcessor(Check[] checks) {
			this.checks = checks;
		}

		@Override
		public int getOrder() {
			return Ordered.LOWEST_PRECEDENCE - 1;
		}

		@Override
		public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory)
				throws BeansException {
		}

		@Override
		public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry)
				throws BeansException {
			for (Check check : this.checks) {
				String message = check.getWarning(registry);
				if (StringUtils.hasLength(message)) {
					warn(message);
				}
			}

		}

		private void warn(String message) {
			if (logger.isWarnEnabled()) {
				logger.warn("\n\n** WARNING ** : " + message + "\n\n");
			}
		}

	}

	/**
	 * A single check that can be applied.
	 */
	protected interface Check {

		/**
		 * Returns a warning if the check fails or {@code null} if there are no problems.
		 * @param registry the {@link BeanDefinitionRegistry}
		 * @return a warning message or {@code null}
		 */
		String getWarning(BeanDefinitionRegistry registry);

	}

	/**
	 * {@link Check} for {@code @ComponentScan} on the default package.
	 */
	protected static class ComponentScanDefaultPackageCheck implements Check {

		@Override
		public String getWarning(BeanDefinitionRegistry registry) {
			if (isComponentScanningDefaultPackage(registry)) {
				return "Your ApplicationContext is unlikely to start due to a "
						+ "@ComponentScan of the default package.";
			}
			return null;
		}

		private boolean isComponentScanningDefaultPackage(
				BeanDefinitionRegistry registry) {
			String[] names = registry.getBeanDefinitionNames();
			for (String name : names) {
				BeanDefinition definition = registry.getBeanDefinition(name);
				if (definition instanceof AnnotatedBeanDefinition) {
					AnnotatedBeanDefinition annotatedDefinition = (AnnotatedBeanDefinition) definition;
					if (isScanningDefaultPackage(annotatedDefinition.getMetadata())) {
						return true;
					}
				}
			}
			return false;
		}

		private boolean isScanningDefaultPackage(AnnotationMetadata metadata) {
			AnnotationAttributes attributes = AnnotationAttributes.fromMap(metadata
					.getAnnotationAttributes(ComponentScan.class.getName(), true));
			if (attributes != null && hasNoScanPackageSpecified(attributes)) {
				if (isInDefaultPackage(metadata.getClassName())) {
					return true;
				}
			}
			return false;
		}

		private boolean hasNoScanPackageSpecified(AnnotationAttributes attributes) {
			return isAllEmpty(attributes, "value", "basePackages", "basePackageClasses");
		}

		private boolean isAllEmpty(AnnotationAttributes attributes, String... names) {
			for (String name : names) {
				if (!ObjectUtils.isEmpty(attributes.getStringArray(name))) {
					return false;
				}
			}
			return true;
		}

		protected boolean isInDefaultPackage(String className) {
			String packageName = ClassUtils.getPackageName(className);
			return StringUtils.isEmpty(packageName);
		}
	}

}
