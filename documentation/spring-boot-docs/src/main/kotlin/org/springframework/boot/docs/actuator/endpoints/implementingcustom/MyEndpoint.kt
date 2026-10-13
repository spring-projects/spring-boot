/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.actuator.endpoints.implementingcustom

import org.springframework.boot.actuate.endpoint.annotation.Endpoint
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation
import org.springframework.boot.actuate.endpoint.annotation.WriteOperation

@Endpoint(id = "custom")
@Suppress("UNUSED_PARAMETER")
class MyEndpoint {

	// tag::read[]
	@ReadOperation
	fun getData(): CustomData {
		return CustomData("test", 5)
	}
	// end::read[]

	// tag::write[]
	@WriteOperation
	fun updateData(name: String?, counter: Int) {
		// injects "test" and 42
	}
	// end::write[]
}

