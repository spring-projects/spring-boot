/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.configurationprocessor;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;

import org.springframework.boot.configurationprocessor.metadata.ItemDeprecation;

/**
 * A {@link PropertyDescriptor} for a constructor parameter.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 */
class ConstructorParameterPropertyDescriptor extends ParameterPropertyDescriptor {

	private final ExecutableElement setter;

	private final VariableElement field;

	ConstructorParameterPropertyDescriptor(String name, TypeMirror type, VariableElement parameter,
			TypeElement declaringElement, ExecutableElement getter, ExecutableElement setter, VariableElement field) {
		super(name, type, parameter, declaringElement, getter);
		this.setter = setter;
		this.field = field;
	}

	@Override
	protected Element getSourceElement() {
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
		return environment.getTypeUtils().getJavaDoc(this.field);
	}

}
