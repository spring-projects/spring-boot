/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.features.externalconfig.typesafeconfigurationproperties.usingannotatedtypes

import org.springframework.stereotype.Service

@Service
class MyService(val properties: MyProperties) {

	fun openConnection() {
		val server = Server(properties.remoteAddress)
		server.start()
		// ...
	}

	// ...

}

