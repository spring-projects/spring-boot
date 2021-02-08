/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.autoconfigure.hateoas;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.hateoas.HypermediaAutoConfiguration.HypermediaConfiguration;
import org.springframework.boot.autoconfigure.http.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.client.LinkDiscoverer;
import org.springframework.hateoas.client.LinkDiscoverers;
import org.springframework.hateoas.config.EnableHypermediaSupport;
import org.springframework.hateoas.config.EnableHypermediaSupport.HypermediaType;
import org.springframework.hateoas.mediatype.hal.HalLinkDiscoverer;
import org.springframework.hateoas.server.EntityLinks;
import org.springframework.hateoas.server.mvc.TypeConstrainedMappingJackson2HttpMessageConverter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link HypermediaAutoConfiguration}.
 *
 * @author Roy Clarkson
 * @author Oliver Gierke
 * @author Andy Wilkinson
 * @author Madhura Bhave
 */
class HypermediaAutoConfigurationTests {

	private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
			.withUserConfiguration(BaseConfig.class);

	@Test
	void autoConfigurationWhenSpringMvcNotOnClasspathShouldBackOff() {
		this.contextRunner.withClassLoader(new FilteredClassLoader(RequestMappingHandlerAdapter.class))
				.run((context) -> assertThat(context.getBeansOfType(HypermediaConfiguration.class)).isEmpty());
	}

	@Test
	void linkDiscoverersCreated() {
		this.contextRunner.run((context) -> {
			LinkDiscoverers discoverers = context.getBean(LinkDiscoverers.class);
			assertThat(discoverers).isNotNull();
			Optional<LinkDiscoverer> discoverer = discoverers.getLinkDiscovererFor(MediaTypes.HAL_JSON);
			assertThat(discoverer).containsInstanceOf(HalLinkDiscoverer.class);
		});
	}

	@Test
	void entityLinksCreated() {
		this.contextRunner.run((context) -> {
			EntityLinks discoverers = context.getBean(EntityLinks.class);
			assertThat(discoverers).isNotNull();
		});
	}

	@Test
	void doesBackOffIfEnableHypermediaSupportIsDeclaredManually() {
		this.contextRunner.withUserConfiguration(EnableHypermediaSupportConfig.class)
				.withPropertyValues("spring.jackson.serialization.INDENT_OUTPUT:true")
				.run((context) -> assertThat(context.getBeansOfType(HypermediaConfiguration.class)).isEmpty());
	}

	@Test
	void supportedMediaTypesOfTypeConstrainedConvertersIsCustomized() {
		this.contextRunner.run((context) -> {
			RequestMappingHandlerAdapter handlerAdapter = context.getBean(RequestMappingHandlerAdapter.class);
			for (HttpMessageConverter<?> converter : handlerAdapter.getMessageConverters()) {
				if (converter instanceof TypeConstrainedMappingJackson2HttpMessageConverter) {
					assertThat(converter.getSupportedMediaTypes()).contains(MediaType.APPLICATION_JSON,
							MediaTypes.HAL_JSON);
				}
			}
		});
	}

	@Test
	void customizationOfSupportedMediaTypesCanBeDisabled() {
		this.contextRunner.withPropertyValues("spring.hateoas.use-hal-as-default-json-media-type:false")
				.run((context) -> {
					RequestMappingHandlerAdapter handlerAdapter = context.getBean(RequestMappingHandlerAdapter.class);
					for (HttpMessageConverter<?> converter : handlerAdapter.getMessageConverters()) {
						if (converter instanceof TypeConstrainedMappingJackson2HttpMessageConverter) {
							assertThat(converter.getSupportedMediaTypes()).containsExactly(MediaTypes.HAL_JSON);
						}
					}
				});
	}

	@ImportAutoConfiguration({ HttpMessageConvertersAutoConfiguration.class, WebMvcAutoConfiguration.class,
			JacksonAutoConfiguration.class, HypermediaAutoConfiguration.class })
	static class BaseConfig {

	}

	@Configuration(proxyBeanMethods = false)
	@EnableHypermediaSupport(type = HypermediaType.HAL)
	static class EnableHypermediaSupportConfig {

	}

}
