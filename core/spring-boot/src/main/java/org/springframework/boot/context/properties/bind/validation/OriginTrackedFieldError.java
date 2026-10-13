/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.context.properties.bind.validation;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.origin.Origin;
import org.springframework.boot.origin.OriginProvider;
import org.springframework.lang.Contract;
import org.springframework.validation.FieldError;

/**
 * {@link FieldError} implementation that tracks the source {@link Origin}.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 */
final class OriginTrackedFieldError extends FieldError implements OriginProvider {

	private final @Nullable Origin origin;

	private OriginTrackedFieldError(FieldError fieldError, @Nullable Origin origin) {
		super(fieldError.getObjectName(), fieldError.getField(), fieldError.getRejectedValue(),
				fieldError.isBindingFailure(), fieldError.getCodes(), fieldError.getArguments(),
				fieldError.getDefaultMessage());
		this.origin = origin;
	}

	@Override
	public @Nullable Origin getOrigin() {
		return this.origin;
	}

	@Override
	public String toString() {
		if (this.origin == null) {
			return super.toString();
		}
		return super.toString() + "; origin " + this.origin;
	}

	@Contract("!null, _ -> !null")
	static @Nullable FieldError of(@Nullable FieldError fieldError, @Nullable Origin origin) {
		if (fieldError == null || origin == null) {
			return fieldError;
		}
		return new OriginTrackedFieldError(fieldError, origin);
	}

}
