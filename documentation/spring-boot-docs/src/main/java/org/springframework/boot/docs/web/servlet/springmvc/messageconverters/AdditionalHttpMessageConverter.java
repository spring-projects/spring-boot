/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.web.servlet.springmvc.messageconverters;

import java.io.IOException;

import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;

class AdditionalHttpMessageConverter extends AbstractHttpMessageConverter<Object> {

	@Override
	protected boolean supports(Class<?> type) {
		return false;
	}

	@Override
	protected Object readInternal(Class<?> type, HttpInputMessage inputMessage)
			throws IOException, HttpMessageNotReadableException {
		return null;
	}

	@Override
	protected void writeInternal(Object instance, HttpOutputMessage outputMessage)
			throws IOException, HttpMessageNotWritableException {
	}

}
