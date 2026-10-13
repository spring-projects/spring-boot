/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.configurationsample.lombok;

import java.util.ArrayList;
import java.util.List;

import lombok.Value;

import org.springframework.boot.configurationsample.TestConfigurationProperties;

/**
 * Configuration properties using Lombok {@code @Value}.
 *
 * @author Mark Jeffrey
 */
@Value
@TestConfigurationProperties("value")
@SuppressWarnings("unused")
public class LombokSimpleValueProperties {

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
