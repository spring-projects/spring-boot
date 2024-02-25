/*
 * Copyright 2012-2021 the original author or authors.
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

package org.springframework.boot.docs.web.servlet.springmvc.messageconverters;

import java.io.IOException;

import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;

/**
 * AdditionalHttpMessageConverter class.
 */
class AdditionalHttpMessageConverter extends AbstractHttpMessageConverter<Object> {

	/**
	 * Determines whether this HttpMessageConverter can convert the given class type.
	 * @param clazz the class type to check
	 * @return true if this HttpMessageConverter supports the given class type, false
	 * otherwise
	 */
	@Override
	protected boolean supports(Class<?> clazz) {
		return false;
	}

	/**
	 * Reads the HTTP input message and converts it into an object of the specified class.
	 * @param clazz the class to convert the input message to
	 * @param inputMessage the HTTP input message to read from
	 * @return the converted object, or null if the conversion fails
	 * @throws IOException if an I/O error occurs while reading the input message
	 * @throws HttpMessageNotReadableException if the input message cannot be read
	 */
	@Override
	protected Object readInternal(Class<?> clazz, HttpInputMessage inputMessage)
			throws IOException, HttpMessageNotReadableException {
		return null;
	}

	/**
	 * Writes the given object to the HTTP output message.
	 * @param t the object to be written
	 * @param outputMessage the HTTP output message to write to
	 * @throws IOException if an I/O error occurs
	 * @throws HttpMessageNotWritableException if the object cannot be written to the
	 * output message
	 */
	@Override
	protected void writeInternal(Object t, HttpOutputMessage outputMessage)
			throws IOException, HttpMessageNotWritableException {
	}

}
