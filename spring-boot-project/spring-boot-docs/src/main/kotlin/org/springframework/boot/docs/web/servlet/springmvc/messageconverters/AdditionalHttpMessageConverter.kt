/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot.docs.web.servlet.springmvc.messageconverters

import org.springframework.http.HttpInputMessage
import org.springframework.http.HttpOutputMessage
import org.springframework.http.converter.AbstractHttpMessageConverter
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.http.converter.HttpMessageNotWritableException
import java.io.IOException

open class AdditionalHttpMessageConverter : AbstractHttpMessageConverter<Any>() {

	override fun supports(clazz: Class<*>): Boolean {
		return false
	}

	@Throws(IOException::class, HttpMessageNotReadableException::class)
	override fun readInternal(clazz: Class<*>, inputMessage: HttpInputMessage): Any {
		return Any()
	}

	@Throws(IOException::class, HttpMessageNotWritableException::class)
	override fun writeInternal(t: Any, outputMessage: HttpOutputMessage) {
	}

}
