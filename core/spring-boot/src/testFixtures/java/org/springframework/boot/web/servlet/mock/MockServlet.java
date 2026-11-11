/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.web.servlet.mock;

import java.io.IOException;

import jakarta.servlet.GenericServlet;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;

/**
 * Simple mock Servlet that does nothing.
 *
 * @author Phillip Webb
 */
public class MockServlet extends GenericServlet {

	@Override
	public void service(ServletRequest req, ServletResponse res) throws ServletException, IOException {
	}

}
