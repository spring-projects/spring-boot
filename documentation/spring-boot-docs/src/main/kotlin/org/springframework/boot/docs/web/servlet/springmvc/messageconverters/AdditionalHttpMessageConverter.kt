/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.web.servlet.springmvc.messageconverters

import org.springframework.http.HttpInputMessage
import org.springframework.http.HttpOutputMessage
import org.springframework.http.converter.AbstractHttpMessageConverter
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.http.converter.HttpMessageNotWritableException
import java.io.IOException

open class AdditionalHttpMessageConverter : AbstractHttpMessageConverter<Any>() {

	override fun supports(type: Class<*>): Boolean {
		return false
	}

	@Throws(IOException::class, HttpMessageNotReadableException::class)
	override fun readInternal(type: Class<*>, inputMessage: HttpInputMessage): Any {
		return Any()
	}

	@Throws(IOException::class, HttpMessageNotWritableException::class)
	override fun writeInternal(instance: Any, outputMessage: HttpOutputMessage) {
	}

}

