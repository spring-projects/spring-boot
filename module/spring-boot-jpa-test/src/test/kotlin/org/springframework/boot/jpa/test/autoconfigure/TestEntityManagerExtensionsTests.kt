/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.jpa.test.autoconfigure

import jakarta.persistence.EntityManager
import jakarta.persistence.EntityManagerFactory
import jakarta.persistence.PersistenceUnitUtil
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.then
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.orm.jpa.EntityManagerHolder
import org.springframework.transaction.support.TransactionSynchronizationManager

/**
 * Tests for [TestEntityManager] Kotlin extensions.
 *
 * @author Beom Su
 * @since 4.1.0
 */
@ExtendWith(MockitoExtension::class)
class TestEntityManagerExtensionsTests {

	@Mock
	private lateinit var entityManagerFactory: EntityManagerFactory

	@Mock
	private lateinit var entityManager: EntityManager

	@Mock
	private lateinit var persistenceUnitUtil: PersistenceUnitUtil

	private lateinit var testEntityManager: TestEntityManager

	@BeforeEach
	fun setup() {
		this.testEntityManager = TestEntityManager(this.entityManagerFactory)
	}

	@Test
	fun `find with reified type parameter`() {
		bindEntityManager()
		val entity = TestEntity()
		given(this.entityManager.find(TestEntity::class.java, 123)).willReturn(entity)
		val result = this.testEntityManager.find<TestEntity>(123)
		assertThat(result).isSameAs(entity)
	}

	@Test
	fun `persistAndGetId with reified type parameter`() {
		bindEntityManager()
		val entity = TestEntity()
		given(this.entityManagerFactory.persistenceUnitUtil).willReturn(this.persistenceUnitUtil)
		given(this.persistenceUnitUtil.getIdentifier(entity)).willReturn(123)
		val result = this.testEntityManager.persistAndGetId<Int>(entity)
		then(this.entityManager).should().persist(entity)
		assertThat(result).isEqualTo(123)
	}

	@Test
	fun `getId with reified type parameter`() {
		val entity = TestEntity()
		given(this.entityManagerFactory.persistenceUnitUtil).willReturn(this.persistenceUnitUtil)
		given(this.persistenceUnitUtil.getIdentifier(entity)).willReturn(123)
		val result = this.testEntityManager.getId<Int>(entity)
		assertThat(result).isEqualTo(123)
	}

	private fun bindEntityManager() {
		val holder = EntityManagerHolder(this.entityManager)
		TransactionSynchronizationManager.bindResource(this.entityManagerFactory, holder)
	}

	class TestEntity

}
