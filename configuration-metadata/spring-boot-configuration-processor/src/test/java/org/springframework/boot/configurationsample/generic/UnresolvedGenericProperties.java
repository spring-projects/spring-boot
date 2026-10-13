/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.configurationsample.generic;

import org.springframework.boot.configurationsample.TestConfigurationProperties;

/**
 * Properties with unresolved generic information.
 *
 * @param <B> mapping key type
 * @param <C> mapping value type
 * @author Stephane Nicoll
 */
@TestConfigurationProperties("generic")
public class UnresolvedGenericProperties<B extends Number, C> extends AbstractGenericProperties<String, B, C> {

}
