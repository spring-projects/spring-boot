/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.configurationsample.lombok;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

import org.springframework.boot.configurationsample.TestConfigurationProperties;

/**
 * Configuration properties using lombok @Getter/@Setter at class level.
 *
 * @author Stephane Nicoll
 */
@Getter
@Setter
@TestConfigurationProperties("simple")
@SuppressWarnings("unused")
public class LombokSimpleProperties {

	private final String id = "super-id";

	/**
	 * Name description.
	 */
	private String name;

	private String description;

	private Integer counter;

	@Deprecated
	private Integer number = 0;

	private final List<String> items = new ArrayList<>();

	private final String ignored = "foo";

}
