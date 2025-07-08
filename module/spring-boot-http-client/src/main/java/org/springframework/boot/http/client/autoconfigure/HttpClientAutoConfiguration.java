/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.http.client.autoconfigure;

import java.util.List;

import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.ssl.SslAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.ClientHttpRequestFactorySettings;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.boot.util.LambdaSafe;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.http.client.ClientHttpRequestFactory;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for
 * {@link ClientHttpRequestFactoryBuilder} and {@link ClientHttpRequestFactorySettings}.
 *
 * @author Phillip Webb
 * @since 4.0.0
 */
@SuppressWarnings("removal")
@AutoConfiguration(after = SslAutoConfiguration.class)
@ConditionalOnClass(ClientHttpRequestFactory.class)
@Conditional(NotReactiveWebApplicationCondition.class)
@EnableConfigurationProperties(HttpClientProperties.class)
public class HttpClientAutoConfiguration implements BeanClassLoaderAware {

	private final ClientHttpRequestFactories factories;

	private ClassLoader beanClassLoader;

	HttpClientAutoConfiguration(ObjectProvider<SslBundles> sslBundles, HttpClientProperties properties) {
		this.factories = new ClientHttpRequestFactories(sslBundles, properties);
	}

	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		this.beanClassLoader = classLoader;
	}

	@Bean
	@ConditionalOnMissingBean
	ClientHttpRequestFactoryBuilder<?> clientHttpRequestFactoryBuilder(
			ObjectProvider<ClientHttpRequestFactoryBuilderCustomizer<?>> clientHttpRequestFactoryBuilderCustomizers) {
		ClientHttpRequestFactoryBuilder<?> builder = this.factories.builder(this.beanClassLoader);
		return customize(builder, clientHttpRequestFactoryBuilderCustomizers.orderedStream().toList());
	}

	@SuppressWarnings("unchecked")
	private ClientHttpRequestFactoryBuilder<?> customize(ClientHttpRequestFactoryBuilder<?> builder,
			List<ClientHttpRequestFactoryBuilderCustomizer<?>> customizers) {
		ClientHttpRequestFactoryBuilder<?>[] builderReference = { builder };
		LambdaSafe.callbacks(ClientHttpRequestFactoryBuilderCustomizer.class, customizers, builderReference[0])
			.invoke((customizer) -> builderReference[0] = customizer.customize(builderReference[0]));
		return builderReference[0];
	}

	@Bean
	@ConditionalOnMissingBean
	ClientHttpRequestFactorySettings clientHttpRequestFactorySettings() {
		return this.factories.settings();
	}

}
