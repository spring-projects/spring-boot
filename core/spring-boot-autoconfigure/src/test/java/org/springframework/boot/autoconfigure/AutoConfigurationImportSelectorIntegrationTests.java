/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.autoconfigure;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.ClassUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link AutoConfigurationImportSelector}.
 *
 * @author Stephane Nicoll
 */
class AutoConfigurationImportSelectorIntegrationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner();

	@Test
	void singleSelectorWithNoImports() {
		this.contextRunner.withUserConfiguration(NoConfig.class)
			.run((context) -> assertThat(getImportedConfigBeans(context)).isEmpty());
	}

	@Test
	void singleSelector() {
		this.contextRunner.withUserConfiguration(SingleConfig.class)
			.run((context) -> assertThat(getImportedConfigBeans(context)).containsExactly("ConfigC"));
	}

	@Test
	void multipleSelectorsShouldMergeAndSortCorrectly() {
		this.contextRunner.withUserConfiguration(MultiConfig.class, AnotherMultiConfig.class)
			.run((context) -> assertThat(getImportedConfigBeans(context)).containsExactly("ConfigA", "ConfigB",
					"ConfigC", "ConfigD"));
	}

	@Test
	void multipleSelectorsWithRedundantImportsShouldMergeAndSortCorrectly() {
		this.contextRunner.withUserConfiguration(SingleConfig.class, MultiConfig.class, AnotherMultiConfig.class)
			.run((context) -> assertThat(getImportedConfigBeans(context)).containsExactly("ConfigA", "ConfigB",
					"ConfigC", "ConfigD"));
	}

	private List<String> getImportedConfigBeans(AssertableApplicationContext context) {
		String shortName = ClassUtils.getShortName(AutoConfigurationImportSelectorIntegrationTests.class);
		int beginIndex = shortName.length() + 1;
		List<String> orderedConfigBeans = new ArrayList<>();
		for (String bean : context.getBeanDefinitionNames()) {
			if (bean.contains("$Config")) {
				String shortBeanName = ClassUtils.getShortName(bean);
				orderedConfigBeans.add(shortBeanName.substring(beginIndex));
			}
		}
		return orderedConfigBeans;
	}

	@ImportAutoConfiguration
	static class NoConfig {

	}

	@ImportAutoConfiguration(ConfigC.class)
	static class SingleConfig {

	}

	@ImportAutoConfiguration({ ConfigD.class, ConfigB.class })
	static class MultiConfig {

	}

	@ImportAutoConfiguration({ ConfigC.class, ConfigA.class })
	static class AnotherMultiConfig {

	}

	@Configuration(proxyBeanMethods = false)
	static class ConfigA {

	}

	@Configuration(proxyBeanMethods = false)
	@AutoConfigureAfter(ConfigA.class)
	@AutoConfigureBefore(ConfigC.class)
	static class ConfigB {

	}

	@Configuration(proxyBeanMethods = false)
	static class ConfigC {

	}

	@Configuration(proxyBeanMethods = false)
	@AutoConfigureAfter(ConfigC.class)
	static class ConfigD {

	}

}
