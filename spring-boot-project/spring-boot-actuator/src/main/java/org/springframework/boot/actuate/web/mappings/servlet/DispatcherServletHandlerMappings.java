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

package org.springframework.boot.actuate.web.mappings.servlet;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import jakarta.servlet.ServletException;
import org.apache.catalina.Container;
import org.apache.catalina.Context;
import org.apache.catalina.core.StandardWrapper;

import org.springframework.boot.web.embedded.tomcat.TomcatWebServer;
import org.springframework.boot.web.embedded.undertow.UndertowServletWebServer;
import org.springframework.boot.web.server.WebServer;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.HandlerMapping;

/**
 * {@code DispatcherServletHandlerMappings} provides access to a {@link DispatcherServlet
 * DispatcherServlet's} handler mappings, triggering initialization of the dispatcher
 * servlet if necessary.
 *
 * @author Andy Wilkinson
 */
final class DispatcherServletHandlerMappings {

	private final String name;

	private final DispatcherServlet dispatcherServlet;

	private final WebApplicationContext applicationContext;

	/**
     * Constructs a new DispatcherServletHandlerMappings object with the specified name, dispatcherServlet, and applicationContext.
     * 
     * @param name the name of the DispatcherServletHandlerMappings object
     * @param dispatcherServlet the DispatcherServlet object associated with the mappings
     * @param applicationContext the WebApplicationContext object associated with the mappings
     */
    DispatcherServletHandlerMappings(String name, DispatcherServlet dispatcherServlet,
			WebApplicationContext applicationContext) {
		this.name = name;
		this.dispatcherServlet = dispatcherServlet;
		this.applicationContext = applicationContext;
	}

	/**
     * Retrieves the list of handler mappings configured in the DispatcherServlet.
     * 
     * @return The list of handler mappings.
     */
    List<HandlerMapping> getHandlerMappings() {
		List<HandlerMapping> handlerMappings = this.dispatcherServlet.getHandlerMappings();
		if (handlerMappings == null) {
			initializeDispatcherServletIfPossible();
			handlerMappings = this.dispatcherServlet.getHandlerMappings();
		}
		return (handlerMappings != null) ? handlerMappings : Collections.emptyList();
	}

	/**
     * Initializes the dispatcher servlet if possible.
     * 
     * This method checks if the application context is an instance of ServletWebServerApplicationContext.
     * If it is, it retrieves the web server from the application context and checks its type.
     * If the web server is an instance of UndertowServletWebServer, it initializes the servlet using UndertowServletInitializer.
     * If the web server is an instance of TomcatWebServer, it initializes the servlet using TomcatServletInitializer.
     * 
     * @see ServletWebServerApplicationContext
     * @see UndertowServletWebServer
     * @see UndertowServletInitializer
     * @see TomcatWebServer
     * @see TomcatServletInitializer
     */
    private void initializeDispatcherServletIfPossible() {
		if (!(this.applicationContext instanceof ServletWebServerApplicationContext webServerApplicationContext)) {
			return;
		}
		WebServer webServer = webServerApplicationContext.getWebServer();
		if (webServer instanceof UndertowServletWebServer undertowServletWebServer) {
			new UndertowServletInitializer(undertowServletWebServer).initializeServlet(this.name);
		}
		else if (webServer instanceof TomcatWebServer tomcatWebServer) {
			new TomcatServletInitializer(tomcatWebServer).initializeServlet(this.name);
		}
	}

	/**
     * Returns the name of the DispatcherServletHandlerMappings object.
     *
     * @return the name of the DispatcherServletHandlerMappings object
     */
    String getName() {
		return this.name;
	}

	/**
     * TomcatServletInitializer class.
     */
    private static final class TomcatServletInitializer {

		private final TomcatWebServer webServer;

		/**
         * Constructs a new TomcatServletInitializer with the specified TomcatWebServer.
         *
         * @param webServer the TomcatWebServer to be associated with this TomcatServletInitializer
         */
        private TomcatServletInitializer(TomcatWebServer webServer) {
			this.webServer = webServer;
		}

		/**
         * Initializes a servlet with the given name.
         * 
         * @param name the name of the servlet to initialize
         */
        void initializeServlet(String name) {
			findContext().ifPresent((context) -> initializeServlet(context, name));
		}

		/**
         * Finds the first Context object within the Tomcat Host.
         *
         * @return an Optional containing the first Context object found, or an empty Optional if no Context object is found
         */
        private Optional<Context> findContext() {
			return Stream.of(this.webServer.getTomcat().getHost().findChildren())
				.filter(Context.class::isInstance)
				.map(Context.class::cast)
				.findFirst();
		}

		/**
         * Initializes the servlet with the given context and name.
         * 
         * @param context the context in which the servlet is initialized
         * @param name the name of the servlet to be initialized
         */
        private void initializeServlet(Context context, String name) {
			Container child = context.findChild(name);
			if (child instanceof StandardWrapper wrapper) {
				try {
					wrapper.deallocate(wrapper.allocate());
				}
				catch (ServletException ex) {
					// Continue
				}
			}
		}

	}

	/**
     * UndertowServletInitializer class.
     */
    private static final class UndertowServletInitializer {

		private final UndertowServletWebServer webServer;

		/**
         * Constructs a new UndertowServletInitializer with the specified UndertowServletWebServer.
         *
         * @param webServer the UndertowServletWebServer to be associated with the UndertowServletInitializer
         */
        private UndertowServletInitializer(UndertowServletWebServer webServer) {
			this.webServer = webServer;
		}

		/**
         * Initializes a servlet with the given name.
         * 
         * @param name the name of the servlet to initialize
         */
        void initializeServlet(String name) {
			try {
				this.webServer.getDeploymentManager().getDeployment().getServlets().getManagedServlet(name).forceInit();
			}
			catch (ServletException ex) {
				// Continue
			}
		}

	}

}
