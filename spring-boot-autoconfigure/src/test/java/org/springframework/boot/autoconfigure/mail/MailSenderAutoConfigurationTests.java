/*
 * Copyright 2012-2018 the original author or authors.
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

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.naming.Context;
import javax.naming.NamingException;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.boot.autoconfigure.jndi.JndiPropertiesHidingClassLoader;
import org.springframework.boot.autoconfigure.jndi.TestableInitialContextFactory;
import org.springframework.boot.test.util.EnvironmentTestUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link MailSenderAutoConfiguration}.
 *
 * @author Stephane Nicoll
 * @author Eddú Meléndez
 */
public class MailSenderAutoConfigurationTests {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private ClassLoader threadContextClassLoader;

	private String initialContextFactory;

	private AnnotationConfigApplicationContext context;

	@Before
	public void setupJndi() {
		this.initialContextFactory = System.getProperty(Context.INITIAL_CONTEXT_FACTORY);
		System.setProperty(Context.INITIAL_CONTEXT_FACTORY,
				TestableInitialContextFactory.class.getName());
	}

	@Before
	public void setupThreadContextClassLoader() {
		this.threadContextClassLoader = Thread.currentThread().getContextClassLoader();
		Thread.currentThread().setContextClassLoader(
				new JndiPropertiesHidingClassLoader(getClass().getClassLoader()));
	}

	@After
	public void close() {
		TestableInitialContextFactory.clearAll();
		if (this.initialContextFactory != null) {
			System.setProperty(Context.INITIAL_CONTEXT_FACTORY,
					this.initialContextFactory);
		}
		else {
			System.clearProperty(Context.INITIAL_CONTEXT_FACTORY);
		}
		if (this.context != null) {
			this.context.close();
		}
		Thread.currentThread().setContextClassLoader(this.threadContextClassLoader);
	}

	@Test
	public void smtpHostSet() {
		String host = "192.168.1.234";
		load(EmptyConfig.class, "spring.mail.host:" + host);
		JavaMailSenderImpl bean = (JavaMailSenderImpl) this.context
				.getBean(JavaMailSender.class);
		assertThat(bean.getHost()).isEqualTo(host);
		assertThat(bean.getPort()).isEqualTo(JavaMailSenderImpl.DEFAULT_PORT);
		assertThat(bean.getProtocol()).isEqualTo(JavaMailSenderImpl.DEFAULT_PROTOCOL);
	}

	@Test
	public void smtpHostWithSettings() {
		String host = "192.168.1.234";
		load(EmptyConfig.class, "spring.mail.host:" + host, "spring.mail.port:42",
				"spring.mail.username:john", "spring.mail.password:secret",
				"spring.mail.default-encoding:US-ASCII", "spring.mail.protocol:smtps");
		JavaMailSenderImpl bean = (JavaMailSenderImpl) this.context
				.getBean(JavaMailSender.class);
		assertThat(bean.getHost()).isEqualTo(host);
		assertThat(bean.getPort()).isEqualTo(42);
		assertThat(bean.getUsername()).isEqualTo("john");
		assertThat(bean.getPassword()).isEqualTo("secret");
		assertThat(bean.getDefaultEncoding()).isEqualTo("US-ASCII");
		assertThat(bean.getProtocol()).isEqualTo("smtps");
	}

	@Test
	public void smtpHostWithJavaMailProperties() {
		load(EmptyConfig.class, "spring.mail.host:localhost",
				"spring.mail.properties.mail.smtp.auth:true");
		JavaMailSenderImpl bean = (JavaMailSenderImpl) this.context
				.getBean(JavaMailSender.class);
		assertThat(bean.getJavaMailProperties().get("mail.smtp.auth")).isEqualTo("true");
	}

	@Test
	public void smtpHostNotSet() {
		load(EmptyConfig.class);
		assertThat(this.context.getBeansOfType(JavaMailSender.class)).isEmpty();
	}

	@Test
	public void mailSenderBackOff() {
		load(ManualMailConfiguration.class, "spring.mail.host:smtp.acme.org",
				"spring.mail.user:user", "spring.mail.password:secret");
		JavaMailSenderImpl bean = (JavaMailSenderImpl) this.context
				.getBean(JavaMailSender.class);
		assertThat(bean.getUsername()).isNull();
		assertThat(bean.getPassword()).isNull();
	}

