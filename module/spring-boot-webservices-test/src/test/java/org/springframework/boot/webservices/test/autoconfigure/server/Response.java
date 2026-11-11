/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.webservices.test.autoconfigure.server;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * Test response.
 *
 * @author Daniil Razorenov
 */
@XmlRootElement(name = "response")
class Response {

	@XmlElement(required = true)
	private int code;

	Response(int code) {
		this.code = code;
	}

	Response() {
	}

}
