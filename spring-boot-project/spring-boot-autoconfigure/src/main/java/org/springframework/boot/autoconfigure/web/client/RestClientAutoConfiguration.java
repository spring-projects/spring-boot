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

package org.springframework.boot.autoconfigure.web.client;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.http.HttpMessageConverters;
import org.springframework.boot.autoconfigure.http.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.autoconfigure.ssl.SslAutoConfiguration;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Scope;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.client.RestClient;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for {@link RestClient}.
 * <p>
 * This will produce a {@link RestClient.Builder RestClient.Builder} bean with the
 * {@code prototype} scope, meaning each injection point will receive a newly cloned
 * instance of the builder.
 *
 * @author Arjen Poutsma
 * @author Moritz Halbritter
 * @since 3.2.0
 */
@AutoConfiguration(after = { HttpMessageConvertersAutoConfiguration.class, SslAutoConfiguration.class })
@ConditionalOnClass(RestClient.class)
@Conditional(NotReactiveWebApplicationCondition.class)
public class RestClientAutoConfiguration {

	/**
	 * Creates a {@link HttpMessageConvertersRestClientCustomizer} bean if no other bean
	 * of the same type is present. This customizer is responsible for customizing the
	 * {@link HttpMessageConverters} used by the REST client. The customizer is assigned
	 * the lowest precedence to ensure it runs after other customizers.
	 * @param messageConverters the provider for the {@link HttpMessageConverters} bean
	 * @return the {@link HttpMessageConvertersRestClientCustomizer} bean
	 */
	@Bean
	@ConditionalOnMissingBean
	@Order(Ordered.LOWEST_PRECEDENCE)
	HttpMessageConvertersRestClientCustomizer httpMessageConvertersRestClientCustomizer(
			ObjectProvider<HttpMessageConverters> messageConverters) {
		return new HttpMessageConvertersRestClientCustomizer(messageConverters.getIfUnique());
	}

	/**
	 * Creates an instance of AutoConfiguredRestClientSsl if RestClientSsl bean is missing
	 * and SslBundles bean is present.
	 * @param sslBundles the SslBundles bean used to configure the RestClientSsl
	 * @return an instance of AutoConfiguredRestClientSsl
	 */
	@Bean
	@ConditionalOnMissingBean(RestClientSsl.class)
	@ConditionalOnBean(SslBundles.class)
	AutoConfiguredRestClientSsl restClientSsl(SslBundles sslBundles) {
		return new AutoConfiguredRestClientSsl(sslBundles);
	}

	/**
	 * Creates a RestClientBuilderConfigurer bean if no other bean of the same type is
	 * present. This bean is responsible for configuring the RestClientBuilder with any
	 * customizations provided by RestClientCustomizer beans.
	 * @param customizerProvider ObjectProvider of RestClientCustomizer beans
	 * @return the RestClientBuilderConfigurer bean
	 */
	@Bean
	@ConditionalOnMissingBean
	RestClientBuilderConfigurer restClientBuilderConfigurer(ObjectProvider<RestClientCustomizer> customizerProvider) {
		RestClientBuilderConfigurer configurer = new RestClientBuilderConfigurer();
		configurer.setRestClientCustomizers(customizerProvider.orderedStream().toList());
		return configurer;
	}

	/**
	 * Creates a new instance of {@link RestClient.Builder} with the provided
	 * {@link RestClientBuilderConfigurer}. This method is annotated with {@link Bean} to
	 * indicate that it is a Spring bean. The scope of the bean is set to "prototype"
	 * using the {@link Scope} annotation. The {@link ConditionalOnMissingBean} annotation
	 * ensures that this bean is only created if there is no existing bean of the same
	 * type.
	 * @param restClientBuilderConfigurer the {@link RestClientBuilderConfigurer} used to
	 * configure the {@link RestClient.Builder}
	 * @return a new instance of {@link RestClient.Builder} configured with the provided
	 * {@link RestClientBuilderConfigurer}
	 */
	@Bean
	@Scope("prototype")
	@ConditionalOnMissingBean
	RestClient.Builder restClientBuilder(RestClientBuilderConfigurer restClientBuilderConfigurer) {
		RestClient.Builder builder = RestClient.builder()
			.requestFactory(ClientHttpRequestFactories.get(ClientHttpRequestFactorySettings.DEFAULTS));
		return restClientBuilderConfigurer.configure(builder);
	}

}
