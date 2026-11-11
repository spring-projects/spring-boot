/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.web.servlet.springmvc;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

@Component
public class MyUserHandler {

	public ServerResponse getUser(ServerRequest request) {
		/**/ return ServerResponse.ok().build();
	}

	public ServerResponse getUserCustomers(ServerRequest request) {
		/**/ return ServerResponse.ok().build();
	}

	public ServerResponse deleteUser(ServerRequest request) {
		/**/ return ServerResponse.ok().build();
	}

}
