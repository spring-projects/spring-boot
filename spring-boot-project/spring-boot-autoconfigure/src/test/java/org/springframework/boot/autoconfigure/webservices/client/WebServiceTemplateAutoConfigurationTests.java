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

package org.springframework.boot.autoconfigure.webservices.client;

import java.util.function.Consumer;

import org.junit.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.ContextConsumer;
import org.springframework.boot.webservices.client.WebServiceTemplateBuilder;
import org.springframework.boot.webservices.client.WebServiceTemplateCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.oxm.Marshaller;
import org.springframework.oxm.Unmarshaller;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.ws.transport.WebServiceMessageSender;
import org.springframework.ws.transport.http.ClientHttpRequestMessageSender;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link WebServiceTemplateAutoConfiguration}.
 *
 * @author Stephane Nicoll
 * @author Dmytro Nosan
 */
public class WebServiceTemplateAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(
					AutoConfigurations.of(WebServiceTemplateAutoConfiguration.class));

	@Test
	public void autoConfiguredBuilderShouldNotHaveMarshallerAndUnmarshaller() {
		this.contextRunner.run(assertWebServiceTemplateBuilder((builder) -> {
			WebServiceTemplate webServiceTemplate = builder.build();
			assertThat(webServiceTemplate.getUnmarshaller()).isNull();
			assertThat(webServiceTemplate.getMarshaller()).isNull();
		}));
	}

	@Test
	public void autoConfiguredBuilderShouldHaveHttpMessageSenderByDefault() {
		this.contextRunner.run(assertWebServiceTemplateBuilder((builder) -> {
			WebServiceTemplate webServiceTemplate = builder.build();
			assertThat(webServiceTemplate.getMessageSenders()).hasSize(1);
			WebServiceMessageSender messageSender = webServiceTemplate
					.getMessageSenders()[0];
			assertThat(messageSender).isInstanceOf(ClientHttpRequestMessageSender.class);
		}));
	}

	@Test
	public void webServiceTemplateWhenHasCustomBuilderShouldUseCustomBuilder() {
		this.contextRunner
				.withUserConfiguration(CustomWebServiceTemplateBuilderConfig.class)
				.run(assertWebServiceTemplateBuilder((builder) -> {
					WebServiceTemplate webServiceTemplate = builder.build();
					assertThat(webServiceTemplate.getMarshaller())
							.isSameAs(CustomWebServiceTemplateBuilderConfig.marshaller);
				}));
	}

	@Test
	public void webServiceTemplateShouldApplyCustomizer() {
		this.contextRunner.withUserConfiguration(WebServiceTemplateCustomizerConfig.class)
				.run(assertWebServiceTemplateBuilder((builder) -> {
					WebServiceTemplate webServiceTemplate = builder.build();
					assertThat(webServiceTemplate.getUnmarshaller())
							.isSameAs(WebServiceTemplateCustomizerConfig.unmarshaller);
				}));
	}

	@Test
	public void builderShouldBeFreshForEachUse() {
		this.contextRunner.withUserConfiguration(DirtyWebServiceTemplateConfig.class)
				.run((context) -> assertThat(context).hasNotFailed());
	}

	private ContextConsumer<AssertableApplicationContext> assertWebServiceTemplateBuilder(
			Consumer<WebServiceTemplateBuilder> builder) {
		return (context) -> {
			assertThat(context).hasSingleBean(WebServiceTemplateBuilder.class);
			builder.accept(context.getBean(WebServiceTemplateBuilder.class));
		};
	}

	@Configuration(proxyBeanMethods = false)
	static class DirtyWebServiceTemplateConfig {

		@Bean
		public WebServiceTemplate webServiceTemplateOne(
				WebServiceTemplateBuilder builder) {
			try {
				return builder.build();
			}
			finally {
				breakBuilderOnNextCall(builder);
			}
		}

		@Bean
		public WebServiceTemplate webServiceTemplateTwo(
				WebServiceTemplateBuilder builder) {
			try {
				return builder.build();
			}
			finally {
				breakBuilderOnNextCall(builder);
			}
		}

		private void breakBuilderOnNextCall(WebServiceTemplateBuilder builder) {
			builder.additionalCustomizers((webServiceTemplate) -> {
				throw new IllegalStateException();
			});
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class CustomWebServiceTemplateBuilderConfig {

		private static final Marshaller marshaller = new Jaxb2Marshaller();

		@Bean
		public WebServiceTemplateBuilder webServiceTemplateBuilder() {
			return new WebServiceTemplateBuilder().setMarshaller(marshaller);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class WebServiceTemplateCustomizerConfig {

		private static final Unmarshaller unmarshaller = new Jaxb2Marshaller();

		@Bean
		public WebServiceTemplateCustomizer webServiceTemplateCustomizer() {
			return (ws) -> ws.setUnmarshaller(unmarshaller);
		}

	}

}
