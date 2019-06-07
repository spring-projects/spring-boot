/*
 * Copyright 2012-2019 the original author or authors.
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

package com.example;

import java.io.IOException;
import java.net.URL;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.context.WebServerPortFileWriter;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;

/**
 * Test application for verifying an embedded container's static resource handling.
 *
 * @author Andy Wilkinson
 */
@SpringBootApplication
public class ResourceHandlingApplication {

	public static void main(String[] args) {
		new SpringApplicationBuilder(ResourceHandlingApplication.class).properties("server.port:0")
				.listeners(new WebServerPortFileWriter("target/server.port")).run(args);
	}

	@Bean
	public ServletRegistrationBean<?> resourceServletRegistration() {
		ServletRegistrationBean<?> registration = new ServletRegistrationBean<HttpServlet>(new HttpServlet() {

			@Override
			protected void doGet(HttpServletRequest req, HttpServletResponse resp)
					throws ServletException, IOException {
				URL resource = getServletContext().getResource(req.getQueryString());
				if (resource == null) {
					resp.sendError(404);
				}
				else {
					resp.getWriter().println(resource);
					resp.getWriter().flush();
				}
			}

		});
		registration.addUrlMappings("/servletContext");
		return registration;
	}

}
