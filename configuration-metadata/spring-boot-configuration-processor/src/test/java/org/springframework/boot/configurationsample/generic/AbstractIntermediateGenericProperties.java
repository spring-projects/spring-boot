/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.configurationsample.generic;

/**
 * An intermediate layer that resolves some of the generics from the parent but not all.
 *
 * @param <C> mapping value type
 * @author Stephane Nicoll
 */
public abstract class AbstractIntermediateGenericProperties<C> extends AbstractGenericProperties<String, Integer, C> {

}
