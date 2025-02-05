/*
 * Copyright 2012-2025 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.r2dbc;

import io.r2dbc.spi.ConnectionFactory;

import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.actuate.autoconfigure.health.CompositeReactiveHealthContributorConfiguration;
import org.springframework.boot.actuate.autoconfigure.health.ConditionalOnEnabledHealthIndicator;
import org.springframework.boot.actuate.health.ReactiveHealthContributor;
import org.springframework.boot.actuate.r2dbc.ConnectionFactoryHealthIndicator;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.r2dbc.R2dbcAutoConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for
 * {@link ConnectionFactoryHealthIndicator}.
 *
 * @author Mark Paluch
 * @since 2.3.0
 */
@AutoConfiguration(after = R2dbcAutoConfiguration.class)
@ConditionalOnClass(ConnectionFactory.class)
@ConditionalOnBean(ConnectionFactory.class)
@ConditionalOnEnabledHealthIndicator("r2dbc")
public class ConnectionFactoryHealthContributorAutoConfiguration
		extends CompositeReactiveHealthContributorConfiguration<ConnectionFactoryHealthIndicator, ConnectionFactory> {

	ConnectionFactoryHealthContributorAutoConfiguration() {
		super(ConnectionFactoryHealthIndicator::new);
	}

	@Bean
	@ConditionalOnMissingBean(name = { "r2dbcHealthIndicator", "r2dbcHealthContributor" })
	public ReactiveHealthContributor r2dbcHealthContributor(ConfigurableListableBeanFactory beanFactory) {
		return createContributor(beanFactory, ConnectionFactory.class);
	}

}
