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

package org.springframework.boot.actuate.endpoint.annotation;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.predicate.RuntimeHintsPredicates;
import org.springframework.boot.actuate.endpoint.web.WebEndpointResponse;
import org.springframework.core.io.Resource;
import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link OperationReflectiveProcessor}.
 *
 * @author Moritz Halbritter
 * @author Stephane Nicoll
 */
class OperationReflectiveProcessorTests {

	private final OperationReflectiveProcessor processor = new OperationReflectiveProcessor();

	private final RuntimeHints runtimeHints = new RuntimeHints();

	@Test
	void shouldRegisterMethodAsInvokable() {
		Method method = ReflectionUtils.findMethod(Methods.class, "string");
		assertThat(method).isNotNull();
		runProcessor(method);
		assertThat(RuntimeHintsPredicates.reflection().onMethodInvocation(method)).accepts(this.runtimeHints);
	}

	@Test
	void shouldRegisterReturnType() {
		Method method = ReflectionUtils.findMethod(Methods.class, "dto");
		assertThat(method).isNotNull();
		runProcessor(method);
		assertHintsForDto();
	}

	@Test
	void shouldRegisterMapValueFromReturnType() {
		Method method = ReflectionUtils.findMethod(Methods.class, "dtos");
		assertThat(method).isNotNull();
		runProcessor(method);
		assertHintsForDto();
	}

	@Test
	void shouldRegisterWebEndpointResponseReturnType() {
		Method method = ReflectionUtils.findMethod(Methods.class, "webEndpointResponse");
		assertThat(method).isNotNull();
		runProcessor(method);
		assertHintsForDto();
		assertThat(RuntimeHintsPredicates.reflection().onType(WebEndpointResponse.class)).rejects(this.runtimeHints);
	}

	@Test
	void shouldNotRegisterResourceReturnType() {
		Method method = ReflectionUtils.findMethod(Methods.class, "resource");
		assertThat(method).isNotNull();
		runProcessor(method);
		assertThat(RuntimeHintsPredicates.reflection().onType(Resource.class)).rejects(this.runtimeHints);
	}

	private void assertHintsForDto() {
		assertThat(RuntimeHintsPredicates.reflection()
			.onType(Dto.class)
			.withMemberCategories(MemberCategory.INVOKE_DECLARED_CONSTRUCTORS)).accepts(this.runtimeHints);
		assertThat(RuntimeHintsPredicates.reflection()
			.onType(NestedDto.class)
			.withMemberCategories(MemberCategory.INVOKE_DECLARED_CONSTRUCTORS)).accepts(this.runtimeHints);
	}

	private void runProcessor(Method method) {
		this.processor.registerReflectionHints(this.runtimeHints.reflection(), method);
	}

	@SuppressWarnings("unused")
	private static final class Methods {

		private @Nullable String string() {
			return null;
		}

		private @Nullable Map<String, List<Dto>> dtos() {
			return null;
		}

		private @Nullable Dto dto() {
			return null;
		}

		private @Nullable WebEndpointResponse<Dto> webEndpointResponse() {
			return null;
		}

		private @Nullable Resource resource() {
			return null;
		}

	}

	@SuppressWarnings("unused")
	public static class Dto {

		private final NestedDto nestedDto = new NestedDto();

		public NestedDto getNestedDto() {
			return this.nestedDto;
		}

	}

	public static class NestedDto {

		private final String string = "some-string";

		public String getString() {
			return this.string;
		}

	}

}
