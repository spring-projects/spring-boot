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

import java.util.List;

import org.jspecify.annotations.Nullable;

import org.springframework.test.context.ContextConfigurationAttributes;
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.ContextCustomizerFactory;
import org.springframework.test.context.TestContextAnnotationUtils;

/**
 * {@link ContextCustomizerFactory} that integrates
 * {@link AutoConfigureTestGrpcTransport} with the Spring Test context cache key and
 * assigns a per-context in-process transport name.
 * <p>
 * The generated name is written as a test-only property
 * ({@value TestGrpcTransportContextCustomizer#INPROCESS_NAME_PROPERTY}) rather than
 * {@code spring.grpc.server.inprocess.name}, which is reserved for application-level
 * in-process servers.
 *
 * @author xing
 * @since 4.1.0
 * @see AutoConfigureTestGrpcTransport
 * @see TestGrpcTransportContextCustomizer
 */
class TestGrpcTransportContextCustomizerFactory implements ContextCustomizerFactory {

	@Override
	public @Nullable ContextCustomizer createContextCustomizer(Class<?> testClass,
			List<ContextConfigurationAttributes> configAttributes) {
		AutoConfigureTestGrpcTransport annotation = TestContextAnnotationUtils.findMergedAnnotation(testClass,
				AutoConfigureTestGrpcTransport.class);
		if (annotation == null) {
			return null;
		}
		return new TestGrpcTransportContextCustomizer(annotation);
	}

}
