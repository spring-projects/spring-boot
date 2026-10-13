/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.jackson.scan.e;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.jspecify.annotations.Nullable;

import org.springframework.boot.jackson.JacksonMixin;
import org.springframework.boot.jackson.types.Name;
import org.springframework.boot.jackson.types.NameAndAge;

@JacksonMixin(type = { Name.class, NameAndAge.class })
class PrivateMixInClass {

	@JsonProperty("username")
	@Nullable String getName() {
		return null;
	}

}
