/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.configurationsample.record;

import org.springframework.boot.configurationsample.TestConfigurationProperties;
import org.springframework.boot.configurationsample.TestName;

/**
 * Example Record Javadoc sample
 *
 * @param someString very long description that doesn't fit single line and is indented
 * @param someInteger description with @param and @ pitfalls
 * @param someBoolean description with extra spaces
 * @param someLong description without space after asterisk
 * @param namedComponent description of a named component
 * @param someByte last description in Javadoc
 * @since 1.0.0
 * @author Pavel Anisimov
 */
@TestConfigurationProperties("record.descriptions")
public record ExampleRecord(String someString, Integer someInteger, Boolean someBoolean, Long someLong,
		@TestName("named.record.component") String namedComponent, Byte someByte) {
}
