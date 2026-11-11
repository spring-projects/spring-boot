/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.testcontainers.service.connection;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.FutureContainer;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;

import org.springframework.boot.autoconfigure.service.connection.ConnectionDetails;
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

	private PostgreSQLContainer container;

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
				this.container.getDockerImageName(), this.annotation, () -> this.container, null, null);
	}

	@Test
	void acceptsWhenContainerIsNotInstanceOfRequiredContainerTypeReturnsFalse() {
		String requiredConnectionName = null;
		Class<?> requiredContainerType = FutureContainer.class;
		Class<?> requiredConnectionDetailsType = DatabaseConnectionDetails.class;
		assertThat(this.source.accepts(requiredConnectionName, requiredContainerType, requiredConnectionDetailsType))
			.isFalse();
	}

	@Test
	void acceptsWhenContainerIsInstanceOfRequiredContainerTypeReturnsTrue() {
		String requiredConnectionName = null;
		Class<?> requiredContainerType = JdbcDatabaseContainer.class;
		Class<?> requiredConnectionDetailsType = DatabaseConnectionDetails.class;
		assertThat(this.source.accepts(requiredConnectionName, requiredContainerType, requiredConnectionDetailsType))
			.isTrue();
	}

	@Test
	void acceptsWhenRequiredConnectionNameDoesNotMatchNameTakenFromAnnotationReturnsFalse() {
		setupSourceAnnotatedWithName("myname");
		String requiredConnectionName = "othername";
		Class<?> requiredContainerType = JdbcDatabaseContainer.class;
		Class<?> requiredConnectionDetailsType = DatabaseConnectionDetails.class;
		assertThat(this.source.accepts(requiredConnectionName, requiredContainerType, requiredConnectionDetailsType))
			.isFalse();
	}

	@Test
	void acceptsWhenRequiredConnectionNameDoesNotMatchNameTakenFromContainerReturnsFalse() {
		String requiredConnectionName = "othername";
		Class<?> requiredContainerType = JdbcDatabaseContainer.class;
		Class<?> requiredConnectionDetailsType = DatabaseConnectionDetails.class;
		assertThat(this.source.accepts(requiredConnectionName, requiredContainerType, requiredConnectionDetailsType))
			.isFalse();
	}

	@Test
	void acceptsWhenRequiredConnectionNameIsUnrestrictedReturnsTrue() {
		String requiredConnectionName = null;
		Class<?> requiredContainerType = JdbcDatabaseContainer.class;
		Class<?> requiredConnectionDetailsType = DatabaseConnectionDetails.class;
		assertThat(this.source.accepts(requiredConnectionName, requiredContainerType, requiredConnectionDetailsType))
			.isTrue();
	}

	@Test
	void acceptsWhenRequiredConnectionNameMatchesNameTakenFromAnnotationReturnsTrue() {
		setupSourceAnnotatedWithName("myname");
		String requiredConnectionName = "myname";
		Class<?> requiredContainerType = JdbcDatabaseContainer.class;
		Class<?> requiredConnectionDetailsType = DatabaseConnectionDetails.class;
		assertThat(this.source.accepts(requiredConnectionName, requiredContainerType, requiredConnectionDetailsType))
			.isTrue();
	}

	@Test
	void acceptsWhenRequiredConnectionNameMatchesNameTakenFromContainerReturnsTrue() {
		String requiredConnectionName = "postgres";
		Class<?> requiredContainerType = JdbcDatabaseContainer.class;
		Class<?> requiredConnectionDetailsType = DatabaseConnectionDetails.class;
		assertThat(this.source.accepts(requiredConnectionName, requiredContainerType, requiredConnectionDetailsType))
			.isTrue();
	}

	@Test
	void acceptsWhenRequiredConnectionDetailsTypeNotInAnnotationRestrictionReturnsFalse() {
		setupSourceAnnotatedWithType(ExampleConnectionDetails.class);
		String requiredConnectionName = null;
		Class<?> requiredContainerType = JdbcDatabaseContainer.class;
		Class<?> requiredConnectionDetailsType = DatabaseConnectionDetails.class;
		assertThat(this.source.accepts(requiredConnectionName, requiredContainerType, requiredConnectionDetailsType))
			.isFalse();
	}

	@Test
	void acceptsWhenRequiredConnectionDetailsTypeInAnnotationRestrictionReturnsTrue() {
		setupSourceAnnotatedWithType(DatabaseConnectionDetails.class);
		String requiredConnectionName = null;
		Class<?> requiredContainerType = JdbcDatabaseContainer.class;
		Class<?> requiredConnectionDetailsType = DatabaseConnectionDetails.class;
		assertThat(this.source.accepts(requiredConnectionName, requiredContainerType, requiredConnectionDetailsType))
			.isTrue();
	}

	@Test
	void acceptsWhenRequiredConnectionDetailsTypeIsNotRestrictedReturnsTrue() {
		String requiredConnectionName = null;
		Class<?> requiredContainerType = JdbcDatabaseContainer.class;
		Class<?> requiredConnectionDetailsType = DatabaseConnectionDetails.class;
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
				this.container.getDockerImageName(), this.annotation, () -> this.container, null, null);
	}

	private void setupSourceAnnotatedWithType(Class<?> type) {
		this.annotation = MergedAnnotation.of(ServiceConnection.class,
				Map.of("name", "", "type", new Class<?>[] { type }));
		this.source = new ContainerConnectionSource<>(this.beanNameSuffix, this.origin, PostgreSQLContainer.class,
				this.container.getDockerImageName(), this.annotation, () -> this.container, null, null);
	}

	interface ExampleConnectionDetails extends ConnectionDetails {

	}

}
