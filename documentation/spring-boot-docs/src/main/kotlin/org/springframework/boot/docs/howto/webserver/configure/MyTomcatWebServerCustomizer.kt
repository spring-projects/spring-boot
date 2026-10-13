/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.howto.webserver.configure

import org.springframework.boot.tomcat.servlet.TomcatServletWebServerFactory
import org.springframework.boot.web.server.WebServerFactoryCustomizer
import org.springframework.stereotype.Component

@Component
class MyTomcatWebServerCustomizer : WebServerFactoryCustomizer<TomcatServletWebServerFactory> {

	override fun customize(factory: TomcatServletWebServerFactory) {
		// customize the factory here
	}

}

