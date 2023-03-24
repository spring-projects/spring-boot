/*
 * Copyright 2012-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
