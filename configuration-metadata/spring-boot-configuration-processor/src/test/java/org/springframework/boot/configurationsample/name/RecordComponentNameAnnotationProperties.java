/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.configurationsample.name;

import org.springframework.boot.configurationsample.TestConfigurationProperties;
import org.springframework.boot.configurationsample.TestDefaultValue;
import org.springframework.boot.configurationsample.TestName;

/**
 * Record properties making use of {@code @Name}.
 *
 * @param imports Imports to apply.
 * @param defaultValue Whether default mode is enabled.
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 */
@TestConfigurationProperties("named")
public record RecordComponentNameAnnotationProperties(@TestName("import") String imports,
		@TestName("default") @TestDefaultValue("true") boolean defaultValue) {

}
