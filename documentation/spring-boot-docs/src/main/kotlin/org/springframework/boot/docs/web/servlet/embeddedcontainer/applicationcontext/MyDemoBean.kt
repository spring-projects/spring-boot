/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.web.servlet.embeddedcontainer.applicationcontext

import jakarta.servlet.ServletContext
import org.springframework.boot.context.event.ApplicationStartedEvent
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationListener
import org.springframework.web.context.WebApplicationContext

class MyDemoBean : ApplicationListener<ApplicationStartedEvent> {

	@Suppress("unused")
	private var servletContext: ServletContext? = null

	override fun onApplicationEvent(event: ApplicationStartedEvent) {
		val applicationContext: ApplicationContext = event.applicationContext
		this.servletContext = (applicationContext as WebApplicationContext).servletContext
	}

}
