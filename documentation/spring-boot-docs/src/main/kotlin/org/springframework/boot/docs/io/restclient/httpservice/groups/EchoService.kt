/*
 * Copyright 2012-present the original author or authors.
 */
package org.springframework.boot.docs.io.restclient.httpservice.groups

import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.service.annotation.HttpExchange
import org.springframework.web.service.annotation.PostExchange

interface EchoService {

	@PostExchange
	fun echo(@RequestBody message: Map<String, String>): Map<*, *>

}
