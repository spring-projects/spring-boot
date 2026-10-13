/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.web.server.servlet.context;

import jakarta.servlet.Servlet;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import org.springframework.boot.web.server.servlet.MockServletWebServerFactory;
import org.springframework.core.io.ClassPathResource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;

/**
 * Tests for {@link XmlServletWebServerApplicationContext}.
 *
 * @author Phillip Webb
 */
class XmlServletWebServerApplicationContextTests {

	private static final String PATH = XmlServletWebServerApplicationContextTests.class.getPackage()
		.getName()
		.replace('.', '/') + "/";

	private static final String FILE = "exampleEmbeddedWebApplicationConfiguration.xml";

	private @Nullable XmlServletWebServerApplicationContext context;

	@Test
	void createFromResource() {
		this.context = new XmlServletWebServerApplicationContext(new ClassPathResource(FILE, getClass()));
		verifyContext();
	}

	@Test
	void createFromResourceLocation() {
		this.context = new XmlServletWebServerApplicationContext(PATH + FILE);
		verifyContext();
	}

	@Test
	void createFromRelativeResourceLocation() {
		this.context = new XmlServletWebServerApplicationContext(getClass(), FILE);
		verifyContext();
	}

	@Test
	void loadAndRefreshFromResource() {
		this.context = new XmlServletWebServerApplicationContext();
		this.context.load(new ClassPathResource(FILE, getClass()));
		this.context.refresh();
		verifyContext();
	}

	@Test
	void loadAndRefreshFromResourceLocation() {
		this.context = new XmlServletWebServerApplicationContext();
		this.context.load(PATH + FILE);
		this.context.refresh();
		verifyContext();
	}

	@Test
	void loadAndRefreshFromRelativeResourceLocation() {
		this.context = new XmlServletWebServerApplicationContext();
		this.context.load(getClass(), FILE);
		this.context.refresh();
		verifyContext();
	}

	private void verifyContext() {
		assertThat(this.context).isNotNull();
		MockServletWebServerFactory factory = this.context.getBean(MockServletWebServerFactory.class);
		Servlet servlet = this.context.getBean(Servlet.class);
		then(factory.getServletContext()).should().addServlet("servlet", servlet);
	}

}
