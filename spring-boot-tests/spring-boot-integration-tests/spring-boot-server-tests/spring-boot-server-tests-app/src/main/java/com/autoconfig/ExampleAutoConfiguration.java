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

package com.autoconfig;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWarDeployment;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;

/**
 * ExampleAutoConfiguration class.
 */
@AutoConfiguration
public class ExampleAutoConfiguration {

	/**
     * Registers a servlet for handling requests when the application is deployed as a WAR file.
     * This method is conditionally executed based on the presence of the @ConditionalOnWarDeployment annotation.
     * The servlet is registered with the specified URL mapping "/conditionalOnWar".
     *
     * @return the ServletRegistrationBean instance for the registered servlet
     */
    @Bean
	@ConditionalOnWarDeployment
	public ServletRegistrationBean<TestServlet> onWarTestServlet() {
		ServletRegistrationBean<TestServlet> registration = new ServletRegistrationBean<>(new TestServlet());
		registration.addUrlMappings("/conditionalOnWar");
		return registration;
	}

	/**
     * Registers the TestServlet with the servlet container.
     * 
     * @return the ServletRegistrationBean<TestServlet> object representing the registration of the TestServlet
     */
    @Bean
	public ServletRegistrationBean<TestServlet> testServlet() {
		ServletRegistrationBean<TestServlet> registration = new ServletRegistrationBean<>(new TestServlet());
		registration.addUrlMappings("/always");
		return registration;
	}

	/**
     * TestServlet class.
     */
    static class TestServlet extends HttpServlet {

		/**
         * This method is used to handle HTTP GET requests.
         * It sets the content type of the response to JSON and writes a JSON object to the response writer.
         * The JSON object contains a key-value pair with "hello" as the key and "world" as the value.
         * Finally, it flushes the response buffer.
         *
         * @param req  the HttpServletRequest object representing the request
         * @param resp the HttpServletResponse object representing the response
         * @throws ServletException if there is a servlet-related problem
         * @throws IOException      if there is an I/O problem
         */
        @Override
		protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
			resp.setContentType(MediaType.APPLICATION_JSON_VALUE);
			resp.getWriter().println("{\"hello\":\"world\"}");
			resp.flushBuffer();
		}

	}

}
