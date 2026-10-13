/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.configurationsample.source;

import lombok.Getter;
import lombok.Setter;

/**
 * Type using lombok with manual metadata. This illustrates the case where the type of a
 * property is defined in a separate class and source-based metadata cannot be discovered.
 */
@Getter
@Setter
public class LombokSource {

	@SuppressWarnings("unused")
	private String name;

	@SuppressWarnings("unused")
	private String description;

	@SuppressWarnings("unused")
	private String type;

}
