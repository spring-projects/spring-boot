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

package org.springframework.boot.autoconfigure.condition;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.StandardEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.internal.matchers.ThrowableMessageMatcher.hasMessage;

/**
 * Tests for {@link ConditionalOnProperty}.
 *
 * @author Maciej Walkowiak
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
public class ConditionalOnPropertyTests {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private ConfigurableApplicationContext context;

	private ConfigurableEnvironment environment = new StandardEnvironment();

	@After
	public void tearDown() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void allPropertiesAreDefined() {
		load(MultiplePropertiesRequiredConfiguration.class, "property1=value1",
				"property2=value2");
		assertThat(this.context.containsBean("foo")).isTrue();
	}

	@Test
	public void notAllPropertiesAreDefined() {
		load(MultiplePropertiesRequiredConfiguration.class, "property1=value1");
		assertThat(this.context.containsBean("foo")).isFalse();
	}

	@Test
	public void propertyValueEqualsFalse() {
		load(MultiplePropertiesRequiredConfiguration.class, "property1=false",
				"property2=value2");
		assertThat(this.context.containsBean("foo")).isFalse();
	}

	@Test
	public void propertyValueEqualsFALSE() {
		load(MultiplePropertiesRequiredConfiguration.class, "property1=FALSE",
				"property2=value2");
		assertThat(this.context.containsBean("foo")).isFalse();
	}

	@Test
	public void relaxedName() {
		load(RelaxedPropertiesRequiredConfiguration.class,
				"spring.theRelaxedProperty=value1");
		assertThat(this.context.containsBean("foo")).isTrue();
	}

	@Test
	public void prefixWithoutPeriod() throws Exception {
		load(RelaxedPropertiesRequiredConfigurationWithShortPrefix.class,
				"spring.property=value1");
		assertThat(this.context.containsBean("foo")).isTrue();
	}

	@Test
	// Enabled by default
	public void enabledIfNotConfiguredOtherwise() {
		load(EnabledIfNotConfiguredOtherwiseConfig.class);
		assertThat(this.context.containsBean("foo")).isTrue();
	}

	@Test
	public void enabledIfNotConfiguredOtherwiseWithConfig() {
		load(EnabledIfNotConfiguredOtherwiseConfig.class, "simple.myProperty:false");
		assertThat(this.context.containsBean("foo")).isFalse();
	}

	@Test
	public void enabledIfNotConfiguredOtherwiseWithConfigDifferentCase() {
		load(EnabledIfNotConfiguredOtherwiseConfig.class, "simple.my-property:FALSE");
		assertThat(this.context.containsBean("foo")).isFalse();
	}

	@Test
	// Disabled by default
	public void disableIfNotConfiguredOtherwise() {
		load(DisabledIfNotConfiguredOtherwiseConfig.class);
		assertThat(this.context.containsBean("foo")).isFalse();
	}

	@Test
	public void disableIfNotConfiguredOtherwiseWithConfig() {
		load(DisabledIfNotConfiguredOtherwiseConfig.class, "simple.myProperty:true");
		assertThat(this.context.containsBean("foo")).isTrue();
	}

	@Test
	public void disableIfNotConfiguredOtherwiseWithConfigDifferentCase() {
		load(DisabledIfNotConfiguredOtherwiseConfig.class, "simple.myproperty:TrUe");
		assertThat(this.context.containsBean("foo")).isTrue();
	}

	@Test
	public void simpleValueIsSet() {
		load(SimpleValueConfig.class, "simple.myProperty:bar");
		assertThat(this.context.containsBean("foo")).isTrue();
	}

	@Test
	public void caseInsensitive() {
		load(SimpleValueConfig.class, "simple.myProperty:BaR");
		assertThat(this.context.containsBean("foo")).isTrue();
	}

	@Test
	public void defaultValueIsSet() {
		load(DefaultValueConfig.class, "simple.myProperty:bar");
		assertThat(this.context.containsBean("foo")).isTrue();
	}

	@Test
	public void defaultValueIsNotSet() {
		load(DefaultValueConfig.class);
		assertThat(this.context.containsBean("foo")).isTrue();
	}

	@Test
	public void defaultValueIsSetDifferentValue() {
		load(DefaultValueConfig.class, "simple.myProperty:another");
		assertThat(this.context.containsBean("foo")).isFalse();
	}

	@Test
	public void prefix() {
		load(PrefixValueConfig.class, "simple.myProperty:bar");
		assertThat(this.context.containsBean("foo")).isTrue();
	}

	@Test
	public void relaxedEnabledByDefault() {
		load(PrefixValueConfig.class, "simple.myProperty:bar");
		assertThat(this.context.containsBean("foo")).isTrue();
	}

	@Test
	public void multiValuesAllSet() {
		load(MultiValuesConfig.class, "simple.my-property:bar",
				"simple.my-another-property:bar");
		assertThat(this.context.containsBean("foo")).isTrue();
	}

	@Test
	public void multiValuesOnlyOneSet() {
		load(MultiValuesConfig.class, "simple.my-property:bar");
		assertThat(this.context.containsBean("foo")).isFalse();
	}

	@Test
	public void usingValueAttribute() throws Exception {
		load(ValueAttribute.class, "some.property");
		assertThat(this.context.containsBean("foo")).isTrue();
	}

	@Test
	public void nameOrValueMustBeSpecified() throws Exception {
		this.thrown.expect(IllegalStateException.class);
		this.thrown.expectCause(hasMessage(containsString("The name or "
				+ "value attribute of @ConditionalOnProperty must be specified")));
		load(NoNameOrValueAttribute.class, "some.property");
	}

	@Test
	public void nameAndValueMustNotBeSpecified() throws Exception {
		this.thrown.expect(IllegalStateException.class);
		this.thrown.expectCause(hasMessage(containsString("The name and "
				+ "value attributes of @ConditionalOnProperty are exclusive")));
		load(NameAndValueAttribute.class, "some.property");
	}

	@Test
	public void metaAnnotationConditionMatchesWhenPropertyIsSet() throws Exception {
		load(MetaAnnotation.class, "my.feature.enabled=true");
		assertThat(this.context.containsBean("foo")).isTrue();
	}

	@Test
	public void metaAnnotationConditionDoesNotMatchWhenPropertyIsNotSet()
			throws Exception {
		load(MetaAnnotation.class);
		assertThat(this.context.containsBean("foo")).isFalse();
	}

	@Test
	public void metaAndDirectAnnotationConditionDoesNotMatchWhenOnlyDirectPropertyIsSet() {
		load(MetaAnnotationAndDirectAnnotation.class, "my.other.feature.enabled=true");
		assertThat(this.context.containsBean("foo")).isFalse();
	}

	@Test
	public void metaAndDirectAnnotationConditionDoesNotMatchWhenOnlyMetaPropertyIsSet() {
		load(MetaAnnotationAndDirectAnnotation.class, "my.feature.enabled=true");
		assertThat(this.context.containsBean("foo")).isFalse();
	}

	@Test
	public void metaAndDirectAnnotationConditionDoesNotMatchWhenNeitherPropertyIsSet() {
		load(MetaAnnotationAndDirectAnnotation.class);
		assertThat(this.context.containsBean("foo")).isFalse();
	}

	@Test
	public void metaAndDirectAnnotationConditionMatchesWhenBothPropertiesAreSet() {
		load(MetaAnnotationAndDirectAnnotation.class, "my.feature.enabled=true",
				"my.other.feature.enabled=true");
		assertThat(this.context.containsBean("foo")).isTrue();
	}

	private void load(Class<?> config, String... environment) {
		TestPropertyValues.of(environment).applyTo(this.environment);
		this.context = new SpringApplicationBuilder(config).environment(this.environment)
				.web(WebApplicationType.NONE).run();
	}

	@Configuration
	@ConditionalOnProperty(name = { "property1", "property2" })
	protected static class MultiplePropertiesRequiredConfiguration {

		@Bean
		public String foo() {
			return "foo";
		}

	}

	@Configuration
	@ConditionalOnProperty(prefix = "spring.", name = "the-relaxed-property")
	protected static class RelaxedPropertiesRequiredConfiguration {

		@Bean
		public String foo() {
			return "foo";
		}

	}

	@Configuration
	@ConditionalOnProperty(prefix = "spring", name = "property")
	protected static class RelaxedPropertiesRequiredConfigurationWithShortPrefix {

		@Bean
		public String foo() {
			return "foo";
		}

	}

	@Configuration
	// i.e ${simple.myProperty:true}
	@ConditionalOnProperty(prefix = "simple", name = "my-property", havingValue = "true", matchIfMissing = true)
	static class EnabledIfNotConfiguredOtherwiseConfig {

		@Bean
		public String foo() {
			return "foo";
		}

	}

	@Configuration
	// i.e ${simple.myProperty:false}
	@ConditionalOnProperty(prefix = "simple", name = "my-property", havingValue = "true", matchIfMissing = false)
	static class DisabledIfNotConfiguredOtherwiseConfig {

		@Bean
		public String foo() {
			return "foo";
		}

	}

	@Configuration
	@ConditionalOnProperty(prefix = "simple", name = "my-property", havingValue = "bar")
	static class SimpleValueConfig {

		@Bean
		public String foo() {
			return "foo";
		}

	}

	@Configuration
	@ConditionalOnProperty(name = "simple.myProperty", havingValue = "bar", matchIfMissing = true)
	static class DefaultValueConfig {

		@Bean
		public String foo() {
			return "foo";
		}

	}

	@Configuration
	@ConditionalOnProperty(prefix = "simple", name = "my-property", havingValue = "bar")
	static class PrefixValueConfig {

		@Bean
		public String foo() {
			return "foo";
		}

	}

	@Configuration
	@ConditionalOnProperty(prefix = "simple", name = { "my-property",
			"my-another-property" }, havingValue = "bar")
	static class MultiValuesConfig {

		@Bean
		public String foo() {
			return "foo";
		}

	}

	@Configuration
	@ConditionalOnProperty("some.property")
	protected static class ValueAttribute {

		@Bean
		public String foo() {
			return "foo";
		}

	}

	@Configuration
	@ConditionalOnProperty
	protected static class NoNameOrValueAttribute {

		@Bean
		public String foo() {
			return "foo";
		}

	}

	@Configuration
	@ConditionalOnProperty(value = "x", name = "y")
	protected static class NameAndValueAttribute {

		@Bean
		public String foo() {
			return "foo";
		}

	}

	@Configuration
	@ConditionalOnMyFeature
	protected static class MetaAnnotation {

		@Bean
		public String foo() {
			return "foo";
		}

	}

	@Configuration
	@ConditionalOnMyFeature
	@ConditionalOnProperty(prefix = "my.other.feature", name = "enabled", havingValue = "true", matchIfMissing = false)
	protected static class MetaAnnotationAndDirectAnnotation {

		@Bean
		public String foo() {
			return "foo";
		}

	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target({ ElementType.TYPE, ElementType.METHOD })
	@ConditionalOnProperty(prefix = "my.feature", name = "enabled", havingValue = "true", matchIfMissing = false)
	public @interface ConditionalOnMyFeature {

	}

}
