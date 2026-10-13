/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.build;

import javax.inject.Inject;

import org.gradle.api.Action;
import org.gradle.api.model.ObjectFactory;

/**
 * DSL extension for configuring a project's system requirements.
 *
 * @author Andy Wilkinson
 */
public class SystemRequirementsExtension {

	private final JavaSpec javaSpec;

	@Inject
	public SystemRequirementsExtension(ObjectFactory objects) {
		this.javaSpec = objects.newInstance(JavaSpec.class);
	}

	public void java(Action<JavaSpec> action) {
		action.execute(this.javaSpec);
	}

	public JavaSpec getJava() {
		return this.javaSpec;
	}

	public abstract static class JavaSpec {

		private int version = 17;

		public int getVersion() {
			return this.version;
		}

		public void setVersion(int version) {
			this.version = version;
		}

	}

}
