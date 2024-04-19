/*
 * Copyright 2012-2024 the original author or authors.
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

import java.util.Arrays;
import java.util.List;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.RecordComponentElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;

/**
 * A {@link PropertyDescriptor} for a record parameter.
 *
 * @author Stephane Nicoll
 * @author Pavel Anisimov
 * @author Phillip Webb
 */
class RecordParameterPropertyDescriptor extends ParameterPropertyDescriptor {

	private final RecordComponentElement recordComponent;

	RecordParameterPropertyDescriptor(String name, TypeMirror type, VariableElement parameter,
			TypeElement declaringElement, ExecutableElement getter, RecordComponentElement recordComponent) {
		super(name, type, parameter, declaringElement, getter);
		this.recordComponent = recordComponent;
	}

	@Override
	protected List<Element> getDeprecatableElements() {
		return Arrays.asList(getGetter());
	}

	@Override
	protected boolean isMarkedAsNested(MetadataGenerationEnvironment environment) {
		return false;
	}

	@Override
	protected String resolveDescription(MetadataGenerationEnvironment environment) {
		return environment.getTypeUtils().getJavaDoc(this.recordComponent);
	}

}
