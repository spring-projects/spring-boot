/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.jackson.scan.b;

import com.fasterxml.jackson.annotation.JsonProperty;

import org.springframework.boot.jackson.JacksonMixin;
import org.springframework.boot.jackson.types.NameAndAge;

@JacksonMixin(type = NameAndAge.class)
public abstract class RenameMixInAbstractClass {

	@JsonProperty("username")
	abstract String getName();

}
