/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.testcontainers.service.connection;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.elasticsearch.ElasticsearchContainer;

import org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchConnectionDetails;
import org.springframework.boot.autoconfigure.jdbc.JdbcConnectionDetails;
import org.springframework.boot.origin.Origin;
import org.springframework.core.annotation.MergedAnnotation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link ContainerConnectionSource}.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
class ContainerConnectionSourceTests {

	private String beanNameSuffix;

	private Origin origin;

	private PostgreSQLContainer<?> container;

	private MergedAnnotation<ServiceConnection> annotation;

	private ContainerConnectionSource<?> source;

	@BeforeEach
	void setup() {
		this.beanNameSuffix = "MyBean";
		this.origin = mock(Origin.class);
		this.container = mock(PostgreSQLContainer.class);
		given(this.container.getDockerImageName()).willReturn("postgres");
		this.annotation = MergedAnnotation.of(ServiceConnection.class, Map.of("name", "", "type", new Class<?>[0]));
		this.source = new ContainerConnectionSource<>(this.beanNameSuffix, this.origin, PostgreSQLContainer.class,
				this.container.getDockerImageName(), this.annotation, () -> this.container);
	}

	@Test
	void acceptsWhenContainerIsNotInstanceOfRequiredContainerTypeReturnsFalse() {
		String requiredConnectionName = null;
		Class<?> requiredContainerType = ElasticsearchContainer.class;
		Class<?> requiredConnectionDetailsType = JdbcConnectionDetails.class;
		assertThat(this.source.accepts(requiredConnectionName, requiredContainerType, requiredConnectionDetailsType))
			.isFalse();
	}

	@Test
	void acceptsWhenContainerIsInstanceOfRequiredContainerTypeReturnsTrue() {
		String requiredConnectionName = null;
		Class<?> requiredContainerType = JdbcDatabaseContainer.class;
		Class<?> requiredConnectionDetailsType = JdbcConnectionDetails.class;
		assertThat(this.source.accepts(requiredConnectionName, requiredContainerType, requiredConnectionDetailsType))
			.isTrue();
	}

	@Test
	void acceptsWhenRequiredConnectionNameDoesNotMatchNameTakenFromAnnotationReturnsFalse() {
		setupSourceAnnotatedWithName("myname");
		String requiredConnectionName = "othername";
		Class<?> requiredContainerType = JdbcDatabaseContainer.class;
		Class<?> requiredConnectionDetailsType = JdbcConnectionDetails.class;
		assertThat(this.source.accepts(requiredConnectionName, requiredContainerType, requiredConnectionDetailsType))
			.isFalse();
	}

	@Test
	void acceptsWhenRequiredConnectionNameDoesNotMatchNameTakenFromContainerReturnsFalse() {
		String requiredConnectionName = "othername";
		Class<?> requiredContainerType = JdbcDatabaseContainer.class;
		Class<?> requiredConnectionDetailsType = JdbcConnectionDetails.class;
		assertThat(this.source.accepts(requiredConnectionName, requiredContainerType, requiredConnectionDetailsType))
			.isFalse();
	}

	@Test
	void acceptsWhenRequiredConnectionNameIsUnrestrictedReturnsTrue() {
		String requiredConnectionName = null;
		Class<?> requiredContainerType = JdbcDatabaseContainer.class;
		Class<?> requiredConnectionDetailsType = JdbcConnectionDetails.class;
		assertThat(this.source.accepts(requiredConnectionName, requiredContainerType, requiredConnectionDetailsType))
			.isTrue();
	}

	@Test
	void acceptsWhenRequiredConnectionNameMatchesNameTakenFromAnnotationReturnsTrue() {
		setupSourceAnnotatedWithName("myname");
		String requiredConnectionName = "myname";
		Class<?> requiredContainerType = JdbcDatabaseContainer.class;
		Class<?> requiredConnectionDetailsType = JdbcConnectionDetails.class;
		assertThat(this.source.accepts(requiredConnectionName, requiredContainerType, requiredConnectionDetailsType))
			.isTrue();
	}

	@Test
	void acceptsWhenRequiredConnectionNameMatchesNameTakenFromContainerReturnsTrue() {
		String requiredConnectionName = "postgres";
		Class<?> requiredContainerType = JdbcDatabaseContainer.class;
		Class<?> requiredConnectionDetailsType = JdbcConnectionDetails.class;
		assertThat(this.source.accepts(requiredConnectionName, requiredContainerType, requiredConnectionDetailsType))
			.isTrue();
	}

	@Test
	void acceptsWhenRequiredConnectionDetailsTypeNotInAnnotationRestrictionReturnsFalse() {
		setupSourceAnnotatedWithType(ElasticsearchConnectionDetails.class);
		String requiredConnectionName = null;
		Class<?> requiredContainerType = JdbcDatabaseContainer.class;
		Class<?> requiredConnectionDetailsType = JdbcConnectionDetails.class;
		assertThat(this.source.accepts(requiredConnectionName, requiredContainerType, requiredConnectionDetailsType))
			.isFalse();
	}

	@Test
	void acceptsWhenRequiredConnectionDetailsTypeInAnnotationRestrictionReturnsTrue() {
		setupSourceAnnotatedWithType(JdbcConnectionDetails.class);
		String requiredConnectionName = null;
		Class<?> requiredContainerType = JdbcDatabaseContainer.class;
		Class<?> requiredConnectionDetailsType = JdbcConnectionDetails.class;
		assertThat(this.source.accepts(requiredConnectionName, requiredContainerType, requiredConnectionDetailsType))
			.isTrue();
	}

	@Test
	void acceptsWhenRequiredConnectionDetailsTypeIsNotRestrictedReturnsTrue() {
		String requiredConnectionName = null;
		Class<?> requiredContainerType = JdbcDatabaseContainer.class;
		Class<?> requiredConnectionDetailsType = JdbcConnectionDetails.class;
		assertThat(this.source.accepts(requiredConnectionName, requiredContainerType, requiredConnectionDetailsType))
			.isTrue();
	}

	@Test
	void getBeanNameSuffixReturnsBeanNameSuffix() {
		assertThat(this.source.getBeanNameSuffix()).isEqualTo(this.beanNameSuffix);
	}

	@Test
	void getOriginReturnsOrigin() {
		assertThat(this.source.getOrigin()).isEqualTo(this.origin);
	}

	@Test
	void getContainerSupplierReturnsSupplierSupplyingContainer() {
		assertThat(this.source.getContainerSupplier().get()).isSameAs(this.container);
	}

	@Test
	void toStringReturnsSensibleString() {
		assertThat(this.source.toString()).startsWith("@ServiceConnection source for Mock for Origin");
	}

	private void setupSourceAnnotatedWithName(String name) {
		this.annotation = MergedAnnotation.of(ServiceConnection.class, Map.of("name", name, "type", new Class<?>[0]));
		this.source = new ContainerConnectionSource<>(this.beanNameSuffix, this.origin, PostgreSQLContainer.class,
				this.container.getDockerImageName(), this.annotation, () -> this.container);
	}

	private void setupSourceAnnotatedWithType(Class<?> type) {
		this.annotation = MergedAnnotation.of(ServiceConnection.class,
				Map.of("name", "", "type", new Class<?>[] { type }));
		this.source = new ContainerConnectionSource<>(this.beanNameSuffix, this.origin, PostgreSQLContainer.class,
				this.container.getDockerImageName(), this.annotation, () -> this.container);
	}

}
