/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.configurationprocessor.ksp

/**
 * A source file to compile in a test.
 *
 * @author Areg Iazychian
 */
internal class SourceFile(val name: String, val content: String) {

	companion object {

		/**
		 * Return a Kotlin source file with the given [name] and [content].
		 */
		fun kotlin(name: String, content: String): SourceFile = SourceFile(name, content.trimIndent())

	}

}
