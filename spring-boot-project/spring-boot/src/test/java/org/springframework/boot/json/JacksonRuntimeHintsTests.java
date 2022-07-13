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

import java.util.Set;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.ser.std.ClassSerializer;
import com.fasterxml.jackson.databind.ser.std.FileSerializer;
import com.fasterxml.jackson.databind.ser.std.StdJdkSerializers.AtomicBooleanSerializer;
import com.fasterxml.jackson.databind.ser.std.StdJdkSerializers.AtomicIntegerSerializer;
import com.fasterxml.jackson.databind.ser.std.StdJdkSerializers.AtomicLongSerializer;
import com.fasterxml.jackson.databind.ser.std.TokenBufferSerializer;
import org.junit.jupiter.api.Test;

import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.ReflectionHints;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.TypeHint;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link JacksonRuntimeHints}.
 *
 * @author Moritz Halbritter
 */
class JacksonRuntimeHintsTests {

	@Test
	void shouldRegisterSerializerConstructors() {
		ReflectionHints hints = registerHints();
		Stream.of(AtomicBooleanSerializer.class, AtomicIntegerSerializer.class, AtomicLongSerializer.class,
				FileSerializer.class, ClassSerializer.class, TokenBufferSerializer.class).forEach((serializer) -> {
					TypeHint typeHint = hints.getTypeHint(serializer);
					assertThat(typeHint).withFailMessage(() -> "No hints found for serializer " + serializer)
							.isNotNull();
					Set<MemberCategory> memberCategories = typeHint.getMemberCategories();
					assertThat(memberCategories).containsExactly(MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS);
				});
	}

	private ReflectionHints registerHints() {
		RuntimeHints hints = new RuntimeHints();
		new JacksonRuntimeHints().registerHints(hints, getClass().getClassLoader());
		return hints.reflection();
	}

}
