/*
 * Copyright 2012-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.bind;

import org.springframework.beans.NotWritablePropertyException;

/**
 * A custom {@link NotWritablePropertyException} that is thrown when a failure occurs
 * during relaxed binding.
 *
 * @author Andy Wilkinson
 * @since 1.3.0
 * @see RelaxedDataBinder
 */
public class RelaxedBindingNotWritablePropertyException
		extends NotWritablePropertyException {

	private final String message;

	private final PropertyOrigin propertyOrigin;

	RelaxedBindingNotWritablePropertyException(NotWritablePropertyException ex,
			PropertyOrigin propertyOrigin) {
		super(ex.getBeanClass(), ex.getPropertyName());
		this.propertyOrigin = propertyOrigin;
		this.message = "Failed to bind '" + propertyOrigin.getName() + "' from '"
				+ propertyOrigin.getSource().getName() + "' to '" + ex.getPropertyName()
				+ "' property on '" + ex.getBeanClass().getName() + "'";
	}

	@Override
	public String getMessage() {
		return this.message;
	}

	public PropertyOrigin getPropertyOrigin() {
		return this.propertyOrigin;
	}

}
