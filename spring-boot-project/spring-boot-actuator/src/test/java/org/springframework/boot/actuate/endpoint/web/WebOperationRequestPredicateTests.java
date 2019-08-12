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

package org.springframework.boot.actuate.endpoint.web;

import java.util.Collections;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link WebOperationRequestPredicate}.
 *
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
class WebOperationRequestPredicateTests {

	@Test
	void predicatesWithIdenticalPathsAreEqual() {
		assertThat(predicateWithPath("/path")).isEqualTo(predicateWithPath("/path"));
	}

	@Test
	void predicatesWithDifferentPathsAreNotEqual() {
		assertThat(predicateWithPath("/one")).isNotEqualTo(predicateWithPath("/two"));
	}

	@Test
	void predicatesWithIdenticalPathsWithVariablesAreEqual() {
		assertThat(predicateWithPath("/path/{foo}")).isEqualTo(predicateWithPath("/path/{foo}"));
	}

	@Test
	void predicatesWhereOneHasAPathAndTheOtherHasAVariableAreNotEqual() {
		assertThat(predicateWithPath("/path/{foo}")).isNotEqualTo(predicateWithPath("/path/foo"));
	}

	@Test
	void predicatesWithSinglePathVariablesInTheSamplePlaceAreEqual() {
		assertThat(predicateWithPath("/path/{foo1}")).isEqualTo(predicateWithPath("/path/{foo2}"));
	}

	@Test
	void predicatesWithSingleWildcardPathVariablesInTheSamplePlaceAreEqual() {
		assertThat(predicateWithPath("/path/{*foo1}")).isEqualTo(predicateWithPath("/path/{*foo2}"));
	}

	@Test
	void predicatesWithSingleWildcardPathVariableAndRegularVariableInTheSamplePlaceAreNotEqual() {
		assertThat(predicateWithPath("/path/{*foo1}")).isNotEqualTo(predicateWithPath("/path/{foo2}"));
	}

	@Test
	void predicatesWithMultiplePathVariablesInTheSamplePlaceAreEqual() {
		assertThat(predicateWithPath("/path/{foo1}/more/{bar1}"))
				.isEqualTo(predicateWithPath("/path/{foo2}/more/{bar2}"));
	}

	@Test
	void predicateWithWildcardPathVariableReturnsMatchAllRemainingPathSegmentsVariable() {
		assertThat(predicateWithPath("/path/{*foo1}").getMatchAllRemainingPathSegmentsVariable()).isEqualTo("foo1");
	}

	@Test
	void predicateWithRegularPathVariableDoesNotReturnMatchAllRemainingPathSegmentsVariable() {
		assertThat(predicateWithPath("/path/{foo1}").getMatchAllRemainingPathSegmentsVariable()).isNull();
	}

	@Test
	void predicateWithNoPathVariableDoesNotReturnMatchAllRemainingPathSegmentsVariable() {
		assertThat(predicateWithPath("/path/foo1").getMatchAllRemainingPathSegmentsVariable()).isNull();
	}

	private WebOperationRequestPredicate predicateWithPath(String path) {
		return new WebOperationRequestPredicate(path, WebEndpointHttpMethod.GET, Collections.emptyList(),
				Collections.emptyList());
	}

}
