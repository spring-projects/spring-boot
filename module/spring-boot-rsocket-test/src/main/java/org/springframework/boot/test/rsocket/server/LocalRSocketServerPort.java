/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.test.rsocket.server;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.beans.factory.annotation.Value;

/**
 * Annotation at the field or method/constructor parameter level that injects the RSocket
 * port that was allocated at runtime. Provides a convenient alternative for
 * <code>&#064;Value(&quot;${local.rsocket.server.port}&quot;)</code>.
 *
 * @author Verónica Vásquez
 * @author Eddú Meléndez
 * @since 4.0.0
 */
@Target({ ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Value("${local.rsocket.server.port}")
public @interface LocalRSocketServerPort {

}
