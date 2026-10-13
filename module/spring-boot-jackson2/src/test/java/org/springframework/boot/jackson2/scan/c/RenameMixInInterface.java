/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.jackson2.scan.c;

import com.fasterxml.jackson.annotation.JsonProperty;

import org.springframework.boot.jackson2.JsonMixin;
import org.springframework.boot.jackson2.types.NameAndAge;

@JsonMixin(type = NameAndAge.class)
@Deprecated(since = "4.0.0", forRemoval = true)
@SuppressWarnings("removal")
public interface RenameMixInInterface {

	@JsonProperty("username")
	String getName();

}
