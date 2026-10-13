/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.howto.webserver.builduritestwebserver

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment
import org.springframework.boot.test.http.server.LocalTestWebServer
import org.springframework.context.ApplicationContext

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class MyWebIntegrationTests(@Autowired val context: ApplicationContext) {

	// ...

	@Test
	fun test() {
		val urlToTest = LocalTestWebServer.obtain(this.context).uri("/test")
	}

}

