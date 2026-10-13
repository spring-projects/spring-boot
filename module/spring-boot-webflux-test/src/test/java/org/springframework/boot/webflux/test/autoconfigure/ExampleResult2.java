/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.webflux.test.autoconfigure;

/**
 * Example result that requires a {@link org.springframework.boot.jackson2.JsonComponent}.
 *
 * @param id sample data
 * @author Stephane Nicoll
 * @deprecated since 4.0.0 for removal in 4.3.0 in favor of Jackson 3
 */
@Deprecated(since = "4.0.0", forRemoval = true)
public record ExampleResult2(String id) {
}
