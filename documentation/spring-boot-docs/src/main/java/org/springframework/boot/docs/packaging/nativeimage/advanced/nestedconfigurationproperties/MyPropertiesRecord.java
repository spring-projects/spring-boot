/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.packaging.nativeimage.advanced.nestedconfigurationproperties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

@ConfigurationProperties("my.properties")
public record MyPropertiesRecord(String name, @NestedConfigurationProperty Nested nested) {

}
