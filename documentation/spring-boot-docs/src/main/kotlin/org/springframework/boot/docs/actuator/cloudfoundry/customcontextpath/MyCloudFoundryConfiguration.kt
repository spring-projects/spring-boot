/*
 * Copyright 2012-present the original author or authors.
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
import org.springframework.boot.tomcat.servlet.TomcatServletWebServerFactory
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

			override fun prepareContext(host: Host, initializers: Array<ServletContextInitializer>, tempDirs: TempDirs) {
				super.prepareContext(host, initializers, tempDirs)
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

