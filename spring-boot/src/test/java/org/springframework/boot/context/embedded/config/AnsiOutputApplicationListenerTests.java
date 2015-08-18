/*
 * Copyright 2012-2015 the original author or authors.
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

package org.springframework.boot.context.embedded.config;

import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.ansi.AnsiOutput;
import org.springframework.boot.ansi.AnsiOutput.Enabled;
import org.springframework.boot.ansi.AnsiOutputEnabledValue;
import org.springframework.boot.context.config.AnsiOutputApplicationListener;
import org.springframework.boot.test.EnvironmentTestUtils;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.StandardEnvironment;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * Tests for {@link AnsiOutputApplicationListener}.
 *
 * @author Phillip Webb
 */
public class AnsiOutputApplicationListenerTests {

	@Before
	@After
	public void resetAnsi() {
		AnsiOutput.setEnabled(Enabled.DETECT);
	}

	@Test
	public void enabled() {
		SpringApplication application = new SpringApplication(Config.class);
		application.setWebEnvironment(false);
		Map<String, Object> props = new HashMap<String, Object>();
		props.put("spring.output.ansi.enabled", "ALWAYS");
		application.setDefaultProperties(props);
		application.run();
		assertThat(AnsiOutputEnabledValue.get(), equalTo(Enabled.ALWAYS));
	}

	@Test
	public void disabled() throws Exception {
		SpringApplication application = new SpringApplication(Config.class);
		application.setWebEnvironment(false);
		Map<String, Object> props = new HashMap<String, Object>();
		props.put("spring.output.ansi.enabled", "never");
		application.setDefaultProperties(props);
		application.run();
		assertThat(AnsiOutputEnabledValue.get(), equalTo(Enabled.NEVER));
	}

	@Test
	public void disabledViaApplicationProperties() throws Exception {
		ConfigurableEnvironment environment = new StandardEnvironment();
		EnvironmentTestUtils.addEnvironment(environment, "spring.config.name:ansi");
		SpringApplication application = new SpringApplication(Config.class);
		application.setWebEnvironment(false);
		application.setEnvironment(environment);
		application.run();
		assertThat(AnsiOutputEnabledValue.get(), equalTo(Enabled.NEVER));
	}

	@Configuration
	public static class Config {
	}

}
