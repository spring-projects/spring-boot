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

package org.springframework.boot.autoconfigure;

import java.util.Locale;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.MessageSourceAutoConfigurationProfileTests.Config;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.junit.Assert.assertEquals;

/**
 * Tests for {@link MessageSourceAutoConfiguration}.
 *
 * @author Dave Syer
 */
@SpringApplicationConfiguration(classes = { Config.class,
		MessageSourceAutoConfiguration.class, PropertyPlaceholderAutoConfiguration.class })
@RunWith(SpringJUnit4ClassRunner.class)
@ActiveProfiles("switch-messages")
public class MessageSourceAutoConfigurationProfileTests {

	@Autowired
	private ApplicationContext context;

	@Test
	public void testMessageSourceFromPropertySourceAnnotation() throws Exception {
		assertEquals("bar",
				this.context.getMessage("foo", null, "Foo message", Locale.UK));
	}

	@Configuration
	protected static class Config {

	}
}
