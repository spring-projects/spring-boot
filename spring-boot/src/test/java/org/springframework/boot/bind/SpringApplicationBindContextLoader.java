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

package org.springframework.boot.bind;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;

import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.test.context.ContextLoader;
import org.springframework.test.context.MergedContextConfiguration;
import org.springframework.test.context.support.AbstractContextLoader;
import org.springframework.test.context.support.TestPropertySourceUtils;

/**
 * {@link ContextLoader} used with binding tests.
 *
 * @author Phillip Webb
 */
class SpringApplicationBindContextLoader extends AbstractContextLoader {

	private static final String[] NO_SUFFIXES = new String[] {};

	@Override
	public ApplicationContext loadContext(MergedContextConfiguration config)
			throws Exception {
		SpringApplication application = new SpringApplication();
		application.setMainApplicationClass(config.getTestClass());
		application.setWebEnvironment(false);
		application.setSources(
				new LinkedHashSet<Object>(Arrays.asList(config.getClasses())));
		ConfigurableEnvironment environment = new StandardEnvironment();
		Map<String, Object> properties = new LinkedHashMap<String, Object>();
		properties.put("spring.jmx.enabled", "false");
		properties.putAll(TestPropertySourceUtils
				.convertInlinedPropertiesToMap(config.getPropertySourceProperties()));
		environment.getPropertySources().addAfter(
				StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME,
				new MapPropertySource("integrationTest", properties));
		application.setEnvironment(environment);
		return application.run();
	}

	@Override
	public ApplicationContext loadContext(String... locations) throws Exception {
		throw new UnsupportedOperationException();
	}

	@Override
	protected String[] getResourceSuffixes() {
		return NO_SUFFIXES;
	}

	@Override
	protected String getResourceSuffix() {
		throw new UnsupportedOperationException();
	}

}
