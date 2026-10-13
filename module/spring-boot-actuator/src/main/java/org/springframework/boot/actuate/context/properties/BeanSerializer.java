/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.actuate.context.properties;

import java.util.Map;

import org.jspecify.annotations.Nullable;

/**
 * Strategy used by {@link ConfigurationPropertiesReportEndpoint} to serialize beans into
 * a {@link Map}.
 *
 * @author Phillip Webb
 */
@FunctionalInterface
interface BeanSerializer {

	Map<String, @Nullable Object> serialize(@Nullable Object bean);

}
