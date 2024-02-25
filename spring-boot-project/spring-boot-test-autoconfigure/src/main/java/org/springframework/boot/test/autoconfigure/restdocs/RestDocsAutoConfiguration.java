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

package org.springframework.boot.test.autoconfigure.restdocs;

import io.restassured.builder.RequestSpecBuilder;
import io.restassured.specification.RequestSpecification;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation;
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentationConfigurer;
import org.springframework.restdocs.mockmvc.RestDocumentationResultHandler;
import org.springframework.restdocs.restassured.RestAssuredRestDocumentation;
import org.springframework.restdocs.restassured.RestAssuredRestDocumentationConfigurer;
import org.springframework.restdocs.webtestclient.WebTestClientRestDocumentation;
import org.springframework.restdocs.webtestclient.WebTestClientRestDocumentationConfigurer;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Spring REST Docs.
 *
 * @author Andy Wilkinson
 * @author Eddú Meléndez
 * @author Roman Zaynetdinov
 * @since 1.4.0
 */
@AutoConfiguration
@ConditionalOnWebApplication
public class RestDocsAutoConfiguration {

	/**
	 * RestDocsMockMvcConfiguration class.
	 */
	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(MockMvcRestDocumentation.class)
	@ConditionalOnWebApplication(type = Type.SERVLET)
	@EnableConfigurationProperties(RestDocsProperties.class)
	static class RestDocsMockMvcConfiguration {

		/**
		 * Creates and configures a {@link MockMvcRestDocumentationConfigurer} bean if one
		 * is not already present. This bean is responsible for configuring the MockMvc
		 * instance used for generating REST documentation.
		 * @param configurationCustomizers An {@link ObjectProvider} of
		 * {@link RestDocsMockMvcConfigurationCustomizer} instances that can be used to
		 * customize the configuration of the MockMvc instance.
		 * @param contextProvider The {@link RestDocumentationContextProvider} used for
		 * creating the documentation context.
		 * @return The configured {@link MockMvcRestDocumentationConfigurer} bean.
		 */
		@Bean
		@ConditionalOnMissingBean
		MockMvcRestDocumentationConfigurer restDocsMockMvcConfigurer(
				ObjectProvider<RestDocsMockMvcConfigurationCustomizer> configurationCustomizers,
				RestDocumentationContextProvider contextProvider) {
			MockMvcRestDocumentationConfigurer configurer = MockMvcRestDocumentation
				.documentationConfiguration(contextProvider);
			configurationCustomizers.orderedStream()
				.forEach((configurationCustomizer) -> configurationCustomizer.customize(configurer));
			return configurer;
		}

		/**
		 * Returns a RestDocsMockMvcBuilderCustomizer bean that configures the
		 * RestDocsProperties, MockMvcRestDocumentationConfigurer, and
		 * RestDocumentationResultHandler for generating REST documentation.
		 * @param properties The RestDocsProperties object containing the configuration
		 * properties for REST documentation.
		 * @param configurer The MockMvcRestDocumentationConfigurer object used for
		 * configuring REST documentation.
		 * @param resultHandler The RestDocumentationResultHandler object used for
		 * handling REST documentation results.
		 * @return A RestDocsMockMvcBuilderCustomizer bean that configures the REST
		 * documentation.
		 */
		@Bean
		RestDocsMockMvcBuilderCustomizer restDocumentationConfigurer(RestDocsProperties properties,
				MockMvcRestDocumentationConfigurer configurer,
				ObjectProvider<RestDocumentationResultHandler> resultHandler) {
			return new RestDocsMockMvcBuilderCustomizer(properties, configurer, resultHandler.getIfAvailable());
		}

	}

	/**
	 * RestDocsRestAssuredConfiguration class.
	 */
	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass({ RequestSpecification.class, RestAssuredRestDocumentation.class })
	@EnableConfigurationProperties(RestDocsProperties.class)
	static class RestDocsRestAssuredConfiguration {

