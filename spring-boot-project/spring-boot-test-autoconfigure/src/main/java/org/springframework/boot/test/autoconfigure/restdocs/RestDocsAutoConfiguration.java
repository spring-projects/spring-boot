/*
 * Copyright 2012-2019 the original author or authors.
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
import org.springframework.restdocs.restassured3.RestAssuredRestDocumentation;
import org.springframework.restdocs.restassured3.RestAssuredRestDocumentationConfigurer;
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
@Configuration(proxyBeanMethods = false)
@ConditionalOnWebApplication
public class RestDocsAutoConfiguration {

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(MockMvcRestDocumentation.class)
	@ConditionalOnWebApplication(type = Type.SERVLET)
	@EnableConfigurationProperties(RestDocsProperties.class)
	static class RestDocsMockMvcConfiguration {

		@Bean
		@ConditionalOnMissingBean
		public MockMvcRestDocumentationConfigurer restDocsMockMvcConfigurer(
				ObjectProvider<RestDocsMockMvcConfigurationCustomizer> configurationCustomizers,
				RestDocumentationContextProvider contextProvider) {
			MockMvcRestDocumentationConfigurer configurer = MockMvcRestDocumentation
					.documentationConfiguration(contextProvider);
			configurationCustomizers.orderedStream()
					.forEach((configurationCustomizer) -> configurationCustomizer.customize(configurer));
			return configurer;
		}

		@Bean
		public RestDocsMockMvcBuilderCustomizer restDocumentationConfigurer(RestDocsProperties properties,
				MockMvcRestDocumentationConfigurer configurer,
				ObjectProvider<RestDocumentationResultHandler> resultHandler) {
			return new RestDocsMockMvcBuilderCustomizer(properties, configurer, resultHandler.getIfAvailable());
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass({ RequestSpecification.class, RestAssuredRestDocumentation.class })
	@EnableConfigurationProperties(RestDocsProperties.class)
	static class RestDocsRestAssuredConfiguration {

		@Bean
		@ConditionalOnMissingBean
		public RequestSpecification restDocsRestAssuredConfigurer(
				ObjectProvider<RestDocsRestAssuredConfigurationCustomizer> configurationCustomizers,
				RestDocumentationContextProvider contextProvider) {
			RestAssuredRestDocumentationConfigurer configurer = RestAssuredRestDocumentation
					.documentationConfiguration(contextProvider);
			configurationCustomizers.orderedStream()
					.forEach((configurationCustomizer) -> configurationCustomizer.customize(configurer));
			return new RequestSpecBuilder().addFilter(configurer).build();
		}

		@Bean
		public RestDocsRestAssuredBuilderCustomizer restAssuredBuilderCustomizer(RestDocsProperties properties,
				RequestSpecification configurer) {
			return new RestDocsRestAssuredBuilderCustomizer(properties, configurer);
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(WebTestClientRestDocumentation.class)
	@ConditionalOnWebApplication(type = Type.REACTIVE)
	@EnableConfigurationProperties(RestDocsProperties.class)
	static class RestDocsWebTestClientConfiguration {

		@Bean
		@ConditionalOnMissingBean
		public WebTestClientRestDocumentationConfigurer restDocsWebTestClientConfigurer(
				ObjectProvider<RestDocsWebTestClientConfigurationCustomizer> configurationCustomizers,
				RestDocumentationContextProvider contextProvider) {
			WebTestClientRestDocumentationConfigurer configurer = WebTestClientRestDocumentation
					.documentationConfiguration(contextProvider);
			configurationCustomizers.orderedStream()
					.forEach((configurationCustomizer) -> configurationCustomizer.customize(configurer));
			return configurer;
		}

		@Bean
		public RestDocsWebTestClientBuilderCustomizer restDocumentationConfigurer(RestDocsProperties properties,
				WebTestClientRestDocumentationConfigurer configurer) {
			return new RestDocsWebTestClientBuilderCustomizer(properties, configurer);
		}

	}

}
