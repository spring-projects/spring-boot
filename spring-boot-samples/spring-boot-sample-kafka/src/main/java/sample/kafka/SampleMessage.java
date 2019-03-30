/*
 * Copyright 2012-2018 the original author or authors.
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
package sample.kafka;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class SampleMessage {

	private final Integer id;

	private final String message;

	@JsonCreator
	public SampleMessage(@JsonProperty("id") Integer id,
			@JsonProperty("message") String message) {
		this.id = id;
		this.message = message;
	}

	public Integer getId() {
		return this.id;
	}

	public String getMessage() {
		return this.message;
	}

	@Override
	public String toString() {
		return "SampleMessage{id=" + this.id + ", message='" + this.message + "'}";
	}

}
