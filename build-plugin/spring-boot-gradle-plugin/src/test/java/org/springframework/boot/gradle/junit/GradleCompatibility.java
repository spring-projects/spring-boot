/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.gradle.junit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.Extension;

import org.springframework.boot.testsupport.gradle.testkit.GradleBuild;

/**
 * {@link Extension} that runs {@link TestTemplate templated tests} against multiple
 * versions of Gradle. Test classes using the extension must have a non-private and
 * non-final {@link GradleBuild} field named {@code gradleBuild}.
 *
 * @author Andy Wilkinson
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@ExtendWith(GradleCompatibilityExtension.class)
public @interface GradleCompatibility {

	/**
	 * Whether to include running Gradle with {@code --cache-configuration} cache in the
	 * compatibility matrix.
	 * @return {@code true} to enable the configuration cache, {@code false} otherwise
	 */
	boolean configurationCache() default false;

	/**
	 * Minimum version of Gradle against which compatibility should be tested.
	 * @return minimum version
	 */
	String minimumVersion() default "";

}
