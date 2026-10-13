/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.configurationsample.generic;

/**
 * Properties with unresolved generic types that use identical generic parameter names but
 * differ in their positions.
 *
 * @param <C> mapping name type
 * @param <B> mapping value type
 * @author Dmytro Nosan
 */
public class MixGenericNameProperties<B, C extends Number> extends AbstractGenericProperties<String, C, B> {

}
