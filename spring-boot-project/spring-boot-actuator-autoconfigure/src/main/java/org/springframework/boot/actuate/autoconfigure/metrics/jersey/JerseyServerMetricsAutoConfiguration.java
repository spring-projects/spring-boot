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

package org.springframework.boot.actuate.autoconfigure.metrics.jersey;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.jersey.server.AnnotationFinder;
import io.micrometer.core.instrument.binder.jersey.server.DefaultJerseyTagsProvider;
import io.micrometer.core.instrument.binder.jersey.server.JerseyTagsProvider;
import io.micrometer.core.instrument.binder.jersey.server.MetricsApplicationEventListener;
import io.micrometer.core.instrument.config.MeterFilter;
import org.glassfish.jersey.server.ResourceConfig;

import org.springframework.boot.actuate.autoconfigure.metrics.MetricsAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.MetricsProperties;
import org.springframework.boot.actuate.autoconfigure.metrics.OnlyOnceLoggingDenyMeterFilter;
import org.springframework.boot.actuate.autoconfigure.metrics.export.simple.SimpleMetricsExportAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.observation.ObservationProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.jersey.ResourceConfigCustomizer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.annotation.Order;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Jersey server instrumentation.
 *
 * @author Michael Weirauch
 * @author Michael Simons
 * @author Andy Wilkinson
 * @since 2.1.0
 */
@AutoConfiguration(after = { MetricsAutoConfiguration.class, SimpleMetricsExportAutoConfiguration.class })
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass({ ResourceConfig.class, MetricsApplicationEventListener.class })
@ConditionalOnBean({ MeterRegistry.class, ResourceConfig.class })
@EnableConfigurationProperties(MetricsProperties.class)
public class JerseyServerMetricsAutoConfiguration {

	private final ObservationProperties observationProperties;

	public JerseyServerMetricsAutoConfiguration(ObservationProperties observationProperties) {
		this.observationProperties = observationProperties;
	}

	@Bean
	@ConditionalOnMissingBean(JerseyTagsProvider.class)
	public DefaultJerseyTagsProvider jerseyTagsProvider() {
		return new DefaultJerseyTagsProvider();
	}

	@Bean
	public ResourceConfigCustomizer jerseyServerMetricsResourceConfigCustomizer(MeterRegistry meterRegistry,
			JerseyTagsProvider tagsProvider) {
		String metricName = this.observationProperties.getHttp().getServer().getRequests().getName();
		return (config) -> config.register(new MetricsApplicationEventListener(meterRegistry, tagsProvider, metricName,
				true, new AnnotationUtilsAnnotationFinder()));
	}

	@Bean
	@Order(0)
	public MeterFilter jerseyMetricsUriTagFilter(MetricsProperties metricsProperties) {
		String metricName = this.observationProperties.getHttp().getServer().getRequests().getName();
		MeterFilter filter = new OnlyOnceLoggingDenyMeterFilter(
				() -> String.format("Reached the maximum number of URI tags for '%s'.", metricName));
		return MeterFilter.maximumAllowableTags(metricName, "uri",
				metricsProperties.getWeb().getServer().getMaxUriTags(), filter);
	}

	/**
	 * An {@link AnnotationFinder} that uses {@link AnnotationUtils}.
	 */
	private static class AnnotationUtilsAnnotationFinder implements AnnotationFinder {

		@Override
		public <A extends Annotation> A findAnnotation(AnnotatedElement annotatedElement, Class<A> annotationType) {
			return AnnotationUtils.findAnnotation(annotatedElement, annotationType);
		}

	}

}
