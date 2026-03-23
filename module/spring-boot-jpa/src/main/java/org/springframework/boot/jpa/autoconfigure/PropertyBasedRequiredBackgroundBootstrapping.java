/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.jpa.autoconfigure;

import java.util.function.Supplier;

import org.springframework.boot.diagnostics.FailureAnalyzedException;
import org.springframework.boot.jpa.EntityManagerFactoryBuilder;
import org.springframework.util.Assert;

/**
 * {@link Supplier} to use with
 * {@link EntityManagerFactoryBuilder#requireBootstrapExecutor} when a property indicates
 * background bootstrapping is required.
 *
 * @author Phillip Webb
 * @since 4.1.0
 */
public class PropertyBasedRequiredBackgroundBootstrapping implements Supplier<RuntimeException> {

	private final String propertyName;

	private final String propertyValue;

	public PropertyBasedRequiredBackgroundBootstrapping(String propertyName, String propertyValue) {
		Assert.notNull(propertyName, "'propertyName' must not be null");
		Assert.notNull(propertyValue, "'propertyValue' must not be null");
		this.propertyName = propertyName;
		this.propertyValue = propertyValue;
	}

	@Override
	public RuntimeException get() {
		String description = "A LocalContainerEntityManagerFactoryBean bootstrap executor is required when '%s' is set to '%s'"
			.formatted(this.propertyName, this.propertyValue);
		StringBuilder action = new StringBuilder();
		action.append("Use a different '%s' or provide a bootstrap executor using one of the following methods:\n"
			.formatted(this.propertyName));
		action.append("\tWith an auto-configured task executor "
				+ "(you may need to set 'spring.task.execution.mode' to 'force').");
		action.append("\tWith an AsyncTaskExecutor bean named 'applicationTaskExecutor.'");
		action.append("\tUsing a EntityManagerFactoryBuilderCustomizer.");
		return new FailureAnalyzedException(description, action.toString());
	}

}
