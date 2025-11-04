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

package org.springframework.boot.micrometer.tracing.opentelemetry.autoconfigure;

import java.lang.reflect.Method;
import java.util.List;
import java.util.function.Function;

import io.opentelemetry.context.ContextStorage;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import org.springframework.boot.micrometer.tracing.opentelemetry.autoconfigure.OpenTelemetryEventPublisherBeansApplicationListener.Wrapper.Storage;
import org.springframework.boot.testsupport.classpath.ForkedClassPath;

/**
 * Integration tests for {@link OpenTelemetryEventPublisherBeansTestExecutionListener}.
 *
 * @author Phillip Webb
 */
@ForkedClassPath
class OpenTelemetryEventPublishingContextWrapperBeansTestExecutionListenerIntegrationTests {

	private final ContextStorage parent = Mockito.mock(ContextStorage.class);

	@Test
	@SuppressWarnings({ "unchecked", "rawtypes" })
	void wrapperIsInstalled() throws Exception {
		Class<?> wrappersClass = Class.forName("io.opentelemetry.context.ContextStorageWrappers");
		Method getWrappersMethod = wrappersClass.getDeclaredMethod("getWrappers");
		getWrappersMethod.setAccessible(true);
		List<Function> wrappers = (List<Function>) getWrappersMethod.invoke(null);
		Assertions.assertThat(wrappers).anyMatch((function) -> function.apply(this.parent) instanceof Storage);
	}

}
