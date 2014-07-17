/*
 * Copyright 2012-2014 the original author or authors.
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

import org.junit.After;
import org.junit.Test;
import org.springframework.boot.test.EnvironmentTestUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Tests for {@link ConditionalOnProperty}.
 *
 * @author Maciej Walkowiak
 * @author Stephane Nicoll
 */
public class ConditionalOnPropertyTests {

	private AnnotationConfigApplicationContext context;

	@After
	public void tearDown() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void allPropertiesAreDefined() {
		load(MultiplePropertiesRequiredConfiguration.class,
				"property1=value1", "property2=value2");
		assertTrue(this.context.containsBean("foo"));
	}

	@Test
	public void notAllPropertiesAreDefined() {
		load(MultiplePropertiesRequiredConfiguration.class,
				"property1=value1");
		assertFalse(this.context.containsBean("foo"));
	}

	@Test
	public void propertyValueEqualsFalse() {
		load(MultiplePropertiesRequiredConfiguration.class,
				"property1=false", "property2=value2");
		assertFalse(this.context.containsBean("foo"));
	}

	@Test
	public void propertyValueEqualsFALSE() {
		load(MultiplePropertiesRequiredConfiguration.class,
				"property1=FALSE", "property2=value2");
		assertFalse(this.context.containsBean("foo"));
	}

	@Test
	public void relaxedName() {
		load(RelaxedPropertiesRequiredConfiguration.class,
				"spring.theRelaxedProperty=value1");
		assertTrue(this.context.containsBean("foo"));
	}

	@Test
	public void prefixWithoutPeriod() throws Exception {
		load(RelaxedPropertiesRequiredConfigurationWithShortPrefix.class,
				"spring.property=value1");
		assertTrue(this.context.containsBean("foo"));
	}

	@Test
	public void nonRelaxedName() throws Exception {
		load(NonRelaxedPropertiesRequiredConfiguration.class,
				"theRelaxedProperty=value1");
		assertFalse(this.context.containsBean("foo"));
	}

	@Test // Enabled by default
	public void enabledIfNotConfiguredOtherwise() {
		load(EnabledIfNotConfiguredOtherwiseConfig.class);
		assertTrue(this.context.containsBean("foo"));
	}

	@Test
	public void enabledIfNotConfiguredOtherwiseWithConfig() {
		load(EnabledIfNotConfiguredOtherwiseConfig.class, "simple.myProperty:false");
		assertFalse(this.context.containsBean("foo"));
	}

	@Test
	public void enabledIfNotConfiguredOtherwiseWithConfigDifferentCase() {
		load(EnabledIfNotConfiguredOtherwiseConfig.class, "simple.my-property:FALSE");
		assertFalse(this.context.containsBean("foo"));
	}

	@Test // Disabled by default
	public void disableIfNotConfiguredOtherwise() {
		load(DisabledIfNotConfiguredOtherwiseConfig.class);
		assertFalse(this.context.containsBean("foo"));
	}

	@Test
	public void disableIfNotConfiguredOtherwiseWithConfig() {
		load(DisabledIfNotConfiguredOtherwiseConfig.class, "simple.myProperty:true");
		assertTrue(this.context.containsBean("foo"));
	}

	@Test
	public void disableIfNotConfiguredOtherwiseWithConfigDifferentCase() {
		load(DisabledIfNotConfiguredOtherwiseConfig.class, "simple.myproperty:TrUe");
		assertTrue(this.context.containsBean("foo"));
	}

	@Test
	public void simpleValueIsSet() {
		load(SimpleValueConfig.class, "simple.myProperty:bar");
		assertTrue(this.context.containsBean("foo"));
	}

	@Test
	public void caseInsensitive() {
		load(SimpleValueConfig.class, "simple.myProperty:BaR");
		assertTrue(this.context.containsBean("foo"));
	}

	@Test
	public void defaultValueIsSet() {
		load(DefaultValueConfig.class, "simple.myProperty:bar");
		assertTrue(this.context.containsBean("foo"));
	}

	@Test
	public void defaultValueIsNotSet() {
		load(DefaultValueConfig.class);
		assertTrue(this.context.containsBean("foo"));
	}

