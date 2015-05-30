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

package org.springframework.boot.autoconfigure.mail;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.boot.autoconfigure.jndi.JndiPropertiesHidingClassLoader;
import org.springframework.boot.autoconfigure.jndi.TestableInitialContextFactory;
import org.springframework.boot.test.EnvironmentTestUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import javax.mail.Session;
import javax.naming.Context;
import javax.naming.NamingException;

import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

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
		} else {
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
		assertEquals(host, bean.getHost());
	}

	@Test
	public void smptHostWithSettings() {
		String host = "192.168.1.234";
		load(EmptyConfig.class, "spring.mail.host:" + host, "spring.mail.port:42",
				"spring.mail.username:john", "spring.mail.password:secret",
				"spring.mail.default-encoding:ISO-9");
		JavaMailSenderImpl bean = (JavaMailSenderImpl) this.context
				.getBean(JavaMailSender.class);
		assertEquals(host, bean.getHost());
		assertEquals(42, bean.getPort());
		assertEquals("john", bean.getUsername());
		assertEquals("secret", bean.getPassword());
		assertEquals("ISO-9", bean.getDefaultEncoding());
	}

	@Test
	public void smptHostWithJavaMailProperties() {
		load(EmptyConfig.class, "spring.mail.host:localhost",
				"spring.mail.properties.mail.smtp.auth:true");
		JavaMailSenderImpl bean = (JavaMailSenderImpl) this.context
				.getBean(JavaMailSender.class);
		assertEquals("true", bean.getJavaMailProperties().get("mail.smtp.auth"));
	}

	@Test
	public void smtpHostNotSet() {
		load(EmptyConfig.class);
		assertEquals(0, this.context.getBeansOfType(JavaMailSender.class).size());
	}

	@Test
	public void mailSenderBackOff() {
		load(ManualMailConfiguration.class, "spring.mail.host:smtp.acme.org",
				"spring.mail.user:user", "spring.mail.password:secret");
		JavaMailSenderImpl bean = (JavaMailSenderImpl) this.context
				.getBean(JavaMailSender.class);
		assertNull(bean.getUsername());
		assertNull(bean.getPassword());
	}

	@Test
	public void sessionIsAvailableFromJndiAndJndiNameIsSet() throws IllegalStateException,
			NamingException {
		Properties properties = new Properties();
		Session session = Session.getDefaultInstance(properties);
		configureJndi("foo", session);

		load(JndiMailAutoConfiguration.class, "spring.mail.jndi-name:foo");

		assertNotNull(this.context.getBean(Session.class));
		assertEquals(session, this.context.getBean(Session.class));
		assertNotNull(this.context.getBean(JavaMailSender.class));
	}

	@Test
	public void sessionIsAvailableFromJndiAndHostIsSet() throws IllegalStateException,
			NamingException {
		Properties properties = new Properties();
		Session session = Session.getDefaultInstance(properties);
		configureJndi("foo", session);

		load(JndiMailAutoConfiguration.class, "spring.mail.host:smtp.acme.org");

		assertEquals(0, this.context.getBeanNamesForType(Session.class).length);
		assertNotNull(this.context.getBean(JavaMailSender.class));
	}

	@Test
	public void sessionIsAvailableFromJndiAndPropertiesNotUsed() throws IllegalStateException,
			NamingException {
		Properties properties = new Properties();
		Session session = Session.getDefaultInstance(properties);
		configureJndi("foo", session);

		load(JndiMailAutoConfiguration.class);

		assertEquals(0, this.context.getBeanNamesForType(Session.class).length);
		assertEquals(0,  this.context.getBeanNamesForType(JavaMailSender.class).length);
	}

	@Test
	public void sessionIsNotAvailableFromJndiAndJndiNameIsSet() throws IllegalStateException,
			NamingException {
		thrown.expect(BeanCreationException.class);
		thrown.expectMessage("Unable to find Session in JNDI location foo");

		load(JndiMailAutoConfiguration.class, "spring.mail.jndi-name:foo");
	}

	private void configureJndi(String name, Session session)
			throws IllegalStateException, NamingException {
		TestableInitialContextFactory.bind(name, session);
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

}
