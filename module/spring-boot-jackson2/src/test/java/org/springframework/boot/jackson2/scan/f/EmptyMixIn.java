/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.jackson2.scan.f;

import com.fasterxml.jackson.annotation.JsonProperty;

import org.springframework.boot.jackson2.JsonMixin;

@JsonMixin
@Deprecated(since = "4.0.0", forRemoval = true)
@SuppressWarnings("removal")
public interface EmptyMixIn {

	@JsonProperty("username")
	String getName();

}
