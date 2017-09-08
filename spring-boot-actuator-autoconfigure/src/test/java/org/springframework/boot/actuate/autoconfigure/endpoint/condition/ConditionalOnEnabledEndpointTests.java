/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.endpoint.condition;

import org.junit.Test;

import org.springframework.boot.actuate.endpoint.DefaultEnablement;
import org.springframework.boot.actuate.endpoint.EndpointExposure;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.jmx.annotation.JmxEndpointExtension;
import org.springframework.boot.actuate.endpoint.web.annotation.WebEndpointExtension;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ConditionalOnEnabledEndpoint}.
 *
 * @author Stephane Nicoll
 * @author Andy Wilkinson
 */
public class ConditionalOnEnabledEndpointTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner();

	@Test
	public void enabledByDefault() {
		this.contextRunner.withUserConfiguration(FooConfig.class)
				.run((context) -> assertThat(context).hasBean("foo"));
	}

	@Test
	public void disabledViaSpecificProperty() {
		this.contextRunner.withUserConfiguration(FooConfig.class)
				.withPropertyValues("endpoints.foo.enabled=false")
				.run((context) -> assertThat(context).doesNotHaveBean("foo"));
	}

	@Test
	public void disabledViaGeneralProperty() {
		this.contextRunner.withUserConfiguration(FooConfig.class)
				.withPropertyValues("endpoints.default.enabled=false")
				.run((context) -> assertThat(context).doesNotHaveBean("foo"));
	}

	@Test
	public void enabledOverrideViaSpecificProperty() {
		this.contextRunner.withUserConfiguration(FooConfig.class)
				.withPropertyValues("endpoints.default.enabled=false",
						"endpoints.foo.enabled=true")
				.run((context) -> assertThat(context).hasBean("foo"));
	}

	@Test
	public void enabledOverrideViaSpecificWebProperty() {
		this.contextRunner.withUserConfiguration(FooConfig.class)
				.withPropertyValues("endpoints.foo.enabled=false",
						"endpoints.foo.web.enabled=true")
				.run((context) -> assertThat(context).hasBean("foo"));
	}

	@Test
	public void enabledOverrideViaSpecificJmxProperty() {
		this.contextRunner.withUserConfiguration(FooConfig.class)
				.withPropertyValues("endpoints.foo.enabled=false",
						"endpoints.foo.jmx.enabled=true")
				.run((context) -> assertThat(context).hasBean("foo"));
	}

	@Test
	public void enabledOverrideViaSpecificAnyProperty() {
		this.contextRunner.withUserConfiguration(FooConfig.class)
				.withPropertyValues("endpoints.foo.enabled=false",
						"endpoints.foo.web.enabled=false",
						"endpoints.foo.jmx.enabled=true")
				.run((context) -> assertThat(context).hasBean("foo"));
	}

	@Test
	public void enabledOverrideViaGeneralWebProperty() {
		this.contextRunner.withUserConfiguration(FooConfig.class)
				.withPropertyValues("endpoints.default.enabled=false",
						"endpoints.default.web.enabled=true")
				.run((context) -> assertThat(context).hasBean("foo"));
	}

	@Test
	public void enabledOverrideViaGeneralJmxProperty() {
		this.contextRunner.withUserConfiguration(FooConfig.class)
				.withPropertyValues("endpoints.default.enabled=false",
						"endpoints.default.jmx.enabled=true")
				.run((context) -> assertThat(context).hasBean("foo"));
	}

	@Test
	public void enabledOverrideViaGeneralAnyProperty() {
		this.contextRunner.withUserConfiguration(FooConfig.class)
				.withPropertyValues("endpoints.default.enabled=false",
						"endpoints.default.web.enabled=false",
						"endpoints.default.jmx.enabled=true")
				.run((context) -> assertThat(context).hasBean("foo"));
	}

	@Test
	public void disabledEvenWithEnabledGeneralProperties() {
		this.contextRunner.withUserConfiguration(FooConfig.class).withPropertyValues(
				"endpoints.default.enabled=true", "endpoints.default.web.enabled=true",
				"endpoints.default.jmx.enabled=true", "endpoints.foo.enabled=false")
				.run((context) -> assertThat(context).doesNotHaveBean("foo"));
	}

	@Test
	public void disabledByDefaultWithAnnotationFlag() {
		this.contextRunner.withUserConfiguration(BarConfig.class)
				.run((context) -> assertThat(context).doesNotHaveBean("bar"));
	}

	@Test
	public void disabledByDefaultWithAnnotationFlagEvenWithGeneralProperty() {
		this.contextRunner.withUserConfiguration(BarConfig.class)
				.withPropertyValues("endpoints.default.enabled=true")
				.run((context) -> assertThat(context).doesNotHaveBean("bar"));
	}

	@Test
	public void disabledByDefaultWithAnnotationFlagEvenWithGeneralWebProperty() {
		this.contextRunner.withUserConfiguration(BarConfig.class)
				.withPropertyValues("endpoints.default.web.enabled=true")
				.run((context) -> assertThat(context).doesNotHaveBean("bar"));
	}

	@Test
	public void disabledByDefaultWithAnnotationFlagEvenWithGeneralJmxProperty() {
		this.contextRunner.withUserConfiguration(BarConfig.class)
				.withPropertyValues("endpoints.default.jmx.enabled=true")
				.run((context) -> assertThat(context).doesNotHaveBean("bar"));
	}

	@Test
	public void enabledOverrideWithAndAnnotationFlagAndSpecificProperty() {
		this.contextRunner.withUserConfiguration(BarConfig.class)
				.withPropertyValues("endpoints.bar.enabled=true")
				.run((context) -> assertThat(context).hasBean("bar"));
	}

	@Test
	public void enabledOverrideWithAndAnnotationFlagAndSpecificWebProperty() {
		this.contextRunner.withUserConfiguration(BarConfig.class)
				.withPropertyValues("endpoints.bar.web.enabled=true")
				.run((context) -> assertThat(context).hasBean("bar"));
	}

	@Test
	public void enabledOverrideWithAndAnnotationFlagAndSpecificJmxProperty() {
		this.contextRunner.withUserConfiguration(BarConfig.class)
				.withPropertyValues("endpoints.bar.jmx.enabled=true")
				.run((context) -> assertThat(context).hasBean("bar"));
	}

	@Test
	public void enabledOverrideWithAndAnnotationFlagAndAnyProperty() {
		this.contextRunner.withUserConfiguration(BarConfig.class)
				.withPropertyValues("endpoints.bar.web.enabled=false",
						"endpoints.bar.jmx.enabled=true")
				.run((context) -> assertThat(context).hasBean("bar"));
	}

	@Test
	public void enabledOnlyWebByDefault() {
		this.contextRunner.withUserConfiguration(OnlyWebConfig.class)
				.withPropertyValues("endpoints.default.web.enabled=true")
				.run((context) -> assertThat(context).hasBean("onlyweb"));
	}

	@Test
	public void disabledOnlyWebViaEndpointProperty() {
		this.contextRunner.withUserConfiguration(OnlyWebConfig.class)
				.withPropertyValues("endpoints.onlyweb.enabled=false")
				.run((context) -> assertThat(context).doesNotHaveBean("onlyweb"));
	}

	@Test
	public void disabledOnlyWebViaSpecificTechProperty() {
		this.contextRunner.withUserConfiguration(OnlyWebConfig.class)
				.withPropertyValues("endpoints.onlyweb.web.enabled=false")
				.run((context) -> assertThat(context).doesNotHaveBean("onlyweb"));
	}

	@Test
	public void enableOverridesOnlyWebViaGeneralJmxPropertyHasNoEffect() {
		this.contextRunner.withUserConfiguration(OnlyWebConfig.class)
				.withPropertyValues("endpoints.default.enabled=false",
						"endpoints.default.jmx.enabled=true")
				.run((context) -> assertThat(context).doesNotHaveBean("onlyweb"));
	}

	@Test
	public void enableOverridesOnlyWebViaSpecificJmxPropertyHasNoEffect() {
		this.contextRunner.withUserConfiguration(OnlyWebConfig.class)
				.withPropertyValues("endpoints.default.enabled=false",
						"endpoints.onlyweb.jmx.enabled=false")
				.run((context) -> assertThat(context).doesNotHaveBean("onlyweb"));
	}

	@Test
	public void enableOverridesOnlyWebViaSpecificWebProperty() {
		this.contextRunner.withUserConfiguration(OnlyWebConfig.class)
				.withPropertyValues("endpoints.default.enabled=false",
						"endpoints.onlyweb.web.enabled=true")
				.run((context) -> assertThat(context).hasBean("onlyweb"));
	}

	@Test
	public void disabledOnlyWebEvenWithEnabledGeneralProperties() {
		this.contextRunner.withUserConfiguration(OnlyWebConfig.class).withPropertyValues(
				"endpoints.default.enabled=true", "endpoints.default.web.enabled=true",
				"endpoints.onlyweb.enabled=true", "endpoints.onlyweb.web.enabled=false")
				.run((context) -> assertThat(context).doesNotHaveBean("foo"));
	}

	@Test
	public void contextFailIfEndpointTypeIsNotDetected() {
		this.contextRunner.withUserConfiguration(NonEndpointBeanConfig.class)
				.run((context) -> assertThat(context).hasFailed());
	}

	@Test
	public void webExtensionWithEnabledByDefaultEndpoint() {
		this.contextRunner.withUserConfiguration(FooWebExtensionConfig.class)
				.run((context) -> assertThat(context)
						.hasSingleBean(FooWebEndpointExtension.class));
	}

	@Test
	public void webExtensionWithEnabledByDefaultEndpointCanBeDisabled() {
		this.contextRunner.withUserConfiguration(FooWebExtensionConfig.class)
				.withPropertyValues("endpoints.foo.enabled=false")
				.run((context) -> assertThat(context)
						.doesNotHaveBean(FooWebEndpointExtension.class));
	}

	@Test
	public void jmxExtensionWithEnabledByDefaultEndpoint() {
		this.contextRunner.withUserConfiguration(FooJmxExtensionConfig.class)
				.run((context) -> assertThat(context)
						.hasSingleBean(FooJmxEndpointExtension.class));
	}

	@Test
	public void jmxExtensionWithEnabledByDefaultEndpointCanBeDisabled() {
		this.contextRunner.withUserConfiguration(FooJmxExtensionConfig.class)
				.withPropertyValues("endpoints.foo.enabled=false")
				.run((context) -> assertThat(context)
						.doesNotHaveBean(FooJmxEndpointExtension.class));
	}

	@Configuration
	static class FooConfig {

		@Bean
		@ConditionalOnEnabledEndpoint
		public FooEndpoint foo() {
			return new FooEndpoint();
		}

	}

	@Endpoint(id = "foo")
	static class FooEndpoint {

	}

	@Configuration
	static class BarConfig {

		@Bean
		@ConditionalOnEnabledEndpoint
		public BarEndpoint bar() {
			return new BarEndpoint();
		}

	}

	@Endpoint(id = "bar", exposure = { EndpointExposure.WEB,
			EndpointExposure.JMX }, defaultEnablement = DefaultEnablement.DISABLED)
	static class BarEndpoint {

	}

	@Configuration
	static class OnlyWebConfig {

		@Bean(name = "onlyweb")
		@ConditionalOnEnabledEndpoint
		public OnlyWebEndpoint onlyWeb() {
			return new OnlyWebEndpoint();
		}

	}

	@Endpoint(id = "onlyweb", exposure = EndpointExposure.WEB)
	static class OnlyWebEndpoint {

	}

	@Configuration
	static class NonEndpointBeanConfig {

		@Bean
		@ConditionalOnEnabledEndpoint
		public String foo() {
			return "endpoint type cannot be detected";
		}

	}

	@JmxEndpointExtension(endpoint = FooEndpoint.class)
	static class FooJmxEndpointExtension {

	}

	@Configuration
	static class FooJmxExtensionConfig {

		@Bean
		@ConditionalOnEnabledEndpoint
		FooJmxEndpointExtension fooJmxEndpointExtension() {
			return new FooJmxEndpointExtension();
		}

	}

	@WebEndpointExtension(endpoint = FooEndpoint.class)
	static class FooWebEndpointExtension {

	}

	@Configuration
	static class FooWebExtensionConfig {

		@Bean
		@ConditionalOnEnabledEndpoint
		FooWebEndpointExtension fooJmxEndpointExtension() {
			return new FooWebEndpointExtension();
		}

	}

}
