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

package org.springframework.boot.context.properties;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.springframework.boot.context.properties.bind.AbstractBindHandler;
import org.springframework.boot.context.properties.bind.BindContext;
import org.springframework.boot.context.properties.bind.BindHandler;
import org.springframework.boot.context.properties.bind.BindResult;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.source.ConfigurationPropertyName;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.support.TestPropertySourceUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ConfigurationPropertiesBindHandlerAdvisor}.
 *
 * @author Phillip Webb
 */
class ConfigurationPropertiesBindHandlerAdvisorTests {

	private AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();

	@AfterEach
	void cleanup() {
		this.context.close();
	}

	@Test
	void loadWithoutConfigurationPropertiesBindHandlerAdvisor() {
		load(WithoutConfigurationPropertiesBindHandlerAdvisor.class, "foo.bar.default.content-type=text/plain",
				"foo.bar.bindings.input.destination=d1", "foo.bar.bindings.input.content-type=text/xml",
				"foo.bar.bindings.output.destination=d2");
		BindingServiceProperties properties = this.context.getBean(BindingServiceProperties.class);
		BindingProperties input = properties.getBindings().get("input");
		assertThat(input.getDestination()).isEqualTo("d1");
		assertThat(input.getContentType()).isEqualTo("text/xml");
		BindingProperties output = properties.getBindings().get("output");
		assertThat(output.getDestination()).isEqualTo("d2");
		assertThat(output.getContentType()).isEqualTo("application/json");
	}

	@Test
	void loadWithConfigurationPropertiesBindHandlerAdvisor() {
		load(WithConfigurationPropertiesBindHandlerAdvisor.class, "foo.bar.default.content-type=text/plain",
				"foo.bar.bindings.input.destination=d1", "foo.bar.bindings.input.content-type=text/xml",
				"foo.bar.bindings.output.destination=d2");
		BindingServiceProperties properties = this.context.getBean(BindingServiceProperties.class);
		BindingProperties input = properties.getBindings().get("input");
		assertThat(input.getDestination()).isEqualTo("d1");
		assertThat(input.getContentType()).isEqualTo("text/xml");
		BindingProperties output = properties.getBindings().get("output");
		assertThat(output.getDestination()).isEqualTo("d2");
		assertThat(output.getContentType()).isEqualTo("text/plain");
	}

	private AnnotationConfigApplicationContext load(Class<?> configuration, String... inlinedProperties) {
		return load(new Class<?>[] { configuration }, inlinedProperties);
	}

	private AnnotationConfigApplicationContext load(Class<?>[] configuration, String... inlinedProperties) {
		this.context.register(configuration);
		TestPropertySourceUtils.addInlinedPropertiesToEnvironment(this.context, inlinedProperties);
		this.context.refresh();
		return this.context;
	}

	@Configuration(proxyBeanMethods = false)
	@EnableConfigurationProperties(BindingServiceProperties.class)
	static class WithoutConfigurationPropertiesBindHandlerAdvisor {

	}

	@Configuration(proxyBeanMethods = false)
	@EnableConfigurationProperties(BindingServiceProperties.class)
	@Import(DefaultValuesConfigurationPropertiesBindHandlerAdvisor.class)
	static class WithConfigurationPropertiesBindHandlerAdvisor {

	}

	static class DefaultValuesConfigurationPropertiesBindHandlerAdvisor
			implements ConfigurationPropertiesBindHandlerAdvisor {

		@Override
		public BindHandler apply(BindHandler bindHandler) {
			return new DefaultValuesBindHandler(bindHandler);
		}

	}

	static class DefaultValuesBindHandler extends AbstractBindHandler {

		private final Map<ConfigurationPropertyName, ConfigurationPropertyName> mappings;

		DefaultValuesBindHandler(BindHandler bindHandler) {
			super(bindHandler);
			this.mappings = new LinkedHashMap<>();
			this.mappings.put(ConfigurationPropertyName.of("foo.bar.bindings"),
					ConfigurationPropertyName.of("foo.bar.default"));
		}

		@Override
		public <T> Bindable<T> onStart(ConfigurationPropertyName name, Bindable<T> target, BindContext context) {
			ConfigurationPropertyName defaultName = getDefaultName(name);
			if (defaultName != null) {
				BindResult<T> result = context.getBinder().bind(defaultName, target);
				if (result.isBound()) {
					return target.withExistingValue(result.get());
				}
			}
			return super.onStart(name, target, context);
		}

		private ConfigurationPropertyName getDefaultName(ConfigurationPropertyName name) {
			for (Map.Entry<ConfigurationPropertyName, ConfigurationPropertyName> mapping : this.mappings.entrySet()) {
				ConfigurationPropertyName from = mapping.getKey();
				ConfigurationPropertyName to = mapping.getValue();
				if (name.getNumberOfElements() == from.getNumberOfElements() + 1 && from.isParentOf(name)) {
					return to;
				}
			}
			return null;
		}

	}

	@ConfigurationProperties("foo.bar")
	static class BindingServiceProperties {

		private Map<String, BindingProperties> bindings = new TreeMap<>();

		Map<String, BindingProperties> getBindings() {
			return this.bindings;
		}

	}

	static class BindingProperties {

		private String destination;

		private String contentType = "application/json";

		String getDestination() {
			return this.destination;
		}

		void setDestination(String destination) {
			this.destination = destination;
		}

		String getContentType() {
			return this.contentType;
		}

		void setContentType(String contentType) {
			this.contentType = contentType;
		}

	}

}
