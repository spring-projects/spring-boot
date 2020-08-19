/*
 * Copyright 2020 the original author or authors.
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
package org.springframework.boot.actuate.autoconfigure.metrics.cassandra;

import java.security.NoSuchAlgorithmException;

import javax.net.ssl.SSLContext;

import com.datastax.oss.driver.api.core.CqlSessionBuilder;
import com.datastax.oss.driver.api.core.config.DriverConfigLoader;
import com.datastax.oss.driver.api.core.context.DriverContext;
import com.datastax.oss.driver.api.core.session.ProgrammaticArguments;
import com.datastax.oss.driver.internal.metrics.micrometer.MicrometerDriverContext;
import io.micrometer.core.instrument.MeterRegistry;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.cassandra.CassandraProperties;
import org.springframework.boot.autoconfigure.cassandra.CqlSessionBuilderCustomizer;
import org.springframework.context.ApplicationContext;
import org.springframework.core.Ordered;

/**
 * {@link BeanPostProcessor} that configures Cassandra metrics. This is necessary as the
 * CqlSessionBuilder bean provided by {@link CassandraAutoConfigure} must be overridden in
 * order to enable driver metrics. The post-processing here will do just that.
 *
 * @author Erik Merkle
 */
class CassandraMetricsPostProcessor implements BeanPostProcessor, Ordered {

	private final ApplicationContext context;

	private final CassandraProperties cassandraProperties;

	private final DriverConfigLoader driverConfigLoader;

	private final ObjectProvider<CqlSessionBuilderCustomizer> builderCustomizers;

	CassandraMetricsPostProcessor(ApplicationContext context, CassandraProperties cassandraProperties,
			DriverConfigLoader driverConfigLoader, ObjectProvider<CqlSessionBuilderCustomizer> builderCustomizers) {
		this.context = context;
		this.cassandraProperties = cassandraProperties;
		this.driverConfigLoader = driverConfigLoader;
		this.builderCustomizers = builderCustomizers;
	}

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) {
		if (bean instanceof CqlSessionBuilder) {
			MetricsSessionBuilder builder = new MetricsSessionBuilder(this.context.getBean(MeterRegistry.class));
			// the Metrics enabled builder will need all of the same config as a regular
			// CqlSessionBUilder
			builder.withConfigLoader(this.driverConfigLoader);
			configureAuthentication(this.cassandraProperties, builder);
			configureSsl(this.cassandraProperties, builder);
			builder.withKeyspace(this.cassandraProperties.getKeyspaceName());
			this.builderCustomizers.orderedStream().forEach((customizer) -> customizer.customize(builder));
			// override the builder bean
			return builder;
		}
		return bean;
	}

	private void configureAuthentication(CassandraProperties properties, MetricsSessionBuilder builder) {
		if (properties.getUsername() != null) {
			builder.withAuthCredentials(properties.getUsername(), properties.getPassword());
		}
	}

	private void configureSsl(CassandraProperties properties, MetricsSessionBuilder builder) {
		if (properties.isSsl()) {
			try {
				builder.withSslContext(SSLContext.getDefault());
			}
			catch (NoSuchAlgorithmException ex) {
				throw new IllegalStateException("Could not setup SSL default context for Cassandra", ex);
			}
		}
	}

	@Override
	public int getOrder() {
		return Ordered.HIGHEST_PRECEDENCE;
	}

	static class MetricsSessionBuilder extends CqlSessionBuilder {

		private final MeterRegistry registry;

		MetricsSessionBuilder(MeterRegistry registry) {
			this.registry = registry;
		}

		@Override
		protected DriverContext buildContext(DriverConfigLoader configLoader,
				ProgrammaticArguments programmaticArguments) {
			return new MicrometerDriverContext(configLoader, programmaticArguments, this.registry);
		}

	}

}
