/*
 * Copyright 2012-2024 the original author or authors.
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
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.config.MeterFilter;
import org.glassfish.jersey.micrometer.server.AnnotationFinder;
import org.glassfish.jersey.micrometer.server.DefaultJerseyTagsProvider;
import org.glassfish.jersey.micrometer.server.JerseyTagsProvider;
import org.glassfish.jersey.micrometer.server.MetricsApplicationEventListener;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.monitoring.RequestEvent;

import org.springframework.beans.factory.ObjectProvider;
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

	/**
     * Constructs a new instance of JerseyServerMetricsAutoConfiguration with the specified ObservationProperties.
     *
     * @param observationProperties the ObservationProperties to be used by the configuration
     */
    public JerseyServerMetricsAutoConfiguration(ObservationProperties observationProperties) {
		this.observationProperties = observationProperties;
	}

	/**
     * Creates a new instance of {@link DefaultJerseyTagsProvider} if no other bean of type {@link JerseyTagsProvider} or {@link io.micrometer.core.instrument.binder.jersey.server.JerseyTagsProvider} is present.
     * 
     * @return the created {@link DefaultJerseyTagsProvider} instance
     */
    @Bean
	@SuppressWarnings("deprecation")
	@ConditionalOnMissingBean({ JerseyTagsProvider.class,
			io.micrometer.core.instrument.binder.jersey.server.JerseyTagsProvider.class })
	public DefaultJerseyTagsProvider jerseyTagsProvider() {
		return new DefaultJerseyTagsProvider();
	}

	/**
     * Returns a customizer for configuring the Jersey server metrics.
     * 
     * @param meterRegistry The MeterRegistry used for collecting metrics.
     * @param tagsProvider The JerseyTagsProvider used for providing tags for metrics.
     * @param micrometerTagsProvider The micrometerTagsProvider used for providing tags for metrics.
     * @return The ResourceConfigCustomizer for configuring the Jersey server metrics.
     */
    @Bean
	@SuppressWarnings("deprecation")
	public ResourceConfigCustomizer jerseyServerMetricsResourceConfigCustomizer(MeterRegistry meterRegistry,
			ObjectProvider<JerseyTagsProvider> tagsProvider,
			ObjectProvider<io.micrometer.core.instrument.binder.jersey.server.JerseyTagsProvider> micrometerTagsProvider) {
		String metricName = this.observationProperties.getHttp().getServer().getRequests().getName();
		return (config) -> config.register(new MetricsApplicationEventListener(meterRegistry,
				tagsProvider.getIfAvailable(() -> new JerseyTagsProviderAdapter(micrometerTagsProvider.getObject())),
				metricName, true, new AnnotationUtilsAnnotationFinder()));
	}

	/**
     * Returns a MeterFilter bean that filters metrics based on the maximum number of URI tags allowed.
     * The maximum number of URI tags is obtained from the metricsProperties.
     * If the maximum number of URI tags is reached, a logging message is generated.
     * 
     * @param metricsProperties the MetricsProperties bean used to obtain the maximum number of URI tags
     * @return the MeterFilter bean that filters metrics based on the maximum number of URI tags
     */
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
	private static final class AnnotationUtilsAnnotationFinder implements AnnotationFinder {

		/**
         * Finds the specified annotation on the given annotated element.
         * 
         * @param annotatedElement the annotated element to search for the annotation on
         * @param annotationType the type of annotation to find
         * @return the found annotation, or null if not found
         * @throws IllegalArgumentException if the annotated element or annotation type is null
         */
        @Override
		public <A extends Annotation> A findAnnotation(AnnotatedElement annotatedElement, Class<A> annotationType) {
			return AnnotationUtils.findAnnotation(annotatedElement, annotationType);
		}

	}

	/**
     * JerseyTagsProviderAdapter class.
     */
    @SuppressWarnings("deprecation")
	static final class JerseyTagsProviderAdapter implements JerseyTagsProvider {

		private final io.micrometer.core.instrument.binder.jersey.server.JerseyTagsProvider delegate;

		/**
         * Constructs a new JerseyTagsProviderAdapter with the specified delegate.
         *
         * @param delegate the delegate JerseyTagsProvider to be used
         */
        private JerseyTagsProviderAdapter(
				io.micrometer.core.instrument.binder.jersey.server.JerseyTagsProvider delegate) {
			this.delegate = delegate;
		}

		/**
         * Returns an iterable collection of tags for the given HTTP request event.
         *
         * @param event the HTTP request event
         * @return an iterable collection of tags
         */
        @Override
		public Iterable<Tag> httpRequestTags(RequestEvent event) {
			return this.delegate.httpRequestTags(event);
		}

		/**
         * Returns the tags associated with a long HTTP request event.
         * 
         * @param event the HTTP request event
         * @return an iterable collection of tags
         */
        @Override
		public Iterable<Tag> httpLongRequestTags(RequestEvent event) {
			return this.delegate.httpLongRequestTags(event);
		}

	}

}
