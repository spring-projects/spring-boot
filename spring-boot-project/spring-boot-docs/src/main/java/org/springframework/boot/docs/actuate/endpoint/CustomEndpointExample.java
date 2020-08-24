/*
 * Copyright 2012-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.docs.actuate.endpoint;

import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.annotation.WriteOperation;

/**
 * An example of a custom actuator endpoint.
 *
 * @author Stephane Nicoll
 */
@Endpoint(id = "custom")
public class CustomEndpointExample {

	// tag::read[]
	@ReadOperation
	public CustomData getCustomData() {
		return new CustomData("test", 5);
	}
	// end::read[]

	// tag::write[]
	@WriteOperation
	public void updateCustomData(String name, int counter) {
		// injects "test" and 42
	}
	// end::write[]

	public static class CustomData {

		private final String name;

		private final int counter;

		public CustomData(String name, int counter) {
			this.name = name;
			this.counter = counter;
		}

	}

}
