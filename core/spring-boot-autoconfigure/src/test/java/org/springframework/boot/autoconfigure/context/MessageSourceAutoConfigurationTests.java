/*
 * Copyright 2012-present the original author or authors.
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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Locale;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.predicate.RuntimeHintsPredicates;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.MessageSourceAutoConfiguration.MessageSourceRuntimeHints;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.ContextConsumer;
import org.springframework.boot.testsupport.classpath.resources.WithResource;
import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceResolvable;
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
 * @author Marc Becker
 * @author Misagh Moayyed
 * @author Phillip Webb
 */
class MessageSourceAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(MessageSourceAutoConfiguration.class));

	@Test
	void testDefaultMessageSource() {
		this.contextRunner.run((context) -> assertThat(context.getMessage("foo", null, "Foo message", Locale.UK))
			.isEqualTo("Foo message"));
	}

	@Test
	@WithTestMessagesPropertiesResource
	void propertiesBundleWithSlashIsDetected() {
		this.contextRunner.withPropertyValues("spring.messages.basename=test/messages").run((context) -> {
			assertThat(context).hasSingleBean(MessageSource.class);
			assertThat(context.getMessage("foo", null, "Foo message", Locale.UK)).isEqualTo("bar");
		});
	}

	@Test
	@WithTestMessagesPropertiesResource
	void propertiesBundleWithDotIsDetected() {
		this.contextRunner.withPropertyValues("spring.messages.basename=test.messages").run((context) -> {
			assertThat(context).hasSingleBean(MessageSource.class);
			assertThat(context.getMessage("foo", null, "Foo message", Locale.UK)).isEqualTo("bar");
		});
	}

	@Test
	@WithTestSwedishPropertiesResource
	void testEncodingWorks() {
		this.contextRunner.withPropertyValues("spring.messages.basename=test/swedish")
			.run((context) -> assertThat(context.getMessage("foo", null, "Foo message", Locale.UK))
				.isEqualTo("Some text with some swedish öäå!"));
	}

	@Test
	@WithTestMessagesPropertiesResource
	void testCacheDurationNoUnit() {
		this.contextRunner
			.withPropertyValues("spring.messages.basename=test/messages", "spring.messages.cache-duration=10")
			.run(assertCache(10 * 1000));
	}

	@Test
	@WithTestMessagesPropertiesResource
	void testCacheDurationWithUnit() {
		this.contextRunner
			.withPropertyValues("spring.messages.basename=test/messages", "spring.messages.cache-duration=1m")
			.run(assertCache(60 * 1000));
	}

	private ContextConsumer<AssertableApplicationContext> assertCache(long expected) {
		return (context) -> {
			assertThat(context).hasSingleBean(MessageSource.class);
			assertThat(context.getBean(MessageSource.class)).hasFieldOrPropertyWithValue("cacheMillis", expected);
		};
	}

	@Test
	@WithTestMessagesPropertiesResource
	@WithTestMessages2PropertiesResource
	void testMultipleMessageSourceCreated() {
		this.contextRunner.withPropertyValues("spring.messages.basename=test/messages,test/messages2")
			.run((context) -> {
				assertThat(context.getMessage("foo", null, "Foo message", Locale.UK)).isEqualTo("bar");
				assertThat(context.getMessage("foo-foo", null, "Foo-Foo message", Locale.UK)).isEqualTo("bar-bar");
			});
	}

	@Test
	@WithTestMessagesPropertiesResource
	@Disabled("Expected to fail per gh-1075")
	@WithResource(name = "application-switch-messages.properties", content = "spring.messages.basename:test/messages")
	void testMessageSourceFromPropertySourceAnnotation() {
		this.contextRunner.withUserConfiguration(Config.class)
			.run((context) -> assertThat(context.getMessage("foo", null, "Foo message", Locale.UK)).isEqualTo("bar"));
	}

	@Test
	@WithTestMessagesPropertiesResource
	@WithResource(name = "test/common-messages.properties", content = "hello=world")
	void testCommonMessages() {
		this.contextRunner
			.withPropertyValues("spring.messages.basename=test/messages",
					"spring.messages.common-messages=classpath:test/common-messages.properties")
			.run((context) -> assertThat(context.getMessage("hello", null, "Hello!", Locale.UK)).isEqualTo("world"));
	}

	@Test
	@WithTestMessagesPropertiesResource
	void testCommonMessagesWhenNotFound() {
		this.contextRunner
			.withPropertyValues("spring.messages.basename=test/messages",
					"spring.messages.common-messages=classpath:test/common-messages.properties")
			.run((context) -> assertThat(context).getFailure()
				.hasMessageContaining(
						"Failed to load common messages from 'class path resource [test/common-messages.properties]'"));
	}

	@Test
	@WithTestMessagesPropertiesResource
	void testFallbackDefault() {
		this.contextRunner.withPropertyValues("spring.messages.basename=test/messages")
			.run((context) -> assertThat(context.getBean(MessageSource.class))
				.hasFieldOrPropertyWithValue("fallbackToSystemLocale", true));
	}

	@Test
	@WithTestMessagesPropertiesResource
	void testFallbackTurnOff() {
		this.contextRunner
			.withPropertyValues("spring.messages.basename=test/messages",
					"spring.messages.fallback-to-system-locale:false")
			.run((context) -> assertThat(context.getBean(MessageSource.class))
				.hasFieldOrPropertyWithValue("fallbackToSystemLocale", false));
	}

	@Test
	@WithTestMessagesPropertiesResource
	void testFormatMessageDefault() {
		this.contextRunner.withPropertyValues("spring.messages.basename=test/messages")
			.run((context) -> assertThat(context.getBean(MessageSource.class))
				.hasFieldOrPropertyWithValue("alwaysUseMessageFormat", false));
	}

	@Test
	@WithTestMessagesPropertiesResource
	void testFormatMessageOn() {
		this.contextRunner
			.withPropertyValues("spring.messages.basename=test/messages",
					"spring.messages.always-use-message-format:true")
			.run((context) -> assertThat(context.getBean(MessageSource.class))
				.hasFieldOrPropertyWithValue("alwaysUseMessageFormat", true));
	}

	@Test
	@WithTestMessagesPropertiesResource
	void testUseCodeAsDefaultMessageDefault() {
		this.contextRunner.withPropertyValues("spring.messages.basename=test/messages")
			.run((context) -> assertThat(context.getBean(MessageSource.class))
				.hasFieldOrPropertyWithValue("useCodeAsDefaultMessage", false));
	}

	@Test
	@WithTestMessagesPropertiesResource
	void testUseCodeAsDefaultMessageOn() {
		this.contextRunner
			.withPropertyValues("spring.messages.basename=test/messages",
					"spring.messages.use-code-as-default-message=true")
			.run((context) -> assertThat(context.getBean(MessageSource.class))
				.hasFieldOrPropertyWithValue("useCodeAsDefaultMessage", true));
	}

	@Test
	void existingMessageSourceIsPreferred() {
		this.contextRunner.withUserConfiguration(CustomMessageSourceConfiguration.class)
			.run((context) -> assertThat(context.getMessage("foo", null, null, null)).isEqualTo("foo"));
	}

	@Test
	@WithTestMessagesPropertiesResource
	void existingMessageSourceInParentIsIgnored() {
		this.contextRunner.run((parent) -> this.contextRunner.withParent(parent)
			.withPropertyValues("spring.messages.basename=test/messages")
			.run((context) -> assertThat(context.getMessage("foo", null, "Foo message", Locale.UK)).isEqualTo("bar")));
	}

	@Test
	@WithTestMessagesPropertiesResource
	void messageSourceWithNonStandardBeanNameIsIgnored() {
		this.contextRunner.withPropertyValues("spring.messages.basename=test/messages")
			.withUserConfiguration(CustomBeanNameMessageSourceConfiguration.class)
			.run((context) -> assertThat(context.getMessage("foo", null, Locale.US)).isEqualTo("bar"));
	}

	@Test
	void shouldRegisterDefaultHints() {
		RuntimeHints hints = new RuntimeHints();
		new MessageSourceRuntimeHints().registerHints(hints, getClass().getClassLoader());
		assertThat(RuntimeHintsPredicates.resource().forResource("messages.properties")).accepts(hints);
		assertThat(RuntimeHintsPredicates.resource().forResource("messages_de.properties")).accepts(hints);
		assertThat(RuntimeHintsPredicates.resource().forResource("messages_zh-CN.properties")).accepts(hints);
	}

	@Configuration(proxyBeanMethods = false)
	@PropertySource("classpath:/switch-messages.properties")
	static class Config {

	}

	@Configuration(proxyBeanMethods = false)
	static class CustomMessageSourceConfiguration {

		@Bean
		MessageSource messageSource() {
			return new TestMessageSource();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class CustomBeanNameMessageSourceConfiguration {

		@Bean
		MessageSource codeReturningMessageSource() {
			return new TestMessageSource();
		}

	}

	static class TestMessageSource implements MessageSource {

		@Override
		public String getMessage(String code, Object[] args, String defaultMessage, Locale locale) {
			return code;
		}

		@Override
		public String getMessage(String code, Object[] args, Locale locale) {
			return code;
		}

		@Override
		public String getMessage(MessageSourceResolvable resolvable, Locale locale) {
			return resolvable.getCodes()[0];
		}

	}

	@Target(ElementType.METHOD)
	@Retention(RetentionPolicy.RUNTIME)
	@WithResource(name = "test/messages.properties", content = "foo=bar")
	@interface WithTestMessagesPropertiesResource {

	}

	@Target(ElementType.METHOD)
	@Retention(RetentionPolicy.RUNTIME)
	@WithResource(name = "test/messages2.properties", content = "foo-foo=bar-bar")
	@interface WithTestMessages2PropertiesResource {

	}

	@Target(ElementType.METHOD)
	@Retention(RetentionPolicy.RUNTIME)
	@WithResource(name = "test/swedish.properties", content = "foo=Some text with some swedish öäå!")
	@interface WithTestSwedishPropertiesResource {

	}

}
