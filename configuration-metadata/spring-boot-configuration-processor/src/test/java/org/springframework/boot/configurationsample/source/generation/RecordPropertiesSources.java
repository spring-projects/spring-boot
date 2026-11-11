/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.configurationsample.source.generation;

import org.springframework.boot.configurationsample.TestConfigurationPropertiesSource;
import org.springframework.boot.configurationsample.TestDefaultValue;

/**
 * Sample record properties source.
 *
 * @param name Description of this simple property.
 * @param enabled Whether it is enabled.
 * @author Stephane Nicoll
 */
@TestConfigurationPropertiesSource
public record RecordPropertiesSources(@TestDefaultValue("boot") String name, boolean enabled) {
}
