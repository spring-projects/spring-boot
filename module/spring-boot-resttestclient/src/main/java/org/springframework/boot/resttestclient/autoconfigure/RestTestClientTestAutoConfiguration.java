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

package org.springframework.boot.resttestclient.autoconfigure;

import java.util.List;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.http.converter.autoconfigure.ClientHttpMessageConvertersCustomizer;
import org.springframework.boot.test.http.server.LocalTestWebServer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.web.client.RestClient;
import org.springframework.web.context.WebApplicationContext;

/**
 * Test auto-configuration for {@link RestTestClient}.
 *
 * @author Stephane Nicoll
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @see AutoConfigureRestTestClient
 */
@AutoConfiguration
@ConditionalOnClass({ RestClient.class, RestTestClient.class, ClientHttpMessageConvertersCustomizer.class })
final class RestTestClientTestAutoConfiguration {

	@Bean
	SpringBootRestTestClientBuilderCustomizer springBootRestTestClientBuilderCustomizer(
			ObjectProvider<ClientHttpMessageConvertersCustomizer> httpMessageConverterCustomizers) {
		return new SpringBootRestTestClientBuilderCustomizer(httpMessageConverterCustomizers.orderedStream().toList());
	}

	@Bean
	@ConditionalOnMissingBean
	RestTestClient restTestClient(WebApplicationContext applicationContext,
			List<RestTestClientBuilderCustomizer> customizers) {
		RestTestClient.Builder<?> builder = getBuilder(applicationContext);
		customizers.forEach((customizer) -> customizer.customize(builder));
		return builder.build();
	}

	private RestTestClient.Builder<?> getBuilder(WebApplicationContext applicationContext) {
		LocalTestWebServer localTestWebServer = LocalTestWebServer.get(applicationContext);
		if (localTestWebServer != null) {
			return RestTestClient.bindToServer().uriBuilderFactory(localTestWebServer.uriBuilderFactory());
		}
		if (hasBean(applicationContext, MockMvc.class)) {
			return RestTestClient.bindTo(applicationContext.getBean(MockMvc.class));
		}
		return RestTestClient.bindToApplicationContext(applicationContext);
	}

	private boolean hasBean(ApplicationContext applicationContext, Class<?> type) {
		try {
			applicationContext.getBean(type);
			return true;
		}
		catch (NoSuchBeanDefinitionException ex) {
			return false;
		}
	}

}
