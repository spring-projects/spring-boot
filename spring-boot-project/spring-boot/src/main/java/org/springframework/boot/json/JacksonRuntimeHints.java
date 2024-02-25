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

package org.springframework.boot.json;

import com.fasterxml.jackson.databind.ser.std.ClassSerializer;
import com.fasterxml.jackson.databind.ser.std.FileSerializer;
import com.fasterxml.jackson.databind.ser.std.StdJdkSerializers.AtomicBooleanSerializer;
import com.fasterxml.jackson.databind.ser.std.StdJdkSerializers.AtomicIntegerSerializer;
import com.fasterxml.jackson.databind.ser.std.StdJdkSerializers.AtomicLongSerializer;
import com.fasterxml.jackson.databind.ser.std.TokenBufferSerializer;

import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.ReflectionHints;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.aot.hint.TypeHint;
import org.springframework.aot.hint.TypeReference;
import org.springframework.util.ClassUtils;

/**
 * {@link RuntimeHintsRegistrar} implementation for Jackson.
 *
 * @author Moritz Halbritter
 */
class JacksonRuntimeHints implements RuntimeHintsRegistrar {

	/**
     * Registers hints for runtime serialization using Jackson library.
     * 
     * @param hints the runtime hints to register
     * @param classLoader the class loader to use for checking class availability
     */
    @Override
	public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
		if (!ClassUtils.isPresent("com.fasterxml.jackson.databind.ser.BasicSerializerFactory", classLoader)) {
			return;
		}
		registerSerializers(hints.reflection());
	}

	/**
     * Registers serializers for specified types using the given reflection hints.
     * 
     * @param hints the reflection hints to use for registration
     */
    private void registerSerializers(ReflectionHints hints) {
		hints.registerTypes(TypeReference.listOf(AtomicBooleanSerializer.class, AtomicIntegerSerializer.class,
				AtomicLongSerializer.class, FileSerializer.class, ClassSerializer.class, TokenBufferSerializer.class),
				TypeHint.builtWith(MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS));
	}

}
