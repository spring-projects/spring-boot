/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.autoconfigure.context;

import java.util.Locale;

import org.junit.Ignore;
import org.junit.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.ContextConsumer;
import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.NoSuchMessageException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link MessageSourceAutoConfiguration}.
 *
 * @author Dave Syer
 * @author Eddú Meléndez
 * @author Stephane Nicoll
 * @author Kedar Joshi
 */
public class MessageSourceAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(
					AutoConfigurations.of(MessageSourceAutoConfiguration.class));

	@Test
	public void testDefaultMessageSource() {
		this.contextRunner.run((context) -> assertThat(
				context.getMessage("foo", null, "Foo message", Locale.UK))
						.isEqualTo("Foo message"));
	}

	@Test
	public void propertiesBundleWithSlashIsDetected() {
		this.contextRunner.withPropertyValues("spring.messages.basename:test/messages")
				.run((context) -> {
					assertThat(context).hasSingleBean(MessageSource.class);
					assertThat(context.getMessage("foo", null, "Foo message", Locale.UK))
							.isEqualTo("bar");
				});
	}

	@Test
	public void propertiesBundleWithDotIsDetected() {
		this.contextRunner.withPropertyValues("spring.messages.basename:test.messages")
				.run((context) -> {
					assertThat(context).hasSingleBean(MessageSource.class);
					assertThat(context.getMessage("foo", null, "Foo message", Locale.UK))
							.isEqualTo("bar");
				});
	}

	@Test
	public void testEncodingWorks() {
		this.contextRunner.withPropertyValues("spring.messages.basename:test/swedish")
				.run((context) -> assertThat(
						context.getMessage("foo", null, "Foo message", Locale.UK))
								.isEqualTo("Some text with some swedish öäå!"));
	}

	@Test
	public void testCacheDurationNoUnit() {
		this.contextRunner.withPropertyValues("spring.messages.basename:test/messages",
				"spring.messages.cache-duration=10").run(assertCache(10 * 1000));
	}

	@Test
	public void testCacheDurationWithUnit() {
		this.contextRunner.withPropertyValues("spring.messages.basename:test/messages",
				"spring.messages.cache-duration=1m").run(assertCache(60 * 1000));
	}

	private ContextConsumer<AssertableApplicationContext> assertCache(long expected) {
		return (context) -> {
			assertThat(context).hasSingleBean(MessageSource.class);
			assertThat(context.getBean(MessageSource.class))
					.hasFieldOrPropertyWithValue("cacheMillis", expected);
		};
	}

	@Test
	public void testMultipleMessageSourceCreated() {
		this.contextRunner
				.withPropertyValues(
						"spring.messages.basename:test/messages,test/messages2")
				.run((context) -> {
					assertThat(context.getMessage("foo", null, "Foo message", Locale.UK))
							.isEqualTo("bar");
					assertThat(context.getMessage("foo-foo", null, "Foo-Foo message",
							Locale.UK)).isEqualTo("bar-bar");
				});
	}

	@Test
	public void testBadEncoding() {
		// Bad encoding just means the messages are ignored
		this.contextRunner.withPropertyValues("spring.messages.encoding:rubbish")
				.run((context) -> assertThat(
						context.getMessage("foo", null, "blah", Locale.UK))
								.isEqualTo("blah"));
	}

	@Test
	@Ignore("Expected to fail per gh-1075")
	public void testMessageSourceFromPropertySourceAnnotation() {
		this.contextRunner.withUserConfiguration(Config.class)
				.run((context) -> assertThat(
						context.getMessage("foo", null, "Foo message", Locale.UK))
								.isEqualTo("bar"));
	}

	@Test
	public void testFallbackDefault() {
		this.contextRunner.withPropertyValues("spring.messages.basename:test/messages")
				.run((context) -> assertThat(context.getBean(MessageSource.class))
						.hasFieldOrPropertyWithValue("fallbackToSystemLocale", true));
	}

	@Test
	public void testFallbackTurnOff() {
		this.contextRunner
				.withPropertyValues("spring.messages.basename:test/messages",
						"spring.messages.fallback-to-system-locale:false")
				.run((context) -> assertThat(context.getBean(MessageSource.class))
						.hasFieldOrPropertyWithValue("fallbackToSystemLocale", false));
	}

	@Test
	public void testFormatMessageDefault() {
		this.contextRunner.withPropertyValues("spring.messages.basename:test/messages")
				.run((context) -> assertThat(context.getBean(MessageSource.class))
						.hasFieldOrPropertyWithValue("alwaysUseMessageFormat", false));
	}

	@Test
	public void testFormatMessageOn() {
		this.contextRunner
				.withPropertyValues("spring.messages.basename:test/messages",
						"spring.messages.always-use-message-format:true")
				.run((context) -> assertThat(context.getBean(MessageSource.class))
						.hasFieldOrPropertyWithValue("alwaysUseMessageFormat", true));
	}

	@Test
	public void testUseCodeAsDefaultMessageDefault() {
		this.contextRunner.withPropertyValues("spring.messages.basename:test/messages")
				.run((context) -> assertThat(context.getBean(MessageSource.class))
						.hasFieldOrPropertyWithValue("useCodeAsDefaultMessage", false));
	}

	@Test
	public void testUseCodeAsDefaultMessageOn() {
		this.contextRunner
				.withPropertyValues("spring.messages.basename:test/messages",
						"spring.messages.use-code-as-default-message:true")
				.run((context) -> assertThat(context.getBean(MessageSource.class))
						.hasFieldOrPropertyWithValue("useCodeAsDefaultMessage", true));
	}

	@Test
	public void existingMessageSourceIsPreferred() {
		this.contextRunner.withUserConfiguration(CustomMessageSourceConfiguration.class)
				.run((context) -> assertThat(context.getMessage("foo", null, null, null))
						.isEqualTo("foo"));
	}

	@Test
	public void existingMessageSourceInParentIsIgnored() {
		this.contextRunner.run((parent) -> this.contextRunner.withParent(parent)
				.withPropertyValues("spring.messages.basename:test/messages")
				.run((context) -> assertThat(
						context.getMessage("foo", null, "Foo message", Locale.UK))
								.isEqualTo("bar")));
	}

	@Test
	public void messageSourceWithNonStandardBeanNameIsIgnored() {
		this.contextRunner.withPropertyValues("spring.messages.basename:test/messages")
				.withUserConfiguration(CustomBeanNameMessageSourceConfiguration.class)
				.run((context) -> {
					assertThat(context.getMessage("foo", null, Locale.US))
							.isEqualTo("bar");
				});
	}

	@Configuration
	@PropertySource("classpath:/switch-messages.properties")
	protected static class Config {

	}

	@Configuration
	protected static class CustomMessageSourceConfiguration {

		@Bean
		public MessageSource messageSource() {
			return new TestMessageSource();
		}

	}

	@Configuration
	protected static class CustomBeanNameMessageSourceConfiguration {

		@Bean
		public MessageSource codeReturningMessageSource() {
			return new TestMessageSource();
		}

	}

	private static class TestMessageSource implements MessageSource {

		@Override
		public String getMessage(String code, Object[] args, String defaultMessage,
				Locale locale) {
			return code;
		}

		@Override
		public String getMessage(String code, Object[] args, Locale locale)
				throws NoSuchMessageException {
			return code;
		}

		@Override
		public String getMessage(MessageSourceResolvable resolvable, Locale locale)
				throws NoSuchMessageException {
			return resolvable.getCodes()[0];
		}

	}

}
