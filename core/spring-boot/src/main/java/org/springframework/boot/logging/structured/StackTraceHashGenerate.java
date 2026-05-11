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

package org.springframework.boot.logging.structured;

/**
 * The mode for generating stack trace hashes in structured logging.
 *
 * @author [Venkata Naga Sai Srikanth Gollapudi]
 * @since 4.0
 */
public enum StackTraceHashGenerate {

	/**
	 * Include the hash inline as part of the stack trace string. This preserves the
	 * existing behavior where the hash is prefixed to the stack trace text.
	 */
	INLINE,

	/**
	 * Include the hash as a separate structured JSON field. The field name can be
	 * customized using the {@code field-name} property, otherwise a sensible default is
	 * used for the selected logging format.
	 */
	AS_FIELD

}
