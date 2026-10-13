/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.context.embedded;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Annotation used to configure servlet container tests.
 *
 * @author Andy Wilkinson
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@ExtendWith(EmbeddedServerContainerInvocationContextProvider.class)
public @interface EmbeddedServletContainerTest {

	String packaging();

	Class<? extends AbstractApplicationLauncher>[] launchers();

}
