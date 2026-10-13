/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.webflux.test.autoconfigure;

import org.springframework.boot.jackson.JacksonComponent;

/**
 * Example result that requires a {@link JacksonComponent}.
 *
 * @param id sample data
 * @author Stephane Nicoll
 */
public record ExampleResult(String id) {
}
