/*
 * Copyright 2012-2023 the original author or authors.
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

	/**
     * Constructs a new ExampleServlet with the specified parameters.
     * 
     * @param parameter1 a boolean value indicating the value of the first parameter
     * @param parameter2 a boolean value indicating the value of the second parameter
     */
    public ExampleServlet() {
		this(false, false);
	}

	/**
     * Constructs a new ExampleServlet with the specified parameters.
     * 
     * @param echoRequestInfo
     *            a boolean indicating whether to echo the request information
     * @param writeWithoutContentLength
     *            a boolean indicating whether to write without content length
     */
    public ExampleServlet(boolean echoRequestInfo, boolean writeWithoutContentLength) {
		this.echoRequestInfo = echoRequestInfo;
		this.writeWithoutContentLength = writeWithoutContentLength;
	}

	/**
     * This method is responsible for handling the service request and generating the response.
     * It takes in a ServletRequest object and a ServletResponse object as parameters.
     * It throws ServletException and IOException.
     * 
     * The method first initializes a String variable "content" with the value "Hello World".
     * If the boolean variable "echoRequestInfo" is true, it appends additional information to the "content" string.
     * This additional information includes the scheme and remote address obtained from the request object.
     * 
     * If the boolean variable "writeWithoutContentLength" is true, it sets the content type of the response to "text/plain".
     * It then obtains the output stream from the response object and writes the content string to it.
     * Finally, it flushes the output stream.
     * 
     * If the boolean variable "writeWithoutContentLength" is false, it obtains the writer from the response object and writes the content string to it.
     * 
     * @param request The ServletRequest object representing the incoming request.
     * @param response The ServletResponse object representing the response to be generated.
     * @throws ServletException If an exception occurs during the servlet processing.
     * @throws IOException If an I/O exception occurs.
     */
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
