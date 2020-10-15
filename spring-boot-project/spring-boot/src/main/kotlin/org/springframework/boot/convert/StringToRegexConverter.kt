/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.convert

import org.springframework.core.convert.TypeDescriptor
import org.springframework.core.convert.converter.Converter
import org.springframework.core.convert.converter.GenericConverter

/**
 * [Converter] to convert from a [String] to a [Regex]
 *
 * @author Mikhael Sokolov
 * @see Regex
 */
internal object StringToRegexConverter : GenericConverter {
	override fun getConvertibleTypes(): Set<GenericConverter.ConvertiblePair> = setOf(GenericConverter.ConvertiblePair(String::class.java, Regex::class.java))
	override fun convert(str: Any?, p1: TypeDescriptor, p2: TypeDescriptor): Any? = (str as? String)?.ifEmpty { return null }?.toRegex(RegexOption.MULTILINE)
}