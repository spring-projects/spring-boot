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

package org.springframework.boot.test.autoconfigure.grpc;

import java.util.List;
import java.util.Objects;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ContextConfigurationAttributes;
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.ContextCustomizerFactory;
import org.springframework.test.context.MergedContextConfiguration;
import org.springframework.test.context.TestContextAnnotationUtils;

/**
 * {@link ContextCustomizerFactory} that starts an in-process gRPC server and replaces the
 * regular server and channel factories (e.g. Netty). The customizer can be disabled via
 * the {@link AutoConfigureInProcessTransport} annotation or the
 * {@value #ENABLED_PROPERTY} property.
 *
 * @author Chris Bono
 */
class InProcessTransportContextCustomizerFactory implements ContextCustomizerFactory {

	static final String ENABLED_PROPERTY = "spring.test.grpc.inprocess.enabled";

	@Override
	public ContextCustomizer createContextCustomizer(Class<?> testClass,
			List<ContextConfigurationAttributes> configAttributes) {
		AutoConfigureInProcessTransport annotation = TestContextAnnotationUtils.findMergedAnnotation(testClass,
				AutoConfigureInProcessTransport.class);
		return new InProcessTransportContextCustomizer(annotation);
	}

	private static class InProcessTransportContextCustomizer implements ContextCustomizer {

		private final @Nullable AutoConfigureInProcessTransport annotation;

		InProcessTransportContextCustomizer(@Nullable AutoConfigureInProcessTransport annotation) {
			this.annotation = annotation;
		}

		@Override
		public void customizeContext(ConfigurableApplicationContext context,
				MergedContextConfiguration mergedContextConfiguration) {
			if (this.annotation == null
					|| !context.getEnvironment().getProperty(ENABLED_PROPERTY, Boolean.class, false)) {
				return;
			}
			TestPropertyValues
				.of("spring.grpc.client.inprocess.exclusive=true", "spring.grpc.server.inprocess.exclusive=true")
				.applyTo(context);
		}

		@Override
		public boolean equals(@Nullable Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}
			InProcessTransportContextCustomizer that = (InProcessTransportContextCustomizer) o;
			return Objects.equals(this.annotation, that.annotation);
		}

		@Override
		public int hashCode() {
			return Objects.hash(this.annotation);
		}

	}

}
