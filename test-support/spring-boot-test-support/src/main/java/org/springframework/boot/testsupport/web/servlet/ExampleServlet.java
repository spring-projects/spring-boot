/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.testsupport.web.servlet;

import java.io.IOException;

import jakarta.servlet.GenericServlet;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;

import org.springframework.util.StreamUtils;

/**
 * Simple example Servlet used for testing.
 *
 * @author Phillip Webb
 */
@SuppressWarnings("serial")
public class ExampleServlet extends GenericServlet {

	private final boolean echoRequestInfo;

	private final boolean writeWithoutContentLength;

	public ExampleServlet() {
		this(false, false);
	}

	public ExampleServlet(boolean echoRequestInfo, boolean writeWithoutContentLength) {
		this.echoRequestInfo = echoRequestInfo;
		this.writeWithoutContentLength = writeWithoutContentLength;
	}

	@Override
	public void service(ServletRequest request, ServletResponse response) throws ServletException, IOException {
		String content = "Hello World";
		if (this.echoRequestInfo) {
			content += " scheme=" + request.getScheme();
			content += " remoteaddr=" + request.getRemoteAddr();
		}
		if (this.writeWithoutContentLength) {
			response.setContentType("text/plain");
			ServletOutputStream outputStream = response.getOutputStream();
			StreamUtils.copy(content.getBytes(), outputStream);
			outputStream.flush();
		}
		else {
			response.getWriter().write(content);
		}
	}

}
