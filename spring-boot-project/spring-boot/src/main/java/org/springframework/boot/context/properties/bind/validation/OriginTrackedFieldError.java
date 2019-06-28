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

	private OriginTrackedFieldError(FieldError fieldError, Origin origin) {
		super(fieldError.getObjectName(), fieldError.getField(), fieldError.getRejectedValue(),
				fieldError.isBindingFailure(), fieldError.getCodes(), fieldError.getArguments(),
				fieldError.getDefaultMessage());
		this.origin = origin;
	}

	@Override
	public Origin getOrigin() {
		return this.origin;
	}

	@Override
	public String toString() {
		if (this.origin == null) {
			return super.toString();
		}
		return super.toString() + "; origin " + this.origin;
	}

	public static FieldError of(FieldError fieldError, Origin origin) {
		if (fieldError == null || origin == null) {
			return fieldError;
		}
		return new OriginTrackedFieldError(fieldError, origin);
	}

}
