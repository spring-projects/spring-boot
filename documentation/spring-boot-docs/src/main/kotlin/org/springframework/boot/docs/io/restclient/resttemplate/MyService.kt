/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.io.restclient.resttemplate

import org.springframework.boot.restclient.RestTemplateBuilder
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate

@Service
class MyService(restTemplateBuilder: RestTemplateBuilder) {

	private val restTemplate: RestTemplate

	init {
		restTemplate = restTemplateBuilder.build()
	}

	fun someRestCall(name: String): Details {
		return restTemplate.getForObject("/{name}/details", Details::class.java, name)!!
	}

}

