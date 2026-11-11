/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.kafka;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class SampleMessage {

	private final Integer id;

	private final String message;

	@JsonCreator
	public SampleMessage(@JsonProperty("id") Integer id, @JsonProperty("message") String message) {
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
