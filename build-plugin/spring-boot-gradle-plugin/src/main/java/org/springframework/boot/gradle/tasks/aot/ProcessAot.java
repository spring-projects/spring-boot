/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.gradle.tasks.aot;

import java.util.ArrayList;
import java.util.List;

import org.gradle.api.provider.Property;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.JavaExec;
import org.gradle.api.tasks.TaskAction;

/**
 * Custom {@link JavaExec} task for ahead-of-time processing of a Spring Boot application.
 *
 * @author Andy Wilkinson
 * @since 3.0.0
 */
@CacheableTask
public abstract class ProcessAot extends AbstractAot {

	public ProcessAot() {
		getMainClass().set("org.springframework.boot.SpringApplicationAotProcessor");
	}

	/**
	 * Returns the main class of the application that is to be processed ahead-of-time.
	 * @return the application main class property
	 */
	@Input
	public abstract Property<String> getApplicationMainClass();

	@Override
	@TaskAction
	public void exec() {
		List<String> args = new ArrayList<>();
		args.add(getApplicationMainClass().get());
		args.addAll(processorArgs());
		setArgs(args);
		super.exec();
	}

}
