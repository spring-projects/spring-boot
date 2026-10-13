/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.test.web.server;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.beans.factory.annotation.Value;

/**
 * Annotation at the field or method/constructor parameter level that injects the HTTP
 * server port that was allocated at runtime. Provides a convenient alternative for
 * <code>&#064;Value(&quot;${local.server.port}&quot;)</code> with the assumption that the
 * web server implementation has configured such a property.
 *
 * @author Anand Shah
 * @author Stephane Nicoll
 * @since 2.7.0
 */
@Target({ ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Value("${local.server.port}")
public @interface LocalServerPort {

}
