/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.testsupport.classpath.resources;

import java.io.File;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.nio.file.Path;

/**
 * Annotation that indicates that the resources root should be injected. Supported on
 * parameters of type:
 *
 * <ul>
 * <li>{@link File}</li>
 * <li>{@link Path}</li>
 * </ul>
 *
 * @author Andy Wilkinson
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface ResourcesRoot {

}
