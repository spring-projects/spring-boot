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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.boot.actuate.endpoint.EndpointId;
import org.springframework.boot.actuate.endpoint.EndpointsSupplier;
import org.springframework.boot.actuate.endpoint.ExposableEndpoint;
import org.springframework.boot.actuate.endpoint.Operation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link PathMappedEndpoints}.
 *
 * @author Phillip Webb
 */
public class PathMappedEndpointsTests {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Test
	public void createWhenSupplierIsNullShouldThrowException() {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("Supplier must not be null");
		new PathMappedEndpoints(null, (WebEndpointsSupplier) null);
	}

	@Test
	public void createWhenSuppliersIsNullShouldThrowException() {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("Suppliers must not be null");
		new PathMappedEndpoints(null, (Collection<EndpointsSupplier<?>>) null);
	}

	@Test
	public void iteratorShouldReturnPathMappedEndpoints() {
		PathMappedEndpoints mapped = createTestMapped(null);
		assertThat(mapped).hasSize(2);
		assertThat(mapped).extracting("endpointId").containsExactly(EndpointId.of("e2"), EndpointId.of("e3"));
	}

	@Test
	public void streamShouldReturnPathMappedEndpoints() {
		PathMappedEndpoints mapped = createTestMapped(null);
		assertThat(mapped.stream()).hasSize(2);
		assertThat(mapped.stream()).extracting("endpointId").containsExactly(EndpointId.of("e2"), EndpointId.of("e3"));
	}

	@Test
	public void getRootPathWhenContainsIdShouldReturnRootPath() {
		PathMappedEndpoints mapped = createTestMapped(null);
		assertThat(mapped.getRootPath(EndpointId.of("e2"))).isEqualTo("p2");
	}

	@Test
	public void getRootPathWhenMissingIdShouldReturnNull() {
		PathMappedEndpoints mapped = createTestMapped(null);
		assertThat(mapped.getRootPath(EndpointId.of("xx"))).isNull();
	}

	@Test
	public void getPathWhenContainsIdShouldReturnRootPath() {
		assertThat(createTestMapped(null).getPath(EndpointId.of("e2"))).isEqualTo("/p2");
		assertThat(createTestMapped("/x").getPath(EndpointId.of("e2"))).isEqualTo("/x/p2");
	}

	@Test
	public void getPathWhenMissingIdShouldReturnNull() {
		PathMappedEndpoints mapped = createTestMapped(null);
		assertThat(mapped.getPath(EndpointId.of("xx"))).isNull();
	}

	@Test
	public void getAllRootPathsShouldReturnAllPaths() {
		PathMappedEndpoints mapped = createTestMapped(null);
		assertThat(mapped.getAllRootPaths()).containsExactly("p2", "p3");
	}

	@Test
	public void getAllPathsShouldReturnAllPaths() {
		assertThat(createTestMapped(null).getAllPaths()).containsExactly("/p2", "/p3");
		assertThat(createTestMapped("/x").getAllPaths()).containsExactly("/x/p2", "/x/p3");
	}

	@Test
	public void getEndpointWhenContainsIdShouldReturnPathMappedEndpoint() {
		PathMappedEndpoints mapped = createTestMapped(null);
		assertThat(mapped.getEndpoint(EndpointId.of("e2")).getRootPath()).isEqualTo("p2");
	}

	@Test
	public void getEndpointWhenMissingIdShouldReturnNull() {
		PathMappedEndpoints mapped = createTestMapped(null);
		assertThat(mapped.getEndpoint(EndpointId.of("xx"))).isNull();
	}

	private PathMappedEndpoints createTestMapped(String basePath) {
		List<ExposableEndpoint<?>> endpoints = new ArrayList<>();
		endpoints.add(mockEndpoint(EndpointId.of("e1")));
		endpoints.add(mockEndpoint(EndpointId.of("e2"), "p2"));
		endpoints.add(mockEndpoint(EndpointId.of("e3"), "p3"));
		endpoints.add(mockEndpoint(EndpointId.of("e4")));
		return new PathMappedEndpoints(basePath, () -> endpoints);
	}

	private TestPathMappedEndpoint mockEndpoint(EndpointId id, String rootPath) {
		TestPathMappedEndpoint endpoint = mock(TestPathMappedEndpoint.class);
		given(endpoint.getEndpointId()).willReturn(id);
		given(endpoint.getRootPath()).willReturn(rootPath);
		return endpoint;
	}

	private TestEndpoint mockEndpoint(EndpointId id) {
		TestEndpoint endpoint = mock(TestEndpoint.class);
		given(endpoint.getEndpointId()).willReturn(id);
		return endpoint;
	}

	interface TestEndpoint extends ExposableEndpoint<Operation> {

	}

	interface TestPathMappedEndpoint extends ExposableEndpoint<Operation>, PathMappedEndpoint {

	}

}
