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

package org.springframework.boot.test.autoconfigure.actuate.observability;

import java.util.List;
import java.util.Objects;

import io.micrometer.tracing.Tracer;

import org.springframework.aot.AotDetector;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ConfigurationClassPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.test.context.ContextConfigurationAttributes;
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.ContextCustomizerFactory;
import org.springframework.test.context.MergedContextConfiguration;
import org.springframework.test.context.TestContextAnnotationUtils;
import org.springframework.util.ClassUtils;

/**
 * {@link ContextCustomizerFactory} that globally disables metrics export and tracing
 * unless {@link AutoConfigureObservability} is set on the test class.
 * <p>
 * Registers {@link Tracer#NOOP} if tracing is disabled, micrometer-tracing is on the
 * classpath, and the user hasn't supplied their own {@link Tracer}.
 *
 * @author Chris Bono
 * @author Moritz Halbritter
 */
class ObservabilityContextCustomizerFactory implements ContextCustomizerFactory {

	@Override
	public ContextCustomizer createContextCustomizer(Class<?> testClass,
			List<ContextConfigurationAttributes> configAttributes) {
		AutoConfigureObservability annotation = TestContextAnnotationUtils.findMergedAnnotation(testClass,
				AutoConfigureObservability.class);
		if (annotation == null) {
			return new DisableObservabilityContextCustomizer(true, true);
		}
		return new DisableObservabilityContextCustomizer(!annotation.metrics(), !annotation.tracing());
	}

	private static class DisableObservabilityContextCustomizer implements ContextCustomizer {

		private final boolean disableMetrics;

		private final boolean disableTracing;

		DisableObservabilityContextCustomizer(boolean disableMetrics, boolean disableTracing) {
			this.disableMetrics = disableMetrics;
			this.disableTracing = disableTracing;
		}

		@Override
		public void customizeContext(ConfigurableApplicationContext context,
				MergedContextConfiguration mergedContextConfiguration) {
			if (this.disableMetrics) {
				TestPropertyValues.of("management.defaults.metrics.export.enabled=false",
						"management.simple.metrics.export.enabled=true").applyTo(context);
			}
			if (this.disableTracing) {
				TestPropertyValues.of("management.tracing.enabled=false").applyTo(context);
				registerNoopTracer(context);
			}
		}

		private void registerNoopTracer(ConfigurableApplicationContext context) {
			if (AotDetector.useGeneratedArtifacts()) {
				return;
			}
			if (!ClassUtils.isPresent("io.micrometer.tracing.Tracer", context.getClassLoader())) {
				return;
			}
			ConfigurableListableBeanFactory beanFactory = context.getBeanFactory();
			if (beanFactory instanceof BeanDefinitionRegistry registry) {
				registerNoopTracer(registry);
			}
		}

		private void registerNoopTracer(BeanDefinitionRegistry registry) {
			RootBeanDefinition definition = new RootBeanDefinition(NoopTracerRegistrar.class);
			definition.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
			registry.registerBeanDefinition(NoopTracerRegistrar.class.getName(), definition);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}
			DisableObservabilityContextCustomizer that = (DisableObservabilityContextCustomizer) o;
			return this.disableMetrics == that.disableMetrics && this.disableTracing == that.disableTracing;
		}

		@Override
		public int hashCode() {
			return Objects.hash(this.disableMetrics, this.disableTracing);
		}

	}

	/**
	 * {@link BeanDefinitionRegistryPostProcessor} that runs after the
	 * {@link ConfigurationClassPostProcessor} and adds a {@link Tracer} bean definition
	 * when a {@link Tracer} hasn't already been registered.
	 */
	static class NoopTracerRegistrar implements BeanDefinitionRegistryPostProcessor, Ordered, BeanFactoryAware {

		private BeanFactory beanFactory;

		@Override
		public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
			this.beanFactory = beanFactory;
		}

		@Override
		public int getOrder() {
			return Ordered.LOWEST_PRECEDENCE;
		}

		@Override
		public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
			if (AotDetector.useGeneratedArtifacts()) {
				return;
			}
			if (BeanFactoryUtils.beanNamesForTypeIncludingAncestors((ListableBeanFactory) this.beanFactory,
					Tracer.class, false, false).length == 0) {
				registry.registerBeanDefinition("noopTracer", new RootBeanDefinition(NoopTracerFactoryBean.class));
			}
		}

		@Override
		public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
		}

	}

	static class NoopTracerFactoryBean implements FactoryBean<Tracer> {

		@Override
		public Tracer getObject() {
			return Tracer.NOOP;
		}

		@Override
		public Class<?> getObjectType() {
			return Tracer.class;
		}

	}

}
