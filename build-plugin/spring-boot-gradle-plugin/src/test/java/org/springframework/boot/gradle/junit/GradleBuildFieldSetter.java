/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.gradle.junit;

import java.lang.reflect.Field;

import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import org.springframework.boot.testsupport.gradle.testkit.GradleBuild;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

/**
 * {@link BeforeEachCallback} to set a test class's {@code gradleBuild} field prior to
 * test execution.
 *
 * @author Andy Wilkinson
 */
final class GradleBuildFieldSetter implements BeforeEachCallback {

	private final GradleBuild gradleBuild;

	GradleBuildFieldSetter(GradleBuild gradleBuild) {
		this.gradleBuild = gradleBuild;
	}

	@Override
	public void beforeEach(ExtensionContext context) throws Exception {
		Field field = ReflectionUtils.findField(context.getRequiredTestClass(), "gradleBuild");
		Assert.notNull(field, "Field named gradleBuild not found in " + context.getRequiredTestClass().getName());
		field.setAccessible(true);
		field.set(context.getRequiredTestInstance(), this.gradleBuild);

	}

}
