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

import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.logging.ConditionEvaluationReportLoggingListener;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for configuration group used by
 * {@link ImportBeforeAutoConfigurationDeferredImportSelector} and
 * {@link ImportAfterAutoConfigurationDeferredImportSelector}.
 *
 * @author Tadaya Tsuyukubo
 */
class ImportBeforeAndAfterAutoConfigurationGroupOrderTests {

	// NOTE: cannot use "AutoConfigurations" since it is NOT a deferred import selector
	private ApplicationContextRunner runner = new ApplicationContextRunner()
			.withInitializer(new ConditionEvaluationReportLoggingListener());

	@Test
	void order() {
		this.runner.withUserConfiguration(Import_10_20_30_Config.class)
				.run((context) -> assertThat(context).hasNotFailed().getBean("myBean").isEqualTo("Config Order 10"));

		this.runner.withUserConfiguration(Import_20_30_40_Config.class)
				.run((context) -> assertThat(context).hasNotFailed().getBean("myBean").isEqualTo("Config Order 20"));
	}

	@Test
	void noOrder() {
		this.runner.withUserConfiguration(Import_NoOrder_20_30_Config.class)
				.run((context) -> assertThat(context).hasNotFailed().getBean("myBean").isEqualTo("Config Order 20"));

		this.runner.withUserConfiguration(Import_NoOrder_Config.class)
				.run((context) -> assertThat(context).hasNotFailed().getBean("myBean").isEqualTo("Config No Order"));
	}

	@Test
	void importAfterOnly() {
		this.runner.withUserConfiguration(ImportAfterOnlyConfig.class)
				.run((context) -> assertThat(context).hasNotFailed().getBean("myBean").isEqualTo("Config Order 10"));
	}

	@Test
	void importAfterAndAutoConfig() {
		this.runner.withUserConfiguration(ImportAfterAndAutoConfig.class)
				.run((context) -> assertThat(context).hasNotFailed().getBean("myBean").isEqualTo("Auto Config"));
	}

	@Test
	void userConfig() {
		// UserConfig always takes precedence
		this.runner.withUserConfiguration(Import_10_20_30_Config.class, UserConfig.class)
				.run((context) -> assertThat(context).hasNotFailed().getBean("myBean").isEqualTo("User Config"));

		this.runner.withUserConfiguration(Import_NoOrder_Config.class, UserConfig.class)
				.run((context) -> assertThat(context).hasNotFailed().getBean("myBean").isEqualTo("User Config"));
	}

	@Test
	void importBeforeAndAfter() {
		this.runner.withUserConfiguration(ImportBeforeAndAfterAndAfterConfig.class)
				.run((context) -> assertThat(context).hasNotFailed().getBean("myBean").isEqualTo("Config Order 10"));
	}

	@Test
	void importBothBeforeAndAfterWithAfterHasHigherPriority() {
		this.runner.withUserConfiguration(ImportBeforeAndAfterWithAfterHasHigherPriorityConfig.class)
				.run((context) -> assertThat(context).hasNotFailed().getBean("myBean")
						.as("@EnableConfigurationBeforeAutoConfiguration should be resolved first")
						.isEqualTo("Config Order 20"));
	}

	@Test
	void importFooAndBarAndAutoConfig() {
		this.runner.withUserConfiguration(ImportFooAndBarAndAutoConfig.class)
				.run((context) -> assertThat(context).hasNotFailed().hasBean("foo").hasBean("bar").hasBean("baz"));
	}

	@Configuration(proxyBeanMethods = false)
	@Order(10)
	static class ConfigOrder10 {

		@Bean
		@ConditionalOnMissingBean
		String myBean() {
			return "Config Order 10";
		}

	}

	@Configuration(proxyBeanMethods = false)
	@Order(20)
	static class ConfigOrder20 {

		@Bean
		@ConditionalOnMissingBean
		String myBean() {
			return "Config Order 20";
		}

	}

	@Configuration(proxyBeanMethods = false)
	@Order(30)
	static class ConfigOrder30 {

		@Bean
		@ConditionalOnMissingBean
		String myBean() {
			return "Config Order 30";
		}

	}

	@Configuration(proxyBeanMethods = false)
	@Order(40)
	static class ConfigOrder40 {

		@Bean
		@ConditionalOnMissingBean
		String myBean() {
			return "Config Order 40";
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class ConfigNoOrder {

		@Bean
		@ConditionalOnMissingBean
		String myBean() {
			return "Config No Order";
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class AutoConfig {

		@Bean
		@ConditionalOnMissingBean
		String myBean() {
			return "Auto Config";
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class UserConfig {

		@Bean
		String myBean() {
			return "User Config";
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ImportBeforeAutoConfiguration({ ConfigOrder10.class, ConfigOrder20.class, ConfigOrder30.class })
	@ImportAutoConfiguration(AutoConfig.class)
	static class Import_10_20_30_Config {

	}

	@Configuration(proxyBeanMethods = false)
	@ImportBeforeAutoConfiguration({ ConfigOrder20.class, ConfigOrder30.class, ConfigOrder40.class })
	@ImportAutoConfiguration(AutoConfig.class)
	static class Import_20_30_40_Config {

	}

	@Configuration(proxyBeanMethods = false)
	@ImportBeforeAutoConfiguration({ ConfigNoOrder.class, ConfigOrder20.class, ConfigOrder30.class })
	@ImportAutoConfiguration(AutoConfig.class)
	static class Import_NoOrder_20_30_Config {

	}

	@Configuration(proxyBeanMethods = false)
	@ImportBeforeAutoConfiguration(ConfigNoOrder.class)
	@ImportAutoConfiguration(AutoConfig.class)
	static class Import_NoOrder_Config {

	}

	@Configuration(proxyBeanMethods = false)
	@ImportAfterAutoConfiguration(ConfigOrder20.class)
	@ImportAutoConfiguration(AutoConfig.class)
	static class ImportAfterAndAutoConfig {

	}

	@Configuration(proxyBeanMethods = false)
	@ImportAfterAutoConfiguration({ ConfigOrder10.class, ConfigOrder20.class })
	static class ImportAfterOnlyConfig {

	}

	@Configuration(proxyBeanMethods = false)
	@ImportBeforeAutoConfiguration(ConfigOrder10.class)
	@ImportAfterAutoConfiguration(ConfigOrder20.class)
	@ImportAutoConfiguration(AutoConfig.class)
	static class ImportBeforeAndAfterAndAfterConfig {

	}

	@Configuration(proxyBeanMethods = false)
	@ImportBeforeAutoConfiguration(ConfigOrder20.class)
	@ImportAfterAutoConfiguration(ConfigOrder10.class) // after has higher priority
	@ImportAutoConfiguration(AutoConfig.class)
	static class ImportBeforeAndAfterWithAfterHasHigherPriorityConfig {

	}

	// -------

	@Configuration(proxyBeanMethods = false)
	static class FooConfig {

		@Bean
		String foo() {
			return "FOO";
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class BarConfig {

		@Bean
		String bar() {
			return "BAR";
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class BazConfig {

		@Bean
		String baz() {
			return "BAZ";
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ImportBeforeAutoConfiguration(FooConfig.class)
	@ImportAfterAutoConfiguration(BarConfig.class)
	@ImportAutoConfiguration(BazConfig.class) // BazConfig as AutoConfiguration
	static class ImportFooAndBarAndAutoConfig {

	}

}
