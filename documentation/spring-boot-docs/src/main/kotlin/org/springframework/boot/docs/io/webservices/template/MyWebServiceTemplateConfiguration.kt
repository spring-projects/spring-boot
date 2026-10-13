/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.io.webservices.template

import org.springframework.boot.http.client.HttpClientSettings
import org.springframework.boot.webservices.client.WebServiceMessageSenderFactory
import org.springframework.boot.webservices.client.WebServiceTemplateBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.ws.client.core.WebServiceTemplate
import java.time.Duration

@Configuration(proxyBeanMethods = false)
class MyWebServiceTemplateConfiguration {

	@Bean
	fun webServiceTemplate(builder: WebServiceTemplateBuilder): WebServiceTemplate {
		val settings = HttpClientSettings.defaults()
				.withConnectTimeout(Duration.ofSeconds(2))
				.withReadTimeout(Duration.ofSeconds(2))
		builder.httpMessageSenderFactory(WebServiceMessageSenderFactory.http(settings))
		return builder.build()
	}

}

