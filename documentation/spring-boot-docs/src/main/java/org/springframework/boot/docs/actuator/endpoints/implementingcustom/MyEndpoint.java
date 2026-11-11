/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.actuator.endpoints.implementingcustom;

import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.annotation.WriteOperation;

@Endpoint(id = "custom")
public class MyEndpoint {

	// tag::read[]
	@ReadOperation
	public CustomData getData() {
		return new CustomData("test", 5);
	}
	// end::read[]

	// tag::write[]
	@WriteOperation
	public void updateData(String name, int counter) {
		// injects "test" and 42
	}
	// end::write[]

}
