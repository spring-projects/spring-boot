/*
 * Copyright 2012-2021 the original author or authors.
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

package org.springframework.boot.autoconfigure.condition;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.function.Consumer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.StandardEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link ConditionalOnProperty @ConditionalOnProperty}.
 *
 * @author Maciej Walkowiak
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
class ConditionalOnPropertyTests {

	private ConfigurableApplicationContext context;

	private ConfigurableEnvironment environment = new StandardEnvironment();

	@AfterEach
	void tearDown() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	void allPropertiesAreDefined() {
		load(MultiplePropertiesRequiredConfiguration.class, "property1=value1", "property2=value2");
		assertThat(this.context.containsBean("foo")).isTrue();
	}

	@Test
	void notAllPropertiesAreDefined() {
		load(MultiplePropertiesRequiredConfiguration.class, "property1=value1");
		assertThat(this.context.containsBean("foo")).isFalse();
	}

	@Test
	void propertyValueEqualsFalse() {
		load(MultiplePropertiesRequiredConfiguration.class, "property1=false", "property2=value2");
		assertThat(this.context.containsBean("foo")).isFalse();
	}

	@Test
	void propertyValueEqualsFALSE() {
		load(MultiplePropertiesRequiredConfiguration.class, "property1=FALSE", "property2=value2");
		assertThat(this.context.containsBean("foo")).isFalse();
	}

	@Test
	void relaxedName() {
		load(RelaxedPropertiesRequiredConfiguration.class, "spring.theRelaxedProperty=value1");
		assertThat(this.context.containsBean("foo")).isTrue();
	}

	@Test
	void prefixWithoutPeriod() {
		load(RelaxedPropertiesRequiredConfigurationWithShortPrefix.class, "spring.property=value1");
		assertThat(this.context.containsBean("foo")).isTrue();
	}

	@Test
	// Enabled by default
	void enabledIfNotConfiguredOtherwise() {
		load(EnabledIfNotConfiguredOtherwiseConfig.class);
		assertThat(this.context.containsBean("foo")).isTrue();
	}

	@Test
	void enabledIfNotConfiguredOtherwiseWithConfig() {
		load(EnabledIfNotConfiguredOtherwiseConfig.class, "simple.myProperty:false");
		assertThat(this.context.containsBean("foo")).isFalse();
	}

	@Test
	void enabledIfNotConfiguredOtherwiseWithConfigDifferentCase() {
		load(EnabledIfNotConfiguredOtherwiseConfig.class, "simple.my-property:FALSE");
		assertThat(this.context.containsBean("foo")).isFalse();
	}

	@Test
	// Disabled by default
	void disableIfNotConfiguredOtherwise() {
		load(DisabledIfNotConfiguredOtherwiseConfig.class);
		assertThat(this.context.containsBean("foo")).isFalse();
	}

	@Test
	void disableIfNotConfiguredOtherwiseWithConfig() {
		load(DisabledIfNotConfiguredOtherwiseConfig.class, "simple.myProperty:true");
		assertThat(this.context.containsBean("foo")).isTrue();
	}

	@Test
	void disableIfNotConfiguredOtherwiseWithConfigDifferentCase() {
		load(DisabledIfNotConfiguredOtherwiseConfig.class, "simple.myproperty:TrUe");
		assertThat(this.context.containsBean("foo")).isTrue();
	}

	@Test
	void simpleValueIsSet() {
		load(SimpleValueConfig.class, "simple.myProperty:bar");
		assertThat(this.context.containsBean("foo")).isTrue();
	}

	@Test
	void caseInsensitive() {
		load(SimpleValueConfig.class, "simple.myProperty:BaR");
		assertThat(this.context.containsBean("foo")).isTrue();
	}

	@Test
	void defaultValueIsSet() {
		load(DefaultValueConfig.class, "simple.myProperty:bar");
		assertThat(this.context.containsBean("foo")).isTrue();
	}

	@Test
	void defaultValueIsNotSet() {
		load(DefaultValueConfig.class);
		assertThat(this.context.containsBean("foo")).isTrue();
	}

	@Test
	void defaultValueIsSetDifferentValue() {
		load(DefaultValueConfig.class, "simple.myProperty:another");
		assertThat(this.context.containsBean("foo")).isFalse();
	}

	@Test
	void prefix() {
		load(PrefixValueConfig.class, "simple.myProperty:bar");
		assertThat(this.context.containsBean("foo")).isTrue();
	}

	@Test
	void relaxedEnabledByDefault() {
		load(PrefixValueConfig.class, "simple.myProperty:bar");
		assertThat(this.context.containsBean("foo")).isTrue();
	}

	@Test
	void multiValuesAllSet() {
		load(MultiValuesConfig.class, "simple.my-property:bar", "simple.my-another-property:bar");
		assertThat(this.context.containsBean("foo")).isTrue();
	}

	@Test
	void multiValuesOnlyOneSet() {
		load(MultiValuesConfig.class, "simple.my-property:bar");
		assertThat(this.context.containsBean("foo")).isFalse();
	}

	@Test
	void usingValueAttribute() {
		load(ValueAttribute.class, "some.property");
		assertThat(this.context.containsBean("foo")).isTrue();
	}

	@Test
	void nameOrValueMustBeSpecified() {
		assertThatIllegalStateException().isThrownBy(() -> load(NoNameOrValueAttribute.class, "some.property"))
				.satisfies(causeMessageContaining(
						"The name or value attribute of @ConditionalOnProperty must be specified"));
	}

	@Test
	void nameAndValueMustNotBeSpecified() {
		assertThatIllegalStateException().isThrownBy(() -> load(NameAndValueAttribute.class, "some.property"))
				.satisfies(causeMessageContaining(
						"The name and value attributes of @ConditionalOnProperty are exclusive"));
	}

	private <T extends Exception> Consumer<T> causeMessageContaining(String message) {
		return (ex) -> assertThat(ex.getCause()).hasMessageContaining(message);
	}

	@Test
	void metaAnnotationConditionMatchesWhenPropertyIsSet() {
		load(MetaAnnotation.class, "my.feature.enabled=true");
		assertThat(this.context.containsBean("foo")).isTrue();
	}

	@Test
	void metaAnnotationConditionDoesNotMatchWhenPropertyIsNotSet() {
		load(MetaAnnotation.class);
		assertThat(this.context.containsBean("foo")).isFalse();
	}

	@Test
	void metaAndDirectAnnotationConditionDoesNotMatchWhenOnlyDirectPropertyIsSet() {
		load(MetaAnnotationAndDirectAnnotation.class, "my.other.feature.enabled=true");
		assertThat(this.context.containsBean("foo")).isFalse();
	}

	@Test
	void metaAndDirectAnnotationConditionDoesNotMatchWhenOnlyMetaPropertyIsSet() {
		load(MetaAnnotationAndDirectAnnotation.class, "my.feature.enabled=true");
		assertThat(this.context.containsBean("foo")).isFalse();
	}

	@Test
	void metaAndDirectAnnotationConditionDoesNotMatchWhenNeitherPropertyIsSet() {
		load(MetaAnnotationAndDirectAnnotation.class);
		assertThat(this.context.containsBean("foo")).isFalse();
	}

	@Test
	void metaAndDirectAnnotationConditionMatchesWhenBothPropertiesAreSet() {
		load(MetaAnnotationAndDirectAnnotation.class, "my.feature.enabled=true", "my.other.feature.enabled=true");
		assertThat(this.context.containsBean("foo")).isTrue();
	}

	private void load(Class<?> config, String... environment) {
		TestPropertyValues.of(environment).applyTo(this.environment);
		this.context = new SpringApplicationBuilder(config).environment(this.environment).web(WebApplicationType.NONE)
				.run();
	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnProperty(name = { "property1", "property2" })
	static class MultiplePropertiesRequiredConfiguration {

		@Bean
		String foo() {
			return "foo";
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnProperty(prefix = "spring.", name = "the-relaxed-property")
	static class RelaxedPropertiesRequiredConfiguration {

		@Bean
		String foo() {
			return "foo";
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnProperty(prefix = "spring", name = "property")
	static class RelaxedPropertiesRequiredConfigurationWithShortPrefix {

		@Bean
		String foo() {
			return "foo";
		}

	}

	@Configuration(proxyBeanMethods = false)
	// i.e ${simple.myProperty:true}
	@ConditionalOnProperty(prefix = "simple", name = "my-property", havingValue = "true", matchIfMissing = true)
	static class EnabledIfNotConfiguredOtherwiseConfig {

		@Bean
		String foo() {
			return "foo";
		}

	}

	@Configuration(proxyBeanMethods = false)
	// i.e ${simple.myProperty:false}
	@ConditionalOnProperty(prefix = "simple", name = "my-property", havingValue = "true")
	static class DisabledIfNotConfiguredOtherwiseConfig {

		@Bean
		String foo() {
			return "foo";
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnProperty(prefix = "simple", name = "my-property", havingValue = "bar")
	static class SimpleValueConfig {

		@Bean
		String foo() {
			return "foo";
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnProperty(name = "simple.myProperty", havingValue = "bar", matchIfMissing = true)
	static class DefaultValueConfig {

		@Bean
		String foo() {
			return "foo";
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnProperty(prefix = "simple", name = "my-property", havingValue = "bar")
	static class PrefixValueConfig {

		@Bean
		String foo() {
			return "foo";
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnProperty(prefix = "simple", name = { "my-property", "my-another-property" }, havingValue = "bar")
	static class MultiValuesConfig {

		@Bean
		String foo() {
			return "foo";
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnProperty("some.property")
	static class ValueAttribute {

		@Bean
		String foo() {
			return "foo";
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnProperty
	static class NoNameOrValueAttribute {

		@Bean
		String foo() {
			return "foo";
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnProperty(value = "x", name = "y")
	static class NameAndValueAttribute {

		@Bean
		String foo() {
			return "foo";
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnMyFeature
	static class MetaAnnotation {

		@Bean
		String foo() {
			return "foo";
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnMyFeature
	@ConditionalOnProperty(prefix = "my.other.feature", name = "enabled", havingValue = "true")
	static class MetaAnnotationAndDirectAnnotation {

		@Bean
		String foo() {
			return "foo";
		}

	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target({ ElementType.TYPE, ElementType.METHOD })
	@ConditionalOnProperty(prefix = "my.feature", name = "enabled", havingValue = "true")
	@interface ConditionalOnMyFeature {

	}

}
