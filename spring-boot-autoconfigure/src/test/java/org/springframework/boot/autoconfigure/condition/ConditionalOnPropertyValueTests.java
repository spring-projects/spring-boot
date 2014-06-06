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

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Test;

import org.springframework.boot.test.EnvironmentTestUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Tests for {@link ConditionalOnPropertyValue}
 *
 * @author Stephane Nicoll
 */
public class ConditionalOnPropertyValueTests {

	private AnnotationConfigApplicationContext context;

	@After
	public void tearDown() {
		if (this.context != null) {
			this.context.close();
		}
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

	private void load(Class<?> config, String... environment) {
		this.context = new AnnotationConfigApplicationContext();
		EnvironmentTestUtils.addEnvironment(this.context, environment);
		this.context.register(config);
		this.context.refresh();
	}

	@Configuration // ${simple.myProperty:true}
	@ConditionalOnPropertyValue(prefix = "simple", property = "my-property", value = "true", defaultMatch = true)
	static class EnabledIfNotConfiguredOtherwiseConfig {
		@Bean
		public String foo() {
			return "foo";
		}
	}

	@Configuration // ${simple.myProperty:false}
	@ConditionalOnPropertyValue(prefix = "simple", property = "my-property", value = "true", defaultMatch = false)
	static class DisabledIfNotConfiguredOtherwiseConfig {
		@Bean
		public String foo() {
			return "foo";
		}
	}

	@Configuration
	@ConditionalOnPropertyValue(prefix = "simple", property = "my-property", value = "bar")
	static class SimpleValueConfig {
		@Bean
		public String foo() {
			return "foo";
		}
	}

	@Configuration
	@ConditionalOnPropertyValue(property = "simple.myProperty", value = "bar", defaultMatch = true)
	static class DefaultValueConfig {
		@Bean
		public String foo() {
			return "foo";
		}
	}

	@Configuration
	@ConditionalOnPropertyValue(prefix = "simple", property = "my-property", value = "bar")
	static class PrefixValueConfig {
		@Bean
		public String foo() {
			return "foo";
		}
	}

	@Configuration
	@ConditionalOnPropertyValue(prefix = "simple", property = "my-property", value = "bar", relaxedName = false)
	static class StrictNameConfig {
		@Bean
		public String foo() {
			return "foo";
		}
	}

}
