/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.features.externalconfig.typesafeconfigurationproperties.usingannotatedtypes;

import org.springframework.stereotype.Service;

@Service
public class MyService {

	private final MyProperties properties;

	public MyService(MyProperties properties) {
		this.properties = properties;
	}

	public void openConnection() {
		Server server = new Server(this.properties.getRemoteAddress());
		server.start();
		// ...
	}

	// ...

}
