/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.webservices.test.autoconfigure.server;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import org.jspecify.annotations.Nullable;

/**
 * Test request.
 *
 * @author Daniil Razorenov
 */
@XmlRootElement(name = "request")
class Request {

	@XmlElement(required = true)
	private @Nullable String message;

}
