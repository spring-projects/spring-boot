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

package org.springframework.boot.actuate.autoconfigure.metrics.jersey2.server;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.jersey2.server.AnnotationFinder;
import io.micrometer.jersey2.server.DefaultJerseyTagsProvider;
import io.micrometer.jersey2.server.JerseyTagsProvider;
import io.micrometer.jersey2.server.MetricsApplicationEventListener;
import org.glassfish.jersey.server.ResourceConfig;

import org.springframework.boot.actuate.autoconfigure.metrics.MetricsAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.export.simple.SimpleMetricsExportAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.jersey.ResourceConfigCustomizer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AnnotationUtils;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Jersey server instrumentation.
 *
 * @author Michael Weirauch
 * @author Michael Simons
 * @since 2.1.0
 */
@Configuration
@AutoConfigureAfter({ MetricsAutoConfiguration.class,
		SimpleMetricsExportAutoConfiguration.class })
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass({ ResourceConfig.class, MetricsApplicationEventListener.class })
@ConditionalOnBean({ MeterRegistry.class, ResourceConfig.class })
@EnableConfigurationProperties(JerseyServerMetricsProperties.class)
public class JerseyServerMetricsAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean(JerseyTagsProvider.class)
	public DefaultJerseyTagsProvider jerseyTagsProvider() {
		return new DefaultJerseyTagsProvider();
	}

	@Bean
	public ResourceConfigCustomizer jerseyServerMetricsResourceConfigCustomizer(
			MeterRegistry meterRegistry, JerseyServerMetricsProperties properties,
			JerseyTagsProvider tagsProvider) {
		return (config) -> config.register(new MetricsApplicationEventListener(
				meterRegistry, tagsProvider, properties.getRequestsMetricName(),
				properties.isAutoTimeRequests(), new AnnotationFinder() {
					@Override
					public <A extends Annotation> A findAnnotation(
							AnnotatedElement annotatedElement, Class<A> annotationType) {
						return AnnotationUtils.findAnnotation(annotatedElement,
								annotationType);
					}
				}));
	}

}
