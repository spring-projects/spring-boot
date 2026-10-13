/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.io.webservices.template

import org.springframework.boot.webservices.client.WebServiceTemplateBuilder
import org.springframework.stereotype.Service
import org.springframework.ws.client.core.WebServiceTemplate
import org.springframework.ws.soap.client.core.SoapActionCallback

@Service
class MyService(webServiceTemplateBuilder: WebServiceTemplateBuilder) {

	private val webServiceTemplate: WebServiceTemplate

	init {
		webServiceTemplate = webServiceTemplateBuilder.build()
	}

	fun someWsCall(detailsReq: SomeRequest): SomeResponse {
		return webServiceTemplate.marshalSendAndReceive(
			detailsReq,
			SoapActionCallback("https://ws.example.com/action")
		) as SomeResponse
	}

}

