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

package org.springframework.boot.test.autoconfigure.orm.jpa;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnitUtil;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import org.springframework.orm.jpa.EntityManagerHolder;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link TestEntityManager}.
 *
 * @author Phillip Webb
 */
class TestEntityManagerTests {

	@Mock
	private EntityManagerFactory entityManagerFactory;

	@Mock
	private EntityManager entityManager;

	@Mock
	private PersistenceUnitUtil persistenceUnitUtil;

	private TestEntityManager testEntityManager;

	@BeforeEach
	public void setup() {
		MockitoAnnotations.initMocks(this);
		this.testEntityManager = new TestEntityManager(this.entityManagerFactory);
		given(this.entityManagerFactory.getPersistenceUnitUtil()).willReturn(this.persistenceUnitUtil);
	}

	@Test
	void createWhenEntityManagerIsNullShouldThrowException() {
		assertThatIllegalArgumentException().isThrownBy(() -> new TestEntityManager(null))
				.withMessageContaining("EntityManagerFactory must not be null");
	}

	@Test
	void persistAndGetIdShouldPersistAndGetId() {
		bindEntityManager();
		TestEntity entity = new TestEntity();
		given(this.persistenceUnitUtil.getIdentifier(entity)).willReturn(123);
		Object result = this.testEntityManager.persistAndGetId(entity);
		verify(this.entityManager).persist(entity);
		assertThat(result).isEqualTo(123);
	}

	@Test
	void persistAndGetIdForTypeShouldPersistAndGetId() {
		bindEntityManager();
		TestEntity entity = new TestEntity();
		given(this.persistenceUnitUtil.getIdentifier(entity)).willReturn(123);
		Integer result = this.testEntityManager.persistAndGetId(entity, Integer.class);
		verify(this.entityManager).persist(entity);
		assertThat(result).isEqualTo(123);
	}

	@Test
	void persistShouldPersist() {
		bindEntityManager();
		TestEntity entity = new TestEntity();
		TestEntity result = this.testEntityManager.persist(entity);
		verify(this.entityManager).persist(entity);
		assertThat(result).isSameAs(entity);
	}

	@Test
	void persistAndFlushShouldPersistAndFlush() {
		bindEntityManager();
		TestEntity entity = new TestEntity();
		TestEntity result = this.testEntityManager.persistAndFlush(entity);
		verify(this.entityManager).persist(entity);
		verify(this.entityManager).flush();
		assertThat(result).isSameAs(entity);
	}

	@Test
	void persistFlushFindShouldPersistAndFlushAndFind() {
		bindEntityManager();
		TestEntity entity = new TestEntity();
		TestEntity found = new TestEntity();
		given(this.persistenceUnitUtil.getIdentifier(entity)).willReturn(123);
		given(this.entityManager.find(TestEntity.class, 123)).willReturn(found);
		TestEntity result = this.testEntityManager.persistFlushFind(entity);
		verify(this.entityManager).persist(entity);
		verify(this.entityManager).flush();
		assertThat(result).isSameAs(found);
	}

	@Test
	void mergeShouldMerge() {
		bindEntityManager();
		TestEntity entity = new TestEntity();
		given(this.entityManager.merge(entity)).willReturn(entity);
		TestEntity result = this.testEntityManager.merge(entity);
		verify(this.entityManager).merge(entity);
		assertThat(result).isSameAs(entity);
	}

	@Test
	void removeShouldRemove() {
		bindEntityManager();
		TestEntity entity = new TestEntity();
		this.testEntityManager.remove(entity);
		verify(this.entityManager).remove(entity);
	}

	@Test
	void findShouldFind() {
		bindEntityManager();
		TestEntity entity = new TestEntity();
		given(this.entityManager.find(TestEntity.class, 123)).willReturn(entity);
		TestEntity result = this.testEntityManager.find(TestEntity.class, 123);
		assertThat(result).isSameAs(entity);
	}

	@Test
	void flushShouldFlush() {
		bindEntityManager();
		this.testEntityManager.flush();
		verify(this.entityManager).flush();
	}

	@Test
	void refreshShouldRefresh() {
		bindEntityManager();
		TestEntity entity = new TestEntity();
		this.testEntityManager.refresh(entity);
		verify(this.entityManager).refresh(entity);
	}

	@Test
	void clearShouldClear() {
		bindEntityManager();
		this.testEntityManager.clear();
		verify(this.entityManager).clear();
	}

	@Test
	void detachShouldDetach() {
		bindEntityManager();
		TestEntity entity = new TestEntity();
		this.testEntityManager.detach(entity);
		verify(this.entityManager).detach(entity);
	}

	@Test
	void getIdForTypeShouldGetId() {
		TestEntity entity = new TestEntity();
		given(this.persistenceUnitUtil.getIdentifier(entity)).willReturn(123);
		Integer result = this.testEntityManager.getId(entity, Integer.class);
		assertThat(result).isEqualTo(123);
	}

	@Test
	void getIdForTypeWhenTypeIsWrongShouldThrowException() {
		TestEntity entity = new TestEntity();
		given(this.persistenceUnitUtil.getIdentifier(entity)).willReturn(123);
		assertThatIllegalArgumentException().isThrownBy(() -> this.testEntityManager.getId(entity, Long.class))
				.withMessageContaining("ID mismatch: Object of class [java.lang.Integer] "
						+ "must be an instance of class java.lang.Long");
	}

	@Test
	void getIdShouldGetId() {
		TestEntity entity = new TestEntity();
		given(this.persistenceUnitUtil.getIdentifier(entity)).willReturn(123);
		Object result = this.testEntityManager.getId(entity);
		assertThat(result).isEqualTo(123);
	}

	@Test
	void getEntityManagerShouldGetEntityManager() {
		bindEntityManager();
		assertThat(this.testEntityManager.getEntityManager()).isEqualTo(this.entityManager);
	}

	@Test
	void getEntityManagerWhenNotSetShouldThrowException() {
		assertThatIllegalStateException().isThrownBy(this.testEntityManager::getEntityManager)
				.withMessageContaining("No transactional EntityManager found");
	}

	private void bindEntityManager() {
		EntityManagerHolder holder = new EntityManagerHolder(this.entityManager);
		TransactionSynchronizationManager.bindResource(this.entityManagerFactory, holder);
	}

	static class TestEntity {

	}

}
