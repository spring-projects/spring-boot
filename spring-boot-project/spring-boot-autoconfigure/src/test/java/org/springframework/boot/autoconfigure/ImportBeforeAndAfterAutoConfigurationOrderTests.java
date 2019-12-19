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

package org.springframework.boot.autoconfigure;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ImportBeforeAutoConfiguration} and
 * {@link ImportAfterAutoConfiguration}
 *
 * @author Tadaya Tsuyukubo
 */
class ImportBeforeAndAfterAutoConfigurationOrderTests {

	@Nested
	@SpringBootTest(classes = MyAppConfigEnableBefore.class)
	@ImportAutoConfiguration(MyAutoConfig.class)
	class BeforeAutoConfigurationTest {

		@Autowired
		String bean;

		@Test
		void check() {
			assertThat(this.bean).isEqualTo("From my config");
		}

	}

	@Nested
	@SpringBootTest(classes = MyAppConfigEnableBeforeWithBean.class)
	@ImportAutoConfiguration(MyAutoConfig.class)
	class BeforeAutoConfigurationWithBeanTest {

		@Autowired
		String bean;

		@Test
		void check() {
			assertThat(this.bean).isEqualTo("From app config");
		}

	}

	@Nested
	@SpringBootTest(classes = MyAppConfigEnableAfter.class)
	@ImportAutoConfiguration(MyAutoConfig.class)
	class AfterAutoConfigurationTest {

		@Autowired
		String bean;

		@Test
		void check() {
			assertThat(this.bean).isEqualTo("From auto config");
		}

	}

	@Nested
	@SpringBootTest(classes = MyAppConfigEnableAfterWithBean.class)
	@ImportAutoConfiguration(MyAutoConfig.class)
	class AfterAutoConfigurationWithBeanTest {

		@Autowired
		String bean;

		@Test
		void check() {
			assertThat(this.bean).isEqualTo("From app config");
		}

	}

	// my configuration
	@Configuration(proxyBeanMethods = false)
	static class MyConfig {

		@Bean
		@ConditionalOnMissingBean
		String bean() {
			return "From my config";
		}

	}

	// pretend to be auto configuration (used by @ImportAutoConfiguration)
	@Configuration(proxyBeanMethods = false)
	static class MyAutoConfig {

		@Bean
		@ConditionalOnMissingBean
		String bean() {
			return "From auto config";
		}

	}

	// @EnableX to apply MyConfig BEFORE auto configurations
	@Target(ElementType.TYPE)
	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	@ImportBeforeAutoConfiguration(MyConfig.class)
	@interface EnableBeforeAutoConfiguration {

	}

	// @EnableX to apply MyConfig AFTER auto configurations
	@Target(ElementType.TYPE)
	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	@ImportAfterAutoConfiguration(MyConfig.class)
	@interface EnableAfterAutoConfiguration {

	}

	// test configuration that enables my configuration BEFORE auto configs
	@Configuration(proxyBeanMethods = false)
	@EnableBeforeAutoConfiguration
	static class MyAppConfigEnableBefore {

	}

	// test configuration that enables my configuration BEFORE auto configs but has
	// defined a bean
	@Configuration(proxyBeanMethods = false)
	@EnableBeforeAutoConfiguration
	static class MyAppConfigEnableBeforeWithBean {

		@Bean
		String bean() {
			return "From app config";
		}

	}

	// test configuration that enables my configuration AFTER auto configs
	@Configuration(proxyBeanMethods = false)
	@EnableAfterAutoConfiguration
	static class MyAppConfigEnableAfter {

	}

	// test configuration that enables my configuration AFTER auto configs but has defined
	// a bean
	@Configuration(proxyBeanMethods = false)
	@EnableAfterAutoConfiguration
	static class MyAppConfigEnableAfterWithBean {

		@Bean
		String bean() {
			return "From app config";
		}

	}

}
