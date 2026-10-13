/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.sni.server;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {

	@GetMapping
	public String hello(HttpServletRequest request) {
		return "Hello from " + request.getRequestURL();
	}

}