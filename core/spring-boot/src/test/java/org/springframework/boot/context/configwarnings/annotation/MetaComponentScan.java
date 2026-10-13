/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.context.configwarnings.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.ComponentScan;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@ComponentScan
public @interface MetaComponentScan {

}
