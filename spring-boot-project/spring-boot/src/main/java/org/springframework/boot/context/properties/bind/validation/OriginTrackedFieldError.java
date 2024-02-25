/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.context.properties.bind.validation;

import org.springframework.boot.origin.Origin;
import org.springframework.boot.origin.OriginProvider;
import org.springframework.validation.FieldError;

/**
 * {@link FieldError} implementation that tracks the source {@link Origin}.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 */
final class OriginTrackedFieldError extends FieldError implements OriginProvider {

	private final Origin origin;

	/**
	 * Constructs a new OriginTrackedFieldError with the specified FieldError and Origin.
	 * @param fieldError the FieldError object containing information about the field
	 * error
	 * @param origin the Origin object representing the origin of the field error
	 */
	private OriginTrackedFieldError(FieldError fieldError, Origin origin) {
		super(fieldError.getObjectName(), fieldError.getField(), fieldError.getRejectedValue(),
				fieldError.isBindingFailure(), fieldError.getCodes(), fieldError.getArguments(),
				fieldError.getDefaultMessage());
		this.origin = origin;
	}

	/**
	 * Returns the origin of the OriginTrackedFieldError.
	 * @return the origin of the OriginTrackedFieldError
	 */
	@Override
	public Origin getOrigin() {
		return this.origin;
	}

	/**
	 * Returns a string representation of the object. If the origin is null, it returns
	 * the default string representation. Otherwise, it returns the default string
	 * representation appended with the origin.
	 * @return a string representation of the object
	 */
	@Override
	public String toString() {
		if (this.origin == null) {
			return super.toString();
		}
		return super.toString() + "; origin " + this.origin;
	}

	/**
	 * Creates a new OriginTrackedFieldError object based on the provided FieldError and
	 * Origin.
	 * @param fieldError the original FieldError object
	 * @param origin the Origin object representing the source of the error
	 * @return a new OriginTrackedFieldError object if both fieldError and origin are not
	 * null, otherwise returns the original FieldError object
	 */
	static FieldError of(FieldError fieldError, Origin origin) {
		if (fieldError == null || origin == null) {
			return fieldError;
		}
		return new OriginTrackedFieldError(fieldError, origin);
	}

}
