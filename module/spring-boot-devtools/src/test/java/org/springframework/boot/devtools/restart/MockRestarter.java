/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.devtools.restart;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolver;

import org.springframework.beans.factory.ObjectFactory;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Mocked version of {@link Restarter}.
 *
 * @author Phillip Webb
 */
public class MockRestarter implements BeforeEachCallback, AfterEachCallback, ParameterResolver {

	private final Map<String, Object> attributes = new HashMap<>();

	private final Restarter mock = mock(Restarter.class);

	public Restarter getMock() {
		return this.mock;
	}

	@Override
	public void afterEach(ExtensionContext context) throws Exception {
		this.attributes.clear();
		Restarter.clearInstance();
	}

	@Override
	public void beforeEach(ExtensionContext context) throws Exception {
		Restarter.setInstance(this.mock);
		given(this.mock.getInitialUrls()).willReturn(new URL[] {});
		given(this.mock.getOrAddAttribute(anyString(), any(ObjectFactory.class))).willAnswer((invocation) -> {
			String name = invocation.getArgument(0);
			ObjectFactory<?> factory = invocation.getArgument(1);
			Object attribute = MockRestarter.this.attributes.get(name);
			if (attribute == null) {
				attribute = factory.getObject();
				MockRestarter.this.attributes.put(name, attribute);
			}
			return attribute;
		});
		given(this.mock.getThreadFactory()).willReturn(Thread::new);
	}

	@Override
	public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
		return parameterContext.getParameter().getType().equals(Restarter.class);
	}

	@Override
	public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
		return this.mock;
	}

}
