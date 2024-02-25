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

package smoketest.kafka;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * SampleMessage class.
 */
public class SampleMessage {

	private final Integer id;

	private final String message;

	/**
	 * Constructs a new SampleMessage object with the specified id and message.
	 * @param id the id of the SampleMessage
	 * @param message the message of the SampleMessage
	 */
	@JsonCreator
	public SampleMessage(@JsonProperty("id") Integer id, @JsonProperty("message") String message) {
		this.id = id;
		this.message = message;
	}

	/**
	 * Returns the ID of the SampleMessage object.
	 * @return the ID of the SampleMessage object
	 */
	public Integer getId() {
		return this.id;
	}

	/**
	 * Returns the message.
	 * @return the message
	 */
	public String getMessage() {
		return this.message;
	}

	/**
	 * Returns a string representation of the SampleMessage object.
	 * @return a string representation of the SampleMessage object
	 */
	@Override
	public String toString() {
		return "SampleMessage{id=" + this.id + ", message='" + this.message + "'}";
	}

}
