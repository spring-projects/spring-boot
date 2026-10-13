/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.jackson.autoconfigure.jsontest.app;

import java.util.Date;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

/**
 * Example object to read/write as JSON through {@link ExampleJacksonComponent}.
 *
 * @author Phillip Webb
 * @param value the value
 * @param date a date
 * @param uuid a uuid
 */
public record ExampleCustomObject(@Nullable String value, @Nullable Date date, @Nullable UUID uuid) {

}
