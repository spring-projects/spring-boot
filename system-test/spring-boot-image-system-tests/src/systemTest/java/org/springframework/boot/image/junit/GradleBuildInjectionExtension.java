/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.image.junit;

import java.lang.reflect.Field;

import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import org.springframework.boot.testsupport.BuildOutput;
import org.springframework.boot.testsupport.gradle.testkit.GradleBuild;
import org.springframework.boot.testsupport.gradle.testkit.GradleVersions;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

/**
 * A {@link BeforeEachCallback} to configure and set a test class's {@code gradleBuild}
 * field prior to test execution.
 *
 * @author Scott Frederick
 */
public class GradleBuildInjectionExtension implements BeforeEachCallback {

	private final String bootVersion;

	GradleBuildInjectionExtension() {
		String bootVersion = System.getProperty("springBootVersion");
		Assert.state(bootVersion != null, "Property 'springBootVersion' must be set in build environment");
		this.bootVersion = bootVersion;
	}

	@Override
	public void beforeEach(ExtensionContext context) throws Exception {
		GradleBuild gradleBuild = new GradleBuild(new BuildOutput(context.getRequiredTestClass()))
			.gradleVersion(GradleVersions.minimumCompatible())
			.bootVersion(this.bootVersion);
		Field field = ReflectionUtils.findField(context.getRequiredTestClass(), "gradleBuild");
		Assert.notNull(field, "No gradleBuild field found in " + context.getRequiredTestClass().getName());
		field.setAccessible(true);
		field.set(context.getRequiredTestInstance(), gradleBuild);
	}

}
