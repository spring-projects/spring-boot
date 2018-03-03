/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.docs.cloudfoundry;

import java.io.IOException;
import java.util.Collections;

import javax.servlet.GenericServlet;
import javax.servlet.Servlet;
import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.apache.catalina.Host;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.startup.Tomcat;

import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.context.annotation.Bean;

/**
 * Example configuration for custom context path in Cloud Foundry.
 *
 * @author Johnny Lim
 */
public class CloudFoundryCustomContextPathExample {

	// tag::configuration[]
	@Bean
	public TomcatServletWebServerFactory servletWebServerFactory() {
		return new TomcatServletWebServerFactory() {

			@Override
			protected void prepareContext(Host host,
					ServletContextInitializer[] initializers) {
				super.prepareContext(host, initializers);
				StandardContext child = new StandardContext();
				child.addLifecycleListener(new Tomcat.FixContextListener());
				child.setPath("/cloudfoundryapplication");
				ServletContainerInitializer initializer = getServletContextInitializer(
						getContextPath());
				child.addServletContainerInitializer(initializer, Collections.emptySet());
				child.setCrossContext(true);
				host.addChild(child);
			}

		};
	}

	private ServletContainerInitializer getServletContextInitializer(String contextPath) {
		return (c, context) -> {
			Servlet servlet = new GenericServlet() {

				@Override
				public void service(ServletRequest req, ServletResponse res)
						throws ServletException, IOException {
					ServletContext context = req.getServletContext()
							.getContext(contextPath);
					context.getRequestDispatcher("/cloudfoundryapplication").forward(req,
							res);
				}

			};
			context.addServlet("cloudfoundry", servlet).addMapping("/*");
		};
	}
	// end::configuration[]

}
