/*
 * Copyright 2012-2021 the original author or authors.
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
import java.util.LinkedHashSet;
import java.util.Set;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

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

	/**
     * Registers a servlet for handling resource requests.
     * 
     * @return the ServletRegistrationBean for the resource servlet
     */
    @Bean
	public ServletRegistrationBean<?> resourceServletRegistration() {
		ServletRegistrationBean<?> registration = new ServletRegistrationBean<HttpServlet>(new GetResourceServlet());
		registration.addUrlMappings("/servletContext");
		return registration;
	}

	/**
     * Registers the GetResourcePathsServlet as a servlet and maps it to the "/resourcePaths" URL.
     * 
     * @return The ServletRegistrationBean for the GetResourcePathsServlet.
     */
    @Bean
	public ServletRegistrationBean<?> resourcePathsServletRegistration() {
		ServletRegistrationBean<?> registration = new ServletRegistrationBean<HttpServlet>(
				new GetResourcePathsServlet());
		registration.addUrlMappings("/resourcePaths");
		return registration;
	}

	/**
     * The main method of the ResourceHandlingApplication class.
     * 
     * This method is the entry point of the application. It checks if the Spring MVC framework is present. If it is, an error message is printed and the application exits. If it is not present, the application is started using the embedded container's static resource handling.
     * 
     * @param args The command line arguments passed to the application.
     */
    public static void main(String[] args) {
		try {
			Class.forName("org.springframework.web.servlet.DispatcherServlet");
			System.err.println("Spring MVC must not be present, otherwise its static resource handling "
					+ "will be used rather than the embedded containers'");
			System.exit(1);
		}
		catch (Throwable ex) {
			new SpringApplicationBuilder(ResourceHandlingApplication.class).properties("server.port:0")
					.listeners(new WebServerPortFileWriter(args[0])).run(args);
		}
	}

	/**
     * GetResourcePathsServlet class.
     */
    private static final class GetResourcePathsServlet extends HttpServlet {

		/**
         * This method is called when a GET request is made to the servlet.
         * It retrieves the resource paths for the specified directory and writes them to the response.
         * 
         * @param req The HttpServletRequest object representing the request made to the servlet.
         * @param resp The HttpServletResponse object representing the response to be sent back to the client.
         * @throws ServletException If there is a servlet-related problem.
         * @throws IOException If there is an I/O problem.
         */
        @Override
		protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
			collectResourcePaths("/").forEach(resp.getWriter()::println);
			resp.getWriter().flush();
		}

		/**
         * Recursively collects all resource paths under the given path.
         * 
         * @param path the path to collect resource paths from
         * @return a set of all resource paths found under the given path
         */
        private Set<String> collectResourcePaths(String path) {
			Set<String> allResourcePaths = new LinkedHashSet<>();
			Set<String> pathsForPath = getServletContext().getResourcePaths(path);
			if (pathsForPath != null) {
				for (String resourcePath : pathsForPath) {
					allResourcePaths.add(resourcePath);
					allResourcePaths.addAll(collectResourcePaths(resourcePath));
				}
			}
			return allResourcePaths;
		}

	}

	/**
     * GetResourceServlet class.
     */
    private static final class GetResourceServlet extends HttpServlet {

		/**
         * This method is called when a GET request is made to the servlet.
         * It retrieves the resource specified in the query string and sends it back as a response.
         * If the resource is not found, a 404 error is sent.
         *
         * @param req  the HttpServletRequest object representing the request made by the client
         * @param resp the HttpServletResponse object representing the response to be sent back to the client
         * @throws ServletException if there is a servlet-related problem
         * @throws IOException      if there is an I/O problem
         */
        @Override
		protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
			URL resource = getServletContext().getResource(req.getQueryString());
			if (resource == null) {
				resp.sendError(404);
			}
			else {
				resp.getWriter().println(resource);
				resp.getWriter().flush();
			}
		}

	}

}