		/**
		 * Creates a RequestSpecification for RestDocsRestAssuredConfigurer.
		 * @param configurationCustomizers ObjectProvider of
		 * RestDocsRestAssuredConfigurationCustomizer
		 * @param contextProvider RestDocumentationContextProvider
		 * @return RequestSpecification for RestDocsRestAssuredConfigurer
		 */
		@Bean
		@ConditionalOnMissingBean
		RequestSpecification restDocsRestAssuredConfigurer(
				ObjectProvider<RestDocsRestAssuredConfigurationCustomizer> configurationCustomizers,
				RestDocumentationContextProvider contextProvider) {
			RestAssuredRestDocumentationConfigurer configurer = RestAssuredRestDocumentation
				.documentationConfiguration(contextProvider);
			configurationCustomizers.orderedStream()
				.forEach((configurationCustomizer) -> configurationCustomizer.customize(configurer));
			return new RequestSpecBuilder().addFilter(configurer).build();
		}

		/**
		 * Creates a customizer for the RestDocsRestAssuredBuilder.
		 * @param properties The RestDocsProperties object containing the configuration
		 * properties.
		 * @param configurer The RequestSpecification object used for configuring
		 * RestAssured.
		 * @return The RestDocsRestAssuredBuilderCustomizer object.
		 */
		@Bean
		RestDocsRestAssuredBuilderCustomizer restAssuredBuilderCustomizer(RestDocsProperties properties,
				RequestSpecification configurer) {
			return new RestDocsRestAssuredBuilderCustomizer(properties, configurer);
		}

	}

	/**
	 * RestDocsWebTestClientConfiguration class.
	 */
	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(WebTestClientRestDocumentation.class)
	@ConditionalOnWebApplication(type = Type.REACTIVE)
	@EnableConfigurationProperties(RestDocsProperties.class)
	static class RestDocsWebTestClientConfiguration {

		/**
		 * Creates a WebTestClientRestDocumentationConfigurer bean if no other bean of the
		 * same type is present. This configurer is responsible for configuring the
		 * WebTestClientRestDocumentation. It takes a list of
		 * RestDocsWebTestClientConfigurationCustomizer beans and a
		 * RestDocumentationContextProvider bean as dependencies. The configuration
		 * customizers are applied to the configurer in the order specified by their
		 * priority.
		 * @param configurationCustomizers The list of
		 * RestDocsWebTestClientConfigurationCustomizer beans that customize the
		 * configurer.
		 * @param contextProvider The RestDocumentationContextProvider bean that provides
		 * the context for the configurer.
		 * @return The WebTestClientRestDocumentationConfigurer bean.
		 */
		@Bean
		@ConditionalOnMissingBean
		WebTestClientRestDocumentationConfigurer restDocsWebTestClientConfigurer(
				ObjectProvider<RestDocsWebTestClientConfigurationCustomizer> configurationCustomizers,
				RestDocumentationContextProvider contextProvider) {
			WebTestClientRestDocumentationConfigurer configurer = WebTestClientRestDocumentation
				.documentationConfiguration(contextProvider);
			configurationCustomizers.orderedStream()
				.forEach((configurationCustomizer) -> configurationCustomizer.customize(configurer));
			return configurer;
		}

		/**
		 * Returns a RestDocsWebTestClientBuilderCustomizer bean that configures the
		 * RestDocsProperties and WebTestClientRestDocumentationConfigurer.
		 * @param properties The RestDocsProperties object containing the configuration
		 * properties for RestDocs.
		 * @param configurer The WebTestClientRestDocumentationConfigurer object used to
		 * configure the WebTestClient for RestDocs.
		 * @return A RestDocsWebTestClientBuilderCustomizer bean that configures the
		 * RestDocsProperties and WebTestClientRestDocumentationConfigurer.
		 */
		@Bean
		RestDocsWebTestClientBuilderCustomizer restDocumentationConfigurer(RestDocsProperties properties,
				WebTestClientRestDocumentationConfigurer configurer) {
			return new RestDocsWebTestClientBuilderCustomizer(properties, configurer);
		}

	}

}
