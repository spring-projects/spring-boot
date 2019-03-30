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

package org.springframework.boot.autoconfigure.context;

import java.util.Locale;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link MessageSourceAutoConfiguration}.
 *
 * @author Dave Syer
 */

@SpringBootTest
@ImportAutoConfiguration({ MessageSourceAutoConfiguration.class,
		PropertyPlaceholderAutoConfiguration.class })
@ActiveProfiles("switch-messages")
@DirtiesContext
@RunWith(SpringRunner.class)
public class MessageSourceAutoConfigurationProfileTests {

	@Autowired
	private ApplicationContext context;

	@Test
	public void testMessageSourceFromPropertySourceAnnotation() {
		assertThat(this.context.getMessage("foo", null, "Foo message", Locale.UK))
				.isEqualTo("bar");
	}

	@Configuration(proxyBeanMethods = false)
	protected static class Config {

	}

}
