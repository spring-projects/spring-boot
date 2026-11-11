/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.webservices.test.autoconfigure.client;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * Test response.
 *
 * @author Dmytro Nosan
 */
@XmlRootElement(name = "response")
@XmlAccessorType(XmlAccessType.FIELD)
class Response {

	private int status;

	int getStatus() {
		return this.status;
	}

}
