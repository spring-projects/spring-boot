/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.jackson.scan.f;

import com.fasterxml.jackson.annotation.JsonProperty;

import org.springframework.boot.jackson.JacksonMixin;

@JacksonMixin
public interface EmptyMixIn {

	@JsonProperty("username")
	String getName();

}
