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

package org.springframework.boot.docs.actuator.cloudfoundry.customcontextpath

import jakarta.servlet.GenericServlet
import jakarta.servlet.Servlet
import jakarta.servlet.ServletContainerInitializer
import jakarta.servlet.ServletContext
import jakarta.servlet.ServletException
import jakarta.servlet.ServletRequest
import jakarta.servlet.ServletResponse
import org.apache.catalina.Host
import org.apache.catalina.core.StandardContext
import org.apache.catalina.startup.Tomcat.FixContextListener
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory
import org.springframework.boot.web.servlet.ServletContextInitializer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.io.IOException
import java.util.Collections.emptySet

@Suppress("UNUSED_ANONYMOUS_PARAMETER")
@Configuration(proxyBeanMethods = false)
class MyCloudFoundryConfiguration {

	@Bean
	fun servletWebServerFactory(): TomcatServletWebServerFactory {
		return object : TomcatServletWebServerFactory() {

			override fun prepareContext(host: Host, initializers: Array<ServletContextInitializer>) {
				super.prepareContext(host, initializers)
				val child = StandardContext()
				child.addLifecycleListener(FixContextListener())
				child.path = "/cloudfoundryapplication"
				val initializer = getServletContextInitializer(contextPath)
				child.addServletContainerInitializer(initializer, emptySet())
				child.crossContext = true
				host.addChild(child)
			}

		}
	}

	private fun getServletContextInitializer(contextPath: String): ServletContainerInitializer {
		return ServletContainerInitializer { classes: Set<Class<*>?>?, context: ServletContext ->
			val servlet: Servlet = object : GenericServlet() {

				@Throws(ServletException::class, IOException::class)
				override fun service(req: ServletRequest, res: ServletResponse) {
					val servletContext = req.servletContext.getContext(contextPath)
					servletContext.getRequestDispatcher("/cloudfoundryapplication").forward(req, res)
				}

			}
			context.addServlet("cloudfoundry", servlet).addMapping("/*")
		}
	}
}