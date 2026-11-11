/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.testsupport.junit;

import java.lang.reflect.Method;
import java.util.stream.Stream;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.support.ParameterDeclarations;
import org.junit.platform.commons.util.Preconditions;

/**
 * An {@link ArgumentsProvider} that provides {@code true} and {@code false} values.
 *
 * @author Scott Frederick
 */
class BooleanArgumentsProvider implements ArgumentsProvider {

	@Override
	public Stream<? extends Arguments> provideArguments(ParameterDeclarations parameterDeclarations,
			ExtensionContext context) {
		Method testMethod = context.getRequiredTestMethod();
		Preconditions.condition(testMethod.getParameterCount() > 0, () -> String.format(
				"@BooleanValueSource cannot provide arguments to method [%s]: the method does not declare any formal parameters.",
				testMethod.toGenericString()));

		return Stream.of(Arguments.arguments(false), Arguments.arguments(true));
	}

}
