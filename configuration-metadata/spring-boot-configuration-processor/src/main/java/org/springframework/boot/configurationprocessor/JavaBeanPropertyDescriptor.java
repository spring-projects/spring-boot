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
 * A {@link PropertyDescriptor} for a standard JavaBean property.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 */
class JavaBeanPropertyDescriptor extends PropertyDescriptor {

	private final ExecutableElement setter;

	private final VariableElement field;

	private final ExecutableElement factoryMethod;

	JavaBeanPropertyDescriptor(String name, TypeMirror type, TypeElement declaringElement, ExecutableElement getter,
			ExecutableElement setter, VariableElement field, ExecutableElement factoryMethod) {
		super(name, type, declaringElement, getter);
		this.setter = setter;
		this.field = field;
		this.factoryMethod = factoryMethod;
	}

	@Override
	protected Element getSourceElement() {
		return getGetter();
	}

	ExecutableElement getSetter() {
		return this.setter;
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

	@Override
	protected Object resolveDefaultValue(MetadataGenerationEnvironment environment) {
		return environment.getFieldDefaultValue(getDeclaringElement(), this.field);
	}

	@Override
	protected ItemDeprecation resolveItemDeprecation(MetadataGenerationEnvironment environment) {
		return resolveItemDeprecation(environment, getGetter(), this.setter, this.field, this.factoryMethod);
	}

	@Override
	public boolean isProperty(MetadataGenerationEnvironment env) {
		boolean isCollection = env.getTypeUtils().isCollectionOrMap(getType());
		boolean hasGetter = getGetter() != null;
		boolean hasSetter = getSetter() != null;
		return !env.isExcluded(getType()) && hasGetter && (hasSetter || isCollection);
	}

}
