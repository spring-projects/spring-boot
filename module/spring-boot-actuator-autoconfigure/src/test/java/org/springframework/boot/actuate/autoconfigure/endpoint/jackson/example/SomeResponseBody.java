/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.endpoint.jackson.example;

import org.springframework.boot.actuate.endpoint.OperationResponseBody;

/**
 * A class that reassembles something like
 * {@code org.springframework.boot.micrometer.metrics.actuate.endpoint.MetricsEndpoint.MetricDescriptor},
 * not exposing a public constructor, only package level. They could be deserialized with
 * Jackson 2 out of the box, but not with Jackson 3 anymore. Constructors with one
 * argument still deserialize as is, so this need to have two or more.
 *
 * @author Michael J. Simons
 */
public final class SomeResponseBody implements OperationResponseBody {

	private final String value1;

	private final String value2;

	SomeResponseBody(String value1, String value2) {
		this.value1 = value1;
		this.value2 = value2;
	}

	public String getValue1() {
		return this.value1;
	}

	public String getValue2() {
		return this.value2;
	}

}
