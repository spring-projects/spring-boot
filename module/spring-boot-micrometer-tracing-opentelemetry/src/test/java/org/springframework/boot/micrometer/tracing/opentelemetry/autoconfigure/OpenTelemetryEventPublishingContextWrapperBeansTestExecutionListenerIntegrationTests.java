/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.micrometer.tracing.opentelemetry.autoconfigure;

import java.lang.reflect.Method;
import java.util.List;
import java.util.function.Function;

import io.opentelemetry.context.ContextStorage;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import org.springframework.boot.micrometer.tracing.opentelemetry.autoconfigure.OpenTelemetryEventPublisherBeansApplicationListener.Wrapper.Storage;
import org.springframework.boot.testsupport.classpath.ForkedClassPath;

import static org.assertj.core.api.Assertions.assertThat;

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
		assertThat(wrappers).anyMatch((function) -> function.apply(this.parent) instanceof Storage);
	}

}
