/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.configurationprocessor;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.RecordComponentElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;

import org.springframework.boot.configurationprocessor.metadata.ItemDeprecation;

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
	protected Element getSourceElement() {
		return this.recordComponent;
	}

	@Override
	protected ItemDeprecation resolveItemDeprecation(MetadataGenerationEnvironment environment) {
		return resolveItemDeprecation(environment, getGetter());
	}

	@Override
	protected boolean isMarkedAsNested(MetadataGenerationEnvironment environment) {
		return environment.getNestedConfigurationPropertyAnnotation(this.recordComponent) != null
				|| environment.getNestedConfigurationPropertyAnnotation(getGetter()) != null;
	}

	@Override
	protected String resolveDescription(MetadataGenerationEnvironment environment) {
		return environment.getTypeUtils().getJavaDoc(this.recordComponent);
	}

}
