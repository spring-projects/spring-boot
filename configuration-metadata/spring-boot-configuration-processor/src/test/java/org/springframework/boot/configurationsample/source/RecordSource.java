/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.configurationsample.source;

/*
 * Record type with manual metadata. This illustrates the case where the type of a
 * property is defined in a separate class and source-based metadata cannot be discovered.
 */
public record RecordSource(String name, String description, String type) {
}
