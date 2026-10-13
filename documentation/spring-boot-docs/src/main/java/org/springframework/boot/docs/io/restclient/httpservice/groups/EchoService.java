/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.io.restclient.httpservice.groups;

import java.util.Map;

import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.PostExchange;

public interface EchoService {

	@PostExchange
	Map<?, ?> echo(@RequestBody Map<String, String> message);

}