	@Test
	public void defaultValueIsSetDifferentValue() {
		load(DefaultValueConfig.class, "simple.myProperty:another");
		assertFalse(this.context.containsBean("foo"));
	}

	@Test
	public void prefix() {
		load(PrefixValueConfig.class, "simple.myProperty:bar");
		assertTrue(this.context.containsBean("foo"));
	}

	@Test
	public void relaxedEnabledByDefault() {
		load(PrefixValueConfig.class, "simple.myProperty:bar");
		assertTrue(this.context.containsBean("foo"));
	}

	@Test
	public void strictNameMatch() {
		load(StrictNameConfig.class, "simple.my-property:bar");
		assertTrue(this.context.containsBean("foo"));
	}

	@Test
	public void strictNameNoMatch() {
		load(StrictNameConfig.class, "simple.myProperty:bar");
		assertFalse(this.context.containsBean("foo"));
	}

	@Test
	public void multiValuesAllSet() {
		load(MultiValuesConfig.class, "simple.my-property:bar", "simple.my-another-property:bar");
		assertTrue(this.context.containsBean("foo"));
	}

	@Test
	public void multiValuesOnlyOneSet() {
		load(MultiValuesConfig.class, "simple.my-property:bar");
		assertFalse(this.context.containsBean("foo"));
	}

	private void load(Class<?> config, String... environment) {
		this.context = new AnnotationConfigApplicationContext();
		EnvironmentTestUtils.addEnvironment(this.context, environment);
		this.context.register(config);
		this.context.refresh();
	}


	@Configuration
	@ConditionalOnProperty({ "property1", "property2" })
	protected static class MultiplePropertiesRequiredConfiguration {

		@Bean
		public String foo() {
			return "foo";
		}

	}

	@Configuration
	@ConditionalOnProperty(prefix = "spring.", value = "the-relaxed-property")
	protected static class RelaxedPropertiesRequiredConfiguration {

		@Bean
		public String foo() {
			return "foo";
		}

	}

	@Configuration
	@ConditionalOnProperty(prefix = "spring", value = "property")
	protected static class RelaxedPropertiesRequiredConfigurationWithShortPrefix {

		@Bean
		public String foo() {
			return "foo";
		}

	}

	@Configuration
	@ConditionalOnProperty(value = "the-relaxed-property", relaxedNames = false)
	protected static class NonRelaxedPropertiesRequiredConfiguration {

		@Bean
		public String foo() {
			return "foo";
		}

	}

	@Configuration // ${simple.myProperty:true}
	@ConditionalOnProperty(prefix = "simple", value = "my-property", match = "true", defaultMatch = true)
	static class EnabledIfNotConfiguredOtherwiseConfig {

		@Bean
		public String foo() {
			return "foo";
		}

	}

	@Configuration // ${simple.myProperty:false}
	@ConditionalOnProperty(prefix = "simple", value = "my-property", match = "true", defaultMatch = false)
	static class DisabledIfNotConfiguredOtherwiseConfig {

		@Bean
		public String foo() {
			return "foo";
		}

	}

	@Configuration
	@ConditionalOnProperty(prefix = "simple", value = "my-property", match = "bar")
	static class SimpleValueConfig {

		@Bean
		public String foo() {
			return "foo";
		}

	}

	@Configuration
	@ConditionalOnProperty(value = "simple.myProperty", match = "bar", defaultMatch = true)
	static class DefaultValueConfig {

		@Bean
		public String foo() {
			return "foo";
		}

	}

	@Configuration
	@ConditionalOnProperty(prefix = "simple", value = "my-property", match = "bar")
	static class PrefixValueConfig {

		@Bean
		public String foo() {
			return "foo";
		}

	}

	@Configuration
	@ConditionalOnProperty(prefix = "simple", value = "my-property", match = "bar", relaxedNames = false)
	static class StrictNameConfig {

		@Bean
		public String foo() {
			return "foo";
		}

	}

	@Configuration
	@ConditionalOnProperty(prefix = "simple", value = {"my-property", "my-another-property"}, match = "bar")
	static class MultiValuesConfig {

		@Bean
		public String foo() {
			return "foo";
		}

	}

}
