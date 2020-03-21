/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.actuate.endpoint.web.annotation;

import java.lang.reflect.Method;
import java.util.Collections;

import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.endpoint.OperationType;
import org.springframework.boot.actuate.endpoint.annotation.DiscoveredOperationMethod;
import org.springframework.boot.actuate.endpoint.annotation.Selector;
import org.springframework.boot.actuate.endpoint.annotation.Selector.Match;
import org.springframework.boot.actuate.endpoint.web.EndpointMediaTypes;
import org.springframework.boot.actuate.endpoint.web.WebOperationRequestPredicate;
import org.springframework.core.annotation.AnnotationAttributes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link RequestPredicateFactory}.
 *
 * @author Phillip Webb
 */
class RequestPredicateFactoryTests {

	private final RequestPredicateFactory factory = new RequestPredicateFactory(
			new EndpointMediaTypes(Collections.emptyList(), Collections.emptyList()));

	private String rootPath = "/root";

	@Test
	void getRequestPredicateWhenHasMoreThanOneMatchAllThrowsException() {
		DiscoveredOperationMethod operationMethod = getDiscoveredOperationMethod(MoreThanOneMatchAll.class);
		assertThatIllegalStateException()
				.isThrownBy(() -> this.factory.getRequestPredicate(this.rootPath, operationMethod))
				.withMessage("@Selector annotation with Match.ALL_REMAINING must be unique");
	}

	@Test
	void getRequestPredicateWhenMatchAllIsNotLastParameterThrowsException() {
		DiscoveredOperationMethod operationMethod = getDiscoveredOperationMethod(MatchAllIsNotLastParameter.class);
		assertThatIllegalStateException()
				.isThrownBy(() -> this.factory.getRequestPredicate(this.rootPath, operationMethod))
				.withMessage("@Selector annotation with Match.ALL_REMAINING must be the last parameter");
	}

	@Test
	void getRequestPredicateReturnsPredicateWithPath() {
		DiscoveredOperationMethod operationMethod = getDiscoveredOperationMethod(ValidSelectors.class);
		WebOperationRequestPredicate requestPredicate = this.factory.getRequestPredicate(this.rootPath,
				operationMethod);
		assertThat(requestPredicate.getPath()).isEqualTo("/root/{one}/{*two}");
	}

	private DiscoveredOperationMethod getDiscoveredOperationMethod(Class<?> source) {
		Method method = source.getDeclaredMethods()[0];
		AnnotationAttributes attributes = new AnnotationAttributes();
		attributes.put("produces", "application/json");
		return new DiscoveredOperationMethod(method, OperationType.READ, attributes);
	}

	static class MoreThanOneMatchAll {

		void test(@Selector(match = Match.ALL_REMAINING) String[] one,
				@Selector(match = Match.ALL_REMAINING) String[] two) {
		}

	}

	static class MatchAllIsNotLastParameter {

		void test(@Selector(match = Match.ALL_REMAINING) String[] one, @Selector String[] two) {
		}

	}

	static class ValidSelectors {

		void test(@Selector String[] one, @Selector(match = Match.ALL_REMAINING) String[] two) {
		}

	}

}
