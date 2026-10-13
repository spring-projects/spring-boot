/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.jackson2.scan.e;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.jspecify.annotations.Nullable;

import org.springframework.boot.jackson2.JsonMixin;
import org.springframework.boot.jackson2.types.Name;
import org.springframework.boot.jackson2.types.NameAndAge;

@JsonMixin(type = { Name.class, NameAndAge.class })
@Deprecated(since = "4.0.0", forRemoval = true)
@SuppressWarnings("removal")
class PrivateMixInClass {

	@JsonProperty("username")
	@Nullable String getName() {
		return null;
	}

}
