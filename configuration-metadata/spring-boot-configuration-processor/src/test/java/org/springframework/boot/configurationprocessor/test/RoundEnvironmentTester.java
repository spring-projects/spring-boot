/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.configurationprocessor.test;

import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.TypeElement;

/**
 * A tester utility for {@link RoundEnvironment}.
 *
 * @author Stephane Nicoll
 */
public class RoundEnvironmentTester {

	private final RoundEnvironment roundEnvironment;

	RoundEnvironmentTester(RoundEnvironment roundEnvironment) {
		this.roundEnvironment = roundEnvironment;
	}

	/**
	 * Return the root {@link TypeElement} for the specified {@code type}.
	 * @param type the type of the class
	 * @return the {@link TypeElement}
	 */
	public TypeElement getRootElement(Class<?> type) {
		return (TypeElement) this.roundEnvironment.getRootElements()
			.stream()
			.filter((element) -> element.toString().equals(type.getName()))
			.findFirst()
			.orElseThrow(() -> new IllegalStateException(
					"No element found for " + type + " make sure it is included in the list of classes to compile"));
	}

}
