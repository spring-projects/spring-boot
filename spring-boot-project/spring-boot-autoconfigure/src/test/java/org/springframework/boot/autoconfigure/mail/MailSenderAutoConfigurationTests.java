/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot.autoconfigure.mail;

import java.util.Properties;

import javax.mail.Session;
import javax.naming.Context;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jndi.JndiPropertiesHidingClassLoader;
import org.springframework.boot.autoconfigure.jndi.TestableInitialContextFactory;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.MailSender;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

/**
 * Tests for {@link MailSenderAutoConfiguration}.
 *
 * @author Stephane Nicoll
 * @author Eddú Meléndez
 */
class MailSenderAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner().withConfiguration(
			AutoConfigurations.of(MailSenderAutoConfiguration.class, MailSenderValidatorAutoConfiguration.class));

	private ClassLoader threadContextClassLoader;

	private String initialContextFactory;

	@BeforeEach
	void setupJndi() {
		this.initialContextFactory = System.getProperty(Context.INITIAL_CONTEXT_FACTORY);
		System.setProperty(Context.INITIAL_CONTEXT_FACTORY, TestableInitialContextFactory.class.getName());
		this.threadContextClassLoader = Thread.currentThread().getContextClassLoader();
		Thread.currentThread().setContextClassLoader(new JndiPropertiesHidingClassLoader(getClass().getClassLoader()));
	}

	@AfterEach
	void close() {
		TestableInitialContextFactory.clearAll();
		if (this.initialContextFactory != null) {
			System.setProperty(Context.INITIAL_CONTEXT_FACTORY, this.initialContextFactory);
		}
		else {
			System.clearProperty(Context.INITIAL_CONTEXT_FACTORY);
		}
		Thread.currentThread().setContextClassLoader(this.threadContextClassLoader);
	}

	@Test
	void smtpHostSet() {
		String host = "192.168.1.234";
		this.contextRunner.withPropertyValues("spring.mail.host:" + host).run((context) -> {
			assertThat(context).hasSingleBean(JavaMailSenderImpl.class);
			JavaMailSenderImpl mailSender = context.getBean(JavaMailSenderImpl.class);
			assertThat(mailSender.getHost()).isEqualTo(host);
			assertThat(mailSender.getPort()).isEqualTo(JavaMailSenderImpl.DEFAULT_PORT);
			assertThat(mailSender.getProtocol()).isEqualTo(JavaMailSenderImpl.DEFAULT_PROTOCOL);
		});
	}

	@Test
	void smtpHostWithSettings() {
		String host = "192.168.1.234";
		this.contextRunner.withPropertyValues("spring.mail.host:" + host, "spring.mail.port:42",
				"spring.mail.username:john", "spring.mail.password:secret", "spring.mail.default-encoding:US-ASCII",
				"spring.mail.protocol:smtps").run((context) -> {
					assertThat(context).hasSingleBean(JavaMailSenderImpl.class);
					JavaMailSenderImpl mailSender = context.getBean(JavaMailSenderImpl.class);
					assertThat(mailSender.getHost()).isEqualTo(host);
					assertThat(mailSender.getPort()).isEqualTo(42);
					assertThat(mailSender.getUsername()).isEqualTo("john");
					assertThat(mailSender.getPassword()).isEqualTo("secret");
					assertThat(mailSender.getDefaultEncoding()).isEqualTo("US-ASCII");
					assertThat(mailSender.getProtocol()).isEqualTo("smtps");
				});
	}

	@Test
	void smtpHostWithJavaMailProperties() {
		this.contextRunner
				.withPropertyValues("spring.mail.host:localhost", "spring.mail.properties.mail.smtp.auth:true")
				.run((context) -> {
					assertThat(context).hasSingleBean(JavaMailSenderImpl.class);
					JavaMailSenderImpl mailSender = context.getBean(JavaMailSenderImpl.class);
					assertThat(mailSender.getJavaMailProperties().get("mail.smtp.auth")).isEqualTo("true");
				});
	}

	@Test
	void smtpHostNotSet() {
		this.contextRunner.run((context) -> assertThat(context).doesNotHaveBean(MailSender.class));
	}

	@Test
	void mailSenderBackOff() {
		this.contextRunner.withUserConfiguration(ManualMailConfiguration.class)
				.withPropertyValues("spring.mail.host:smtp.acme.org", "spring.mail.user:user",
						"spring.mail.password:secret")
				.run((context) -> {
					assertThat(context).hasSingleBean(JavaMailSenderImpl.class);
					JavaMailSenderImpl mailSender = context.getBean(JavaMailSenderImpl.class);
					assertThat(mailSender.getUsername()).isNull();
					assertThat(mailSender.getPassword()).isNull();
				});
	}

	@Test
	void jndiSessionAvailable() {
		Session session = configureJndiSession("java:comp/env/foo");
		testJndiSessionLookup(session, "java:comp/env/foo");
	}

	@Test
	void jndiSessionAvailableWithResourceRef() {
		Session session = configureJndiSession("java:comp/env/foo");
		testJndiSessionLookup(session, "foo");
	}

	private void testJndiSessionLookup(Session session, String jndiName) {
		this.contextRunner.withPropertyValues("spring.mail.jndi-name:" + jndiName).run((context) -> {
			assertThat(context).hasSingleBean(Session.class);
			Session sessionBean = context.getBean(Session.class);
			assertThat(context).hasSingleBean(JavaMailSenderImpl.class);
			assertThat(sessionBean).isEqualTo(session);
			assertThat(context.getBean(JavaMailSenderImpl.class).getSession()).isEqualTo(sessionBean);
		});
	}

	@Test
	void jndiSessionIgnoredIfJndiNameNotSet() {
		configureJndiSession("foo");
		this.contextRunner.withPropertyValues("spring.mail.host:smtp.acme.org").run((context) -> {
			assertThat(context).doesNotHaveBean(Session.class);
			assertThat(context).hasSingleBean(JavaMailSenderImpl.class);
		});
	}

	@Test
	void jndiSessionNotUsedIfJndiNameNotSet() {
		configureJndiSession("foo");
		this.contextRunner.run((context) -> {
			assertThat(context).doesNotHaveBean(Session.class);
			assertThat(context).doesNotHaveBean(MailSender.class);
		});
	}

	@Test
	void jndiSessionNotAvailableWithJndiName() {
		this.contextRunner.withPropertyValues("spring.mail.jndi-name:foo").run((context) -> {
			assertThat(context).hasFailed();
			assertThat(context.getStartupFailure()).isInstanceOf(BeanCreationException.class)
					.hasMessageContaining("Unable to find Session in JNDI location foo");
		});
	}

	@Test
	void jndiSessionTakesPrecedenceOverProperties() {
		Session session = configureJndiSession("foo");
		this.contextRunner.withPropertyValues("spring.mail.jndi-name:foo", "spring.mail.host:localhost")
				.run((context) -> {
					assertThat(context).hasSingleBean(Session.class);
					Session sessionBean = context.getBean(Session.class);
					assertThat(sessionBean).isEqualTo(session);
					assertThat(context.getBean(JavaMailSenderImpl.class).getSession()).isEqualTo(sessionBean);
				});
	}

	@Test
	void defaultEncodingWithProperties() {
		this.contextRunner.withPropertyValues("spring.mail.host:localhost", "spring.mail.default-encoding:UTF-16")
				.run((context) -> {
					assertThat(context).hasSingleBean(JavaMailSenderImpl.class);
					JavaMailSenderImpl mailSender = context.getBean(JavaMailSenderImpl.class);
					assertThat(mailSender.getDefaultEncoding()).isEqualTo("UTF-16");
				});
	}

	@Test
	void defaultEncodingWithJndi() {
		configureJndiSession("foo");
		this.contextRunner.withPropertyValues("spring.mail.jndi-name:foo", "spring.mail.default-encoding:UTF-16")
				.run((context) -> {
					assertThat(context).hasSingleBean(JavaMailSenderImpl.class);
					JavaMailSenderImpl mailSender = context.getBean(JavaMailSenderImpl.class);
					assertThat(mailSender.getDefaultEncoding()).isEqualTo("UTF-16");
				});
	}

	@Test
	void connectionOnStartup() {
		this.contextRunner.withUserConfiguration(MockMailConfiguration.class)
				.withPropertyValues("spring.mail.host:10.0.0.23", "spring.mail.test-connection:true").run((context) -> {
					assertThat(context).hasSingleBean(JavaMailSenderImpl.class);
					JavaMailSenderImpl mailSender = context.getBean(JavaMailSenderImpl.class);
					then(mailSender).should().testConnection();
				});
	}

	@Test
	void connectionOnStartupNotCalled() {
		this.contextRunner.withUserConfiguration(MockMailConfiguration.class)
				.withPropertyValues("spring.mail.host:10.0.0.23", "spring.mail.test-connection:false")
				.run((context) -> {
					assertThat(context).hasSingleBean(JavaMailSenderImpl.class);
					JavaMailSenderImpl mailSender = context.getBean(JavaMailSenderImpl.class);
					then(mailSender).should(never()).testConnection();
				});
	}

	private Session configureJndiSession(String name) {
		Properties properties = new Properties();
		Session session = Session.getDefaultInstance(properties);
		TestableInitialContextFactory.bind(name, session);
		return session;
	}

	@Configuration(proxyBeanMethods = false)
	static class ManualMailConfiguration {

		@Bean
		JavaMailSender customMailSender() {
			return new JavaMailSenderImpl();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class MockMailConfiguration {

		@Bean
		JavaMailSenderImpl mockMailSender() {
			return mock(JavaMailSenderImpl.class);
		}

	}

}