	@Test
	public void jndiSessionAvailable() throws NamingException {
		Session session = configureJndiSession("foo");
		load(EmptyConfig.class, "spring.mail.jndi-name:foo");
		Session sessionBean = this.context.getBean(Session.class);
		assertThat(sessionBean).isEqualTo(session);
		assertThat(this.context.getBean(JavaMailSenderImpl.class).getSession())
				.isEqualTo(sessionBean);
	}

	@Test
	public void jndiSessionIgnoredIfJndiNameNotSet() throws NamingException {
		configureJndiSession("foo");
		load(EmptyConfig.class, "spring.mail.host:smtp.acme.org");
		assertThat(this.context.getBeanNamesForType(Session.class).length).isEqualTo(0);
		assertThat(this.context.getBean(JavaMailSender.class)).isNotNull();
	}

	@Test
	public void jndiSessionNotUsedIfJndiNameNotSet() throws NamingException {
		configureJndiSession("foo");
		load(EmptyConfig.class);
		assertThat(this.context.getBeanNamesForType(Session.class).length).isEqualTo(0);
		assertThat(this.context.getBeanNamesForType(JavaMailSender.class).length)
				.isEqualTo(0);
	}

	@Test
	public void jndiSessionNotAvailableWithJndiName() throws NamingException {
		this.thrown.expect(BeanCreationException.class);
		this.thrown.expectMessage("Unable to find Session in JNDI location foo");
		load(EmptyConfig.class, "spring.mail.jndi-name:foo");
	}

	@Test
	public void jndiSessionTakesPrecedenceOverProperties() throws NamingException {
		Session session = configureJndiSession("foo");
		load(EmptyConfig.class, "spring.mail.jndi-name:foo",
				"spring.mail.host:localhost");
		Session sessionBean = this.context.getBean(Session.class);
		assertThat(sessionBean).isEqualTo(session);
		assertThat(this.context.getBean(JavaMailSenderImpl.class).getSession())
				.isEqualTo(sessionBean);
	}

	@Test
	public void defaultEncodingWithProperties() {
		load(EmptyConfig.class, "spring.mail.host:localhost",
				"spring.mail.default-encoding:UTF-16");
		JavaMailSenderImpl bean = (JavaMailSenderImpl) this.context
				.getBean(JavaMailSender.class);
		assertThat(bean.getDefaultEncoding()).isEqualTo("UTF-16");
	}

	@Test
	public void defaultEncodingWithJndi() throws NamingException {
		configureJndiSession("foo");
		load(EmptyConfig.class, "spring.mail.jndi-name:foo",
				"spring.mail.default-encoding:UTF-16");
		JavaMailSenderImpl bean = (JavaMailSenderImpl) this.context
				.getBean(JavaMailSender.class);
		assertThat(bean.getDefaultEncoding()).isEqualTo("UTF-16");
	}

	@Test
	public void connectionOnStartup() throws MessagingException {
		load(MockMailConfiguration.class, "spring.mail.host:10.0.0.23",
				"spring.mail.test-connection:true");
		JavaMailSenderImpl mailSender = this.context.getBean(JavaMailSenderImpl.class);
		verify(mailSender, times(1)).testConnection();
	}

	@Test
	public void connectionOnStartupNotCalled() throws MessagingException {
		load(MockMailConfiguration.class, "spring.mail.host:10.0.0.23",
				"spring.mail.test-connection:false");
		JavaMailSenderImpl mailSender = this.context.getBean(JavaMailSenderImpl.class);
		verify(mailSender, never()).testConnection();
	}

	private Session configureJndiSession(String name)
			throws IllegalStateException, NamingException {
		Properties properties = new Properties();
		Session session = Session.getDefaultInstance(properties);
		TestableInitialContextFactory.bind(name, session);
		return session;
	}

	private void load(Class<?> config, String... environment) {
		this.context = doLoad(new Class<?>[] { config }, environment);
	}

	private AnnotationConfigApplicationContext doLoad(Class<?>[] configs,
			String... environment) {
		AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext();
		EnvironmentTestUtils.addEnvironment(applicationContext, environment);
		applicationContext.register(configs);
		applicationContext.register(MailSenderAutoConfiguration.class);
		applicationContext.register(MailSenderValidatorAutoConfiguration.class);
		applicationContext.refresh();
		return applicationContext;
	}

	@Configuration
	static class EmptyConfig {

	}

	@Configuration
	static class ManualMailConfiguration {

		@Bean
		JavaMailSender customMailSender() {
			return new JavaMailSenderImpl();
		}

	}

	@Configuration
	static class MockMailConfiguration {

		@Bean
		JavaMailSenderImpl mockMailSender() {
			return mock(JavaMailSenderImpl.class);
		}

	}

}
