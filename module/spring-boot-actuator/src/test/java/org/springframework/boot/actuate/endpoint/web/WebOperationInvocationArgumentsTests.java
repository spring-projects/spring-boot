/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.actuate.endpoint.web;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link WebOperationInvocationArguments}.
 *
 * @author Phillip Webb
 */
class WebOperationInvocationArgumentsTests {

	private final WebOperationRequestPredicate requestPredicate = new WebOperationRequestPredicate("/root/{one}/{*two}",
			WebEndpointHttpMethod.POST, List.of("application/json"), List.of("application/json"));

	private final WebOperation webOperation;

	WebOperationInvocationArgumentsTests() {
		this.webOperation = mock();
		given(this.webOperation.getRequestPredicate()).willReturn(this.requestPredicate);
	}

	@Test
	void getWhenBuiltWithQueryParameters() {
		Map<String, Object> result = WebOperationInvocationArguments.get(this.webOperation, (invocationArguments) -> {
			Map<String, Object> queryParameters = new LinkedHashMap<>();
			queryParameters.put("a", "A");
			queryParameters.put("b", new String[] { "B" });
			queryParameters.put("c", List.of("C"));
			queryParameters.put("d", List.of("D", "DD"));
			invocationArguments.queryParameters(queryParameters);
		});
		assertThat(result).containsOnlyKeys("a", "b", "c", "d");
		assertThat(result.get("a")).isEqualTo("A");
		assertThat(result.get("b")).isEqualTo("B");
		assertThat(result.get("c")).isEqualTo("C");
		assertThat(result.get("d")).isEqualTo(List.of("D", "DD"));
	}

	@Test
	void getWhenBuiltWithPathParameters() {
		Map<String, Object> result = WebOperationInvocationArguments.get(this.webOperation, (invocationArguments) -> {
			Map<String, Object> pathParameters = new LinkedHashMap<>();
			pathParameters.put("a", "A");
			pathParameters.put("b", new String[] { "B" });
			pathParameters.put("c", List.of("C"));
			pathParameters.put("d", List.of("D", "DD"));
			invocationArguments.pathParameters(pathParameters);
		});
		assertThat(result).containsOnlyKeys("a", "b", "c", "d");
		assertThat(result.get("a")).isEqualTo("A");
		assertThat(result.get("b")).isEqualTo("B");
		assertThat(result.get("c")).isEqualTo("C");
		assertThat(result.get("d")).isEqualTo(List.of("D", "DD"));
	}

	@Test
	void getWhenBuiltWithBody() {
		Map<String, Object> result = WebOperationInvocationArguments.get(this.webOperation, (invocationArguments) -> {
			Map<String, String> body = new LinkedHashMap<>();
			body.put("a", "A");
			body.put("b", "B");
			invocationArguments.body(body);
		});
		assertThat(result).containsOnlyKeys("a", "b");
		assertThat(result.get("a")).isEqualTo("A");
		assertThat(result.get("b")).isEqualTo("B");
	}

	@Test
	void getWhenBuiltWithRemainingPathSeqments() {
		Map<String, Object> result = WebOperationInvocationArguments.get(this.webOperation,
				(invocationArguments) -> invocationArguments
					.remainPathSegments((variable) -> new String[] { "T", "W", "O" }));
		assertThat(result).containsOnlyKeys("two");
		assertThat(result.get("two")).isEqualTo(new String[] { "T", "W", "O" });
	}

	@Test
	void getWhenBuiltWithNonEmptyPathSegmentAndClashingQueryParameterUsesPathSegment() {
		Map<String, Object> result = WebOperationInvocationArguments.get(this.webOperation, (invocationArguments) -> {
			invocationArguments.queryParameters(Map.of("two", "2"));
			invocationArguments.remainPathSegments((variable) -> new String[] { "T", "W", "O" });
		});
		assertThat(result).containsOnlyKeys("two");
		assertThat(result.get("two")).isEqualTo(new String[] { "T", "W", "O" });
	}

	@Test
	void getWhenBuiltWithNonEmptyPathSegmentAndClashingPathParameterUsesPathSegment() {
		Map<String, Object> result = WebOperationInvocationArguments.get(this.webOperation, (invocationArguments) -> {
			invocationArguments.pathParameters(Map.of("two", "2"));
			invocationArguments.remainPathSegments((variable) -> new String[] { "T", "W", "O" });
		});
		assertThat(result).containsOnlyKeys("two");
		assertThat(result.get("two")).isEqualTo(new String[] { "T", "W", "O" });
	}

	@Test
	void getWhenBuiltWithNonEmptyPathSegmentAndClashingBodyUsesPathSegment() {
		Map<String, Object> result = WebOperationInvocationArguments.get(this.webOperation, (invocationArguments) -> {
			invocationArguments.body(Map.of("two", "2"));
			invocationArguments.remainPathSegments((variable) -> new String[] { "T", "W", "O" });
		});
		assertThat(result).containsOnlyKeys("two");
		assertThat(result.get("two")).isEqualTo(new String[] { "T", "W", "O" });
	}

	@Test
	void getWhenHasGetMatchAllRemainingPathSegmentsVariableButNoRemainPathSegmentsRemovesQueryParameter() {
		Map<String, Object> result = WebOperationInvocationArguments.get(this.webOperation,
				(invocationArguments) -> invocationArguments.pathParameters(Map.of("two", "2")));
		assertThat(result).doesNotContainKey("two");
	}

}
