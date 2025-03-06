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

package org.springframework.boot.autoconfigure.r2dbc;

import io.r2dbc.proxy.ProxyConnectionFactory;
import io.r2dbc.spi.ConnectionFactory;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.r2dbc.ConnectionFactoryDecorator;
import org.springframework.context.annotation.Bean;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for {@link ProxyConnectionFactory}.
 *
 * @author Tadaya Tsuyukubo
 * @author Moritz Halbritter
 * @since 3.4.0
 */
@AutoConfiguration
@ConditionalOnClass({ ConnectionFactory.class, ProxyConnectionFactory.class })
public class R2dbcProxyAutoConfiguration {

	@Bean
	ConnectionFactoryDecorator connectionFactoryDecorator(
			ObjectProvider<ProxyConnectionFactoryCustomizer> customizers) {
		return (connectionFactory) -> {
			ProxyConnectionFactory.Builder builder = ProxyConnectionFactory.builder(connectionFactory);
			customizers.orderedStream().forEach((customizer) -> customizer.customize(builder));
			return builder.build();
		};
	}

}
