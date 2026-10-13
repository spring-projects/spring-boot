/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.devtools.restart;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Scope;

/**
 * Restart {@code @Scope} Annotation used to indicate that a bean should remain between
 * restarts.
 *
 * @author Phillip Webb
 * @since 1.3.0
 * @see RestartScopeInitializer
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Scope("restart")
public @interface RestartScope {

}
