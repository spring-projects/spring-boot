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

package org.springframework.boot.configurationprocessor;

import org.springframework.boot.configurationprocessor.metadata.ItemDeprecation;
import org.springframework.boot.configurationprocessor.model.Declaration;
import org.springframework.boot.configurationprocessor.model.MethodDeclaration;
import org.springframework.boot.configurationprocessor.model.TypeDeclaration;
import org.springframework.boot.configurationprocessor.model.TypeReference;
import org.springframework.boot.configurationprocessor.model.VariableDeclaration;

/**
 * A {@link PropertyDescriptor} for a constructor parameter.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 */
class ConstructorParameterPropertyDescriptor extends ParameterPropertyDescriptor {

	private final MethodDeclaration setter;

	private final VariableDeclaration field;

	ConstructorParameterPropertyDescriptor(String name, TypeReference type, VariableDeclaration parameter,
			TypeDeclaration declaringElement, MethodDeclaration getter, MethodDeclaration setter,
			VariableDeclaration field) {
		super(name, type, parameter, declaringElement, getter);
		this.setter = setter;
		this.field = field;
	}

	@Override
	protected Declaration getSourceElement() {
		return this.field;
	}

	@Override
	protected ItemDeprecation resolveItemDeprecation(MetadataGenerationEnvironment environment) {
		return resolveItemDeprecation(environment, getGetter(), this.setter, this.field);
	}

	@Override
	protected boolean isMarkedAsNested(MetadataGenerationEnvironment environment) {
		return environment.getNestedConfigurationPropertyAnnotation(this.field) != null
				|| environment.getNestedConfigurationPropertyAnnotation(getGetter()) != null;
	}

	@Override
	protected String resolveDescription(MetadataGenerationEnvironment environment) {
		return environment.getDescription(this.field);
	}

}
