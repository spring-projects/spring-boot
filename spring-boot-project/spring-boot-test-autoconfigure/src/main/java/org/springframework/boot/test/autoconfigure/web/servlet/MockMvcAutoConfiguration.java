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

package org.springframework.boot.test.autoconfigure.web.servlet;

import java.util.List;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.autoconfigure.web.servlet.DispatcherServletPath;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.reactive.WebTestClientAutoConfiguration;
import org.springframework.boot.test.web.reactive.server.WebTestClientBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.web.servlet.DispatcherServletCustomizer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MockMvcBuilder;
import org.springframework.test.web.servlet.client.MockMvcWebTestClient;
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.DispatcherServlet;

/**
 * Auto-configuration for {@link MockMvc}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 * @author Brian Clozel
 * @since 1.4.0
 * @see AutoConfigureWebMvc
 */
@AutoConfiguration(after = { WebMvcAutoConfiguration.class, WebTestClientAutoConfiguration.class })
@ConditionalOnWebApplication(type = Type.SERVLET)
@EnableConfigurationProperties({ ServerProperties.class, WebMvcProperties.class })
public class MockMvcAutoConfiguration {

	private final WebApplicationContext context;

	private final WebMvcProperties webMvcProperties;

	/**
	 * Constructs a new MockMvcAutoConfiguration with the specified WebApplicationContext
	 * and WebMvcProperties.
	 * @param context the WebApplicationContext to be used by the configuration
	 * @param webMvcProperties the WebMvcProperties to be used by the configuration
	 */
	MockMvcAutoConfiguration(WebApplicationContext context, WebMvcProperties webMvcProperties) {
		this.context = context;
		this.webMvcProperties = webMvcProperties;
	}

	/**
	 * Returns the DispatcherServlet path.
	 *
	 * This method is annotated with @Bean and @ConditionalOnMissingBean, indicating that
	 * it should be used as a bean if no other bean of the same type is present.
	 *
	 * The returned DispatcherServletPath is a functional interface that provides a method
	 * to get the path of the DispatcherServlet.
	 *
	 * The path is obtained from the webMvcProperties, which is a property object that
	 * holds the configuration properties for the web MVC framework.
	 * @return the DispatcherServlet path
	 */
	@Bean
	@ConditionalOnMissingBean
	public DispatcherServletPath dispatcherServletPath() {
		return () -> this.webMvcProperties.getServlet().getPath();
	}

	/**
	 * Creates a default MockMvcBuilder if no other MockMvcBuilder bean is present.
	 * @param customizers the list of MockMvcBuilderCustomizer beans
	 * @return the default MockMvcBuilder
	 */
	@Bean
	@ConditionalOnMissingBean(MockMvcBuilder.class)
	public DefaultMockMvcBuilder mockMvcBuilder(List<MockMvcBuilderCustomizer> customizers) {
		DefaultMockMvcBuilder builder = MockMvcBuilders.webAppContextSetup(this.context);
		builder.addDispatcherServletCustomizer(new MockMvcDispatcherServletCustomizer(this.webMvcProperties));
		for (MockMvcBuilderCustomizer customizer : customizers) {
			customizer.customize(builder);
		}
		return builder;
	}

	/**
	 * Returns a SpringBootMockMvcBuilderCustomizer bean that customizes the MockMvc
	 * builder for the Spring Test MockMvc configuration. The properties for customization
	 * are prefixed with "spring.test.mockmvc".
	 * @return the SpringBootMockMvcBuilderCustomizer bean
	 */
	@Bean
	@ConfigurationProperties(prefix = "spring.test.mockmvc")
	public SpringBootMockMvcBuilderCustomizer springBootMockMvcBuilderCustomizer() {
		return new SpringBootMockMvcBuilderCustomizer(this.context);
	}

	/**
	 * Creates a MockMvc bean if no other bean of type MockMvc is present.
	 * @param builder the MockMvcBuilder used to build the MockMvc instance
	 * @return the created MockMvc instance
	 */
	@Bean
	@ConditionalOnMissingBean
	public MockMvc mockMvc(MockMvcBuilder builder) {
		return builder.build();
	}

	/**
	 * Creates a DispatcherServlet bean if no other bean of the same type is present.
	 * @param mockMvc the MockMvc instance used to obtain the DispatcherServlet
	 * @return the DispatcherServlet bean
	 */
	@Bean
	@ConditionalOnMissingBean
	public DispatcherServlet dispatcherServlet(MockMvc mockMvc) {
		return mockMvc.getDispatcherServlet();
	}

	/**
	 * WebTestClientMockMvcConfiguration class.
	 */
	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass({ WebClient.class, WebTestClient.class })
	static class WebTestClientMockMvcConfiguration {

		/**
		 * Creates a WebTestClient instance using the provided MockMvc instance and a list
		 * of customizers. If no customizers are provided, a default WebTestClient.Builder
		 * is used.
		 * @param mockMvc the MockMvc instance to bind the WebTestClient to
		 * @param customizers a list of customizers to apply to the WebTestClient.Builder
		 * @return a WebTestClient instance
		 */
		@Bean
		@ConditionalOnMissingBean
		WebTestClient webTestClient(MockMvc mockMvc, List<WebTestClientBuilderCustomizer> customizers) {
			WebTestClient.Builder builder = MockMvcWebTestClient.bindTo(mockMvc);
			for (WebTestClientBuilderCustomizer customizer : customizers) {
				customizer.customize(builder);
			}
			return builder.build();
		}

	}

	/**
	 * MockMvcDispatcherServletCustomizer class.
	 */
	private static class MockMvcDispatcherServletCustomizer implements DispatcherServletCustomizer {

		private final WebMvcProperties webMvcProperties;

		/**
		 * Constructs a new MockMvcDispatcherServletCustomizer with the specified
		 * WebMvcProperties.
		 * @param webMvcProperties the WebMvcProperties to be used by the
		 * MockMvcDispatcherServletCustomizer
		 */
		MockMvcDispatcherServletCustomizer(WebMvcProperties webMvcProperties) {
			this.webMvcProperties = webMvcProperties;
		}

		/**
		 * Customize the DispatcherServlet with the provided configuration properties.
		 * @param dispatcherServlet the DispatcherServlet to customize
		 */
		@Override
		public void customize(DispatcherServlet dispatcherServlet) {
			dispatcherServlet.setDispatchOptionsRequest(this.webMvcProperties.isDispatchOptionsRequest());
			dispatcherServlet.setDispatchTraceRequest(this.webMvcProperties.isDispatchTraceRequest());
			configureThrowExceptionIfNoHandlerFound(dispatcherServlet);
		}

		/**
		 * Configures whether an exception should be thrown if no handler is found for a
		 * request.
		 * @param dispatcherServlet the DispatcherServlet instance
		 * @deprecated This method is deprecated and may be removed in future versions.
		 * @removal This method is marked for removal and will be removed in future
		 * versions.
		 */
		@SuppressWarnings({ "deprecation", "removal" })
		private void configureThrowExceptionIfNoHandlerFound(DispatcherServlet dispatcherServlet) {
			dispatcherServlet
				.setThrowExceptionIfNoHandlerFound(this.webMvcProperties.isThrowExceptionIfNoHandlerFound());
		}

	}

}
