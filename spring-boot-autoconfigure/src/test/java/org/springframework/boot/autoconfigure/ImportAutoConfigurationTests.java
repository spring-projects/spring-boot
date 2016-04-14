/*
 * Copyright 2012-2016 the original author or authors.
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

package org.springframework.boot.autoconfigure;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.ClassUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ImportAutoConfiguration}.
 *
 * @author Phillip Webb
 */
public class ImportAutoConfigurationTests {

	@Test
	public void multipleAnnotationsShouldMergeCorrectly() {
		testConfigImports(Config.class);
		testConfigImports(AnotherConfig.class);
	}

	private void testConfigImports(Class<?> config) {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
				config);
		String shortName = ClassUtils.getShortName(ImportAutoConfigurationTests.class);
		int beginIndex = shortName.length() + 1;
		List<String> orderedConfigBeans = new ArrayList<String>();
		for (String bean : context.getBeanDefinitionNames()) {
			if (bean.contains("$Config")) {
				String shortBeanName = ClassUtils.getShortName(bean);
				orderedConfigBeans.add(shortBeanName.substring(beginIndex));
			}
		}
		assertThat(orderedConfigBeans).containsExactly("ConfigA", "ConfigB", "ConfigC",
				"ConfigD");
		context.close();
	}

	@ImportAutoConfiguration({ ConfigD.class, ConfigB.class })
	@MetaImportAutoConfiguration
	static class Config {

	}

	@MetaImportAutoConfiguration
	@ImportAutoConfiguration({ ConfigB.class, ConfigD.class })
	static class AnotherConfig {

	}

	@Retention(RetentionPolicy.RUNTIME)
	@ImportAutoConfiguration({ ConfigC.class, ConfigA.class })
	@interface MetaImportAutoConfiguration {

	}

	@Configuration
	static class ConfigA {
	}

	@Configuration
	@AutoConfigureAfter(ConfigA.class)
	static class ConfigB {
	}

	@Configuration
	@AutoConfigureAfter(ConfigB.class)
	static class ConfigC {
	}

	@Configuration
	@AutoConfigureAfter(ConfigC.class)
	static class ConfigD {
	}

}
