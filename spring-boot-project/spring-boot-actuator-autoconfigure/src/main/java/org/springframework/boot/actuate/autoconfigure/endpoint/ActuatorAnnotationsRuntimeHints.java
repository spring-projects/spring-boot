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

package org.springframework.boot.actuate.autoconfigure.endpoint;

import java.util.stream.Stream;

import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.aot.hint.support.RuntimeHintsUtils;
import org.springframework.boot.actuate.endpoint.annotation.DeleteOperation;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.EndpointExtension;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.annotation.WriteOperation;
import org.springframework.core.annotation.SynthesizedAnnotation;

/**
 * {@link RuntimeHintsRegistrar} for actuator support.
 *
 * @author Moritz Halbritter
 */
class ActuatorAnnotationsRuntimeHints implements RuntimeHintsRegistrar {

	@Override
	public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
		Stream.of(Endpoint.class, ReadOperation.class, WriteOperation.class, DeleteOperation.class,
				EndpointExtension.class)
				.forEach((annotationType) -> RuntimeHintsUtils.registerAnnotation(hints, annotationType));
		// TODO: See https://github.com/spring-projects/spring-framework/issues/28767
		Stream.of(Endpoint.class, EndpointExtension.class).forEach(
				(annotationType) -> hints.proxies().registerJdkProxy(annotationType, SynthesizedAnnotation.class));
	}

}
