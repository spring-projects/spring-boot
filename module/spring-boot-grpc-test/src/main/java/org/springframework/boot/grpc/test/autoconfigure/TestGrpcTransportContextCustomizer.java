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

package org.springframework.boot.grpc.test.autoconfigure;

import java.util.Objects;

import io.grpc.inprocess.InProcessServerBuilder;
import org.jspecify.annotations.Nullable;

import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.MergedContextConfiguration;

/**
 * {@link ContextCustomizer} for {@link AutoConfigureTestGrpcTransport}.
 * <p>
 * Equality is based on annotation attributes so the annotation participates in
 * {@link MergedContextConfiguration}. The in-process name is generated once per
 * context creation inside {@link #customizeContext} (not as a JVM-wide static), so
 * cached contexts keep a single name while distinct contexts get distinct names.
 *
 * @author xing
 * @since 4.1.0
 */
class TestGrpcTransportContextCustomizer implements ContextCustomizer {

	/**
	 * Test-only property used by {@link TestGrpcTransportAutoConfiguration} to bind the
	 * in-process transport name for the current application context.
	 */
	static final String INPROCESS_NAME_PROPERTY = "spring.grpc.test.transport.inprocess-name";

	private final boolean enableServlet;

	private final boolean enableServerFactory;

	private final boolean enableChannelFactory;

	TestGrpcTransportContextCustomizer(AutoConfigureTestGrpcTransport annotation) {
		this.enableServlet = annotation.enableServlet();
		this.enableServerFactory = annotation.enableServerFactory();
		this.enableChannelFactory = annotation.enableChannelFactory();
	}

	@Override
	public void customizeContext(ConfigurableApplicationContext context,
			MergedContextConfiguration mergedContextConfiguration) {
		// Generate per context creation. Spring Test does not re-run this for a cached
		// context, so reuse keeps the same name; a new MergedContextConfiguration gets a
		// new name without relying on a static field in auto-configuration.
		String name = InProcessServerBuilder.generateName();
		TestPropertyValues.of(INPROCESS_NAME_PROPERTY + "=" + name).applyTo(context);
	}

	@Override
	public boolean equals(@Nullable Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		TestGrpcTransportContextCustomizer other = (TestGrpcTransportContextCustomizer) obj;
		return this.enableServlet == other.enableServlet && this.enableServerFactory == other.enableServerFactory
				&& this.enableChannelFactory == other.enableChannelFactory;
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.enableServlet, this.enableServerFactory, this.enableChannelFactory);
	}

}
