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

package org.springframework.boot.autoconfigure;

import java.util.Locale;

import org.junit.After;
import org.junit.Ignore;
import org.junit.Test;

import org.springframework.boot.test.EnvironmentTestUtils;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.NoSuchMessageException;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Tests for {@link MessageSourceAutoConfiguration}.
 *
 * @author Dave Syer
 * @author Eddú Meléndez
 * @author Stephane Nicoll
 */
public class MessageSourceAutoConfigurationTests {

	private AnnotationConfigApplicationContext context;

	@After
	public void closeContext() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void testDefaultMessageSource() throws Exception {
		load();
		assertEquals("Foo message",
				this.context.getMessage("foo", null, "Foo message", Locale.UK));
	}

	@Test
	public void testMessageSourceCreated() throws Exception {
		load("spring.messages.basename:test/messages");
		assertEquals("bar",
				this.context.getMessage("foo", null, "Foo message", Locale.UK));
	}

	@Test
	public void testEncodingWorks() throws Exception {
		load("spring.messages.basename:test/swedish");
		assertEquals("Some text with some swedish öäå!",
				this.context.getMessage("foo", null, "Foo message", Locale.UK));
	}

	@Test
	public void testMultipleMessageSourceCreated() throws Exception {
		load("spring.messages.basename:test/messages,test/messages2");
		assertEquals("bar",
				this.context.getMessage("foo", null, "Foo message", Locale.UK));
		assertEquals("bar-bar",
				this.context.getMessage("foo-foo", null, "Foo-Foo message", Locale.UK));
	}

	@Test
	public void testBadEncoding() throws Exception {
		load("spring.messages.encoding:rubbish");
		// Bad encoding just means the messages are ignored
		assertEquals("blah", this.context.getMessage("foo", null, "blah", Locale.UK));
	}

	@Test
	@Ignore("Expected to fail per gh-1075")
	public void testMessageSourceFromPropertySourceAnnotation() throws Exception {
		this.context = new AnnotationConfigApplicationContext();
		this.context.register(Config.class, MessageSourceAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		this.context.refresh();
		assertEquals("bar",
				this.context.getMessage("foo", null, "Foo message", Locale.UK));
	}

	@Test
	public void testFallbackDefault() throws Exception {
		load("spring.messages.basename:test/messages");

		assertTrue(this.context.getBean(MessageSourceAutoConfiguration.class)
				.isFallbackToSystemLocale());
	}

	@Test
	public void testFallbackTurnOff() throws Exception {
		load("spring.messages.basename:test/messages",
				"spring.messages.fallback-to-system-locale:false");
		assertFalse(this.context.getBean(MessageSourceAutoConfiguration.class)
				.isFallbackToSystemLocale());
	}

	@Test
	public void existingMessageSourceIsPreferred() {
		this.context = new AnnotationConfigApplicationContext();
		this.context.register(CustomMessageSource.class,
				MessageSourceAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		this.context.refresh();
		assertEquals("foo", this.context.getMessage("foo", null, null, null));
	}

	@Test
	public void existingMessageSourceInParentIsIgnored() {
		ConfigurableApplicationContext parent = new AnnotationConfigApplicationContext();
		parent.refresh();
		try {
			this.context = new AnnotationConfigApplicationContext();
			this.context.setParent(parent);
			EnvironmentTestUtils.addEnvironment(this.context,
					"spring.messages.basename:test/messages");
			this.context.register(MessageSourceAutoConfiguration.class,
					PropertyPlaceholderAutoConfiguration.class);
			this.context.refresh();
			assertEquals("bar",
					this.context.getMessage("foo", null, "Foo message", Locale.UK));
		}
		finally {
			parent.close();
		}
	}

	private void load(String... environment) {
		this.context = new AnnotationConfigApplicationContext();
		EnvironmentTestUtils.addEnvironment(this.context, environment);
		this.context.register(MessageSourceAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		this.context.refresh();
	}

	@Configuration
	@PropertySource("classpath:/switch-messages.properties")
	protected static class Config {

	}

	@Configuration
	protected static class CustomMessageSource {

		@Bean
		public MessageSource messageSource() {
			return new MessageSource() {

				@Override
				public String getMessage(String code, Object[] args,
						String defaultMessage, Locale locale) {
					return code;
				}

				@Override
				public String getMessage(String code, Object[] args, Locale locale)
						throws NoSuchMessageException {
					return code;
				}

				@Override
				public String getMessage(MessageSourceResolvable resolvable,
						Locale locale) throws NoSuchMessageException {
					return resolvable.getCodes()[0];
				}

			};
		}

	}
}
