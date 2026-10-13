/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.testing.utilities.testresttemplate

import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.autoconfigure.ImportAutoConfiguration
import org.springframework.boot.http.converter.autoconfigure.HttpMessageConvertersAutoConfiguration
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration
import org.springframework.boot.tomcat.autoconfigure.servlet.TomcatServletWebServerAutoConfiguration
import org.springframework.boot.webmvc.autoconfigure.DispatcherServletAutoConfiguration
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.net.URI

@SpringBootConfiguration(proxyBeanMethods = false)
@ImportAutoConfiguration(
	TomcatServletWebServerAutoConfiguration::class,
	DispatcherServletAutoConfiguration::class,
	JacksonAutoConfiguration::class,
	HttpMessageConvertersAutoConfiguration::class
)
class MySpringBootTestsConfiguration {

	@RestController
	private class ExampleController {

		@RequestMapping("/example")
		fun example(): ResponseEntity<String> {
			return ResponseEntity.ok().location(URI.create("https://other.example.com/example")).body("test")
		}

	}

}

