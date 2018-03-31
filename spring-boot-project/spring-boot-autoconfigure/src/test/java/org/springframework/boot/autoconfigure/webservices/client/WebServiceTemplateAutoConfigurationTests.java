/*
 * Copyright 2012-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.autoconfigure.webservices.client;

import org.junit.After;
import org.junit.Test;

import org.springframework.boot.webservices.client.WebServiceTemplateBuilder;
import org.springframework.boot.webservices.client.WebServiceTemplateCustomizer;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.ws.client.core.WebServiceTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link WebServiceTemplateAutoConfiguration
 * WebServiceTemplateAutoConfiguration}.
 *
 * @author Dmytro Nosan
 */
public class WebServiceTemplateAutoConfigurationTests {

	private AnnotationConfigApplicationContext context;

	@After
	public void close() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void webServiceTemplateShouldNotHaveMarshallerAndUnmarshaller() {
		load(WebServiceTemplateConfig.class);
		WebServiceTemplate webServiceTemplate = this.context
				.getBean(WebServiceTemplate.class);
		assertThat(webServiceTemplate.getUnmarshaller()).isNull();
		assertThat(webServiceTemplate.getMarshaller()).isNull();
	}

	@Test
	public void webServiceTemplateShouldUserCustomBuilder() {
		load(CustomWebServiceTemplateBuilderConfig.class, WebServiceTemplateConfig.class);
		WebServiceTemplate webServiceTemplate = this.context
				.getBean(WebServiceTemplate.class);
		assertThat(webServiceTemplate.getMarshaller()).isNotNull();
	}

	@Test
	public void webServiceTemplateShouldApplyCustomizer() {
		load(WebServiceTemplateCustomizerConfig.class, WebServiceTemplateConfig.class);
		WebServiceTemplate webServiceTemplate = this.context
				.getBean(WebServiceTemplate.class);
		assertThat(webServiceTemplate.getUnmarshaller()).isNotNull();
	}

	@Test
	public void builderShouldBeFreshForEachUse() {
		load(DirtyWebServiceTemplateConfig.class);
	}

	private void load(Class<?>... config) {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(config);
		ctx.register(WebServiceTemplateAutoConfiguration.class);
		ctx.refresh();
		this.context = ctx;
	}

	@Configuration
	static class WebServiceTemplateConfig {

		@Bean
		public WebServiceTemplate webServiceTemplate(WebServiceTemplateBuilder builder) {
			return builder.build();
		}

	}

	@Configuration
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
			builder.addCustomizers((webServiceTemplate) -> {
				throw new IllegalStateException();
			});
		}

	}

	@Configuration
	static class CustomWebServiceTemplateBuilderConfig {

		@Bean
		public WebServiceTemplateBuilder webServiceTemplateBuilder() {
			return new WebServiceTemplateBuilder().setMarshaller(new Jaxb2Marshaller());
		}

	}

	@Configuration
	static class WebServiceTemplateCustomizerConfig {

		@Bean
		public WebServiceTemplateCustomizer webServiceTemplateCustomizer() {
			return (ws) -> ws.setUnmarshaller(new Jaxb2Marshaller());
		}

	}

}
