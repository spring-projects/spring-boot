/*
 * Copyright 2012-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.context.embedded;

import java.io.IOException;

import javax.servlet.GenericServlet;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

/**
 * Simple example Servlet used for testing.
 *
 * @author Phillip Webb
 */
@SuppressWarnings("serial")
public class ExampleServlet extends GenericServlet {

	private final boolean echoRequestInfo;

	public ExampleServlet() {
		this(false);
	}

	public ExampleServlet(boolean echoRequestInfo) {
		this.echoRequestInfo = echoRequestInfo;
	}

	@Override
	public void service(ServletRequest request, ServletResponse response)
			throws ServletException, IOException {
		String content = "Hello World";
		if (this.echoRequestInfo) {
			content += " scheme=" + request.getScheme();
		}
		response.getWriter().write(content);
	}

}
