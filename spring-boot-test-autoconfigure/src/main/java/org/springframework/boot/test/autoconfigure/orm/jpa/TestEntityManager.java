/*
 * Copyright 2012-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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

import org.springframework.orm.jpa.EntityManagerFactoryUtils;
import org.springframework.util.Assert;

/**
 * Alternative to {@link EntityManager} for use in JPA tests. Provides a subset of
 * {@link EntityManager} methods that are useful for tests as well as helper methods for
 * common testing tasks such as {@link #persistFlushFind(Object) persist/flush/find}.
 *
 * @author Phillip Webb
 * @since 1.4.0
 */
public class TestEntityManager {

	private final EntityManagerFactory entityManagerFactory;

	/**
	 * Create a new {@link TestEntityManager} instance for the given
	 * {@link EntityManagerFactory}.
	 * @param entityManagerFactory the source entity manager factory
	 */
	public TestEntityManager(EntityManagerFactory entityManagerFactory) {
		Assert.notNull(entityManagerFactory, "EntityManagerFactory must not be null");
		this.entityManagerFactory = entityManagerFactory;
	}

	/**
	 * Make an instance managed and persistent then return it's ID. Delegates to
	 * {@link EntityManager#persist(Object)} then {@link #getId(Object)}.
	 * <p>
	 * Helpful when setting up test data in a test: <pre class="code">
	 * Object entityId = this.testEntityManager.persist(new MyEntity("Spring"));
	 * </pre>
	 * @param entity the source entity
	 * @return the ID of the newly persisted entity
	 */
	public Object persistAndGetId(Object entity) {
		persist(entity);
		return getId(entity);
	}

	/**
	 * Make an instance managed and persistent then return it's ID. Delegates to
	 * {@link EntityManager#persist(Object)} then {@link #getId(Object, Class)}.
	 * <p>
	 * Helpful when setting up test data in a test: <pre class="code">
	 * Long entityId = this.testEntityManager.persist(new MyEntity("Spring"), Long.class);
	 * </pre>
	 * @param <T> the ID type
	 * @param entity the source entity
	 * @param idType the ID type
	 * @return the ID of the newly persisted entity
	 */
	public <T> T persistAndGetId(Object entity, Class<T> idType) {
		persist(entity);
		return getId(entity, idType);

	}

	/**
	 * Make an instance managed and persistent. Delegates to
	 * {@link EntityManager#persist(Object)} then returns the original source entity.
	 * <p>
	 * Helpful when setting up test data in a test: <pre class="code">
	 * MyEntity entity = this.testEntityManager.persist(new MyEntity("Spring"));
	 * </pre>
	 * @param <E> the entity type
	 * @param entity the entity to persist
	 * @return the persisted entity
	 */
	public <E> E persist(E entity) {
		Assert.state(getId(entity) == null,
				"Entity " + entity.getClass().getName() + " already has an ID");
		getEntityManager().persist(entity);
		return entity;
	}

	/**
	 * Make an instance managed and persistent, synchronize the persistence context to the
	 * underlying database and finally find the persisted entity by its ID. Delegates to
	 * {@link #persistAndFlush(Object)} then {@link #find(Class, Object)} with the
	 * {@link #getId(Object) entity ID}.
	 * <p>
	 * Helpful when ensuring that entity data is actually written and read from the
	 * underlying database correctly.
	 * @param <E> the entity type
	 * @param entity the entity to persist
	 * @return the entity found using the ID of the persisted entity
	 */
	@SuppressWarnings("unchecked")
	public <E> E persistFlushFind(E entity) {
		EntityManager entityManager = getEntityManager();
		persistAndFlush(entity);
		Object id = getId(entity);
		entityManager.detach(entity);
		return (E) entityManager.find(entity.getClass(), id);
	}

	/**
	 * Make an instance managed and persistent then synchronize the persistence context to
	 * the underlying database. Delegates to {@link EntityManager#persist(Object)} then
	 * {@link #flush()} and finally returns the original source entity.
	 * <p>
	 * Helpful when setting up test data in a test: <pre class="code">
	 * MyEntity entity = this.testEntityManager.persistAndFlush(new MyEntity("Spring"));
	 * </pre>
	 * @param <E> the entity type
	 * @param entity the entity to persist
	 * @return the persisted entity
	 */
	public <E> E persistAndFlush(E entity) {
		persist(entity);
		flush();
		return entity;
	}

	/**
	 * Merge the state of the given entity into the current persistence context. Delegates
	 * to {@link EntityManager#merge(Object)}
	 * @param <E> the entity type
	 * @param entity the entity to merge
	 * @return the merged entity
	 */
	public <E> E merge(E entity) {
		return getEntityManager().merge(entity);
	}

	/**
	 * Remove the entity instance. Delegates to {@link EntityManager#remove(Object)}
	 * @param entity the entity to remove
	 */
	public void remove(Object entity) {
		getEntityManager().remove(entity);
	}

	/**
	 * Find by primary key. Delegates to {@link EntityManager#find(Class, Object)}.
	 * @param <E> the entity type
	 * @param entityClass the entity class
	 * @param primaryKey the entity primary key
	 * @return the found entity or {@code null} if the entity does not exist
	 * @see #getId(Object)
	 */
	public <E> E find(Class<E> entityClass, Object primaryKey) {
		return getEntityManager().find(entityClass, primaryKey);
	}

	/**
	 * Synchronize the persistence context to the underlying database. Delegates to
	 * {@link EntityManager#flush()}.
	 */
	public void flush() {
		getEntityManager().flush();
	}

	/**
	 * Refresh the state of the instance from the database, overwriting changes made to
	 * the entity, if any. Delegates to {@link EntityManager#refresh(Object)}.
	 * @param <E> the entity type
	 * @param entity the entity to refresh
	 * @return the refreshed entity
	 */
	public <E> E refresh(E entity) {
		getEntityManager().refresh(entity);
		return entity;
	}

	/**
	 * Clear the persistence context, causing all managed entities to become detached.
	 * Delegates to {@link EntityManager#clear()}
	 */
	public void clear() {
		getEntityManager().clear();
	}

	/**
	 * Remove the given entity from the persistence context, causing a managed entity to
	 * become detached. Delegates to {@link EntityManager#detach(Object)}.
	 * @param entity the entity to detach.
	 */
	public void detach(Object entity) {
		getEntityManager().detach(entity);
	}

	/**
	 * Return the ID of the given entity. Delegates to
	 * {@link PersistenceUnitUtil#getIdentifier(Object)}.
	 * @param entity the source entity
	 * @return the ID of the entity or {@code null}
	 * @see #getId(Object, Class)
	 */
	public Object getId(Object entity) {
		return this.entityManagerFactory.getPersistenceUnitUtil().getIdentifier(entity);
	}

	/**
	 * Return the ID of the given entity cast to a specific type. Delegates to
	 * {@link PersistenceUnitUtil#getIdentifier(Object)}.
	 * @param <T> the ID type
	 * @param entity the source entity
	 * @param idType the expected ID type
	 * @return the ID of the entity or {@code null}
	 * @see #getId(Object)
	 */
	@SuppressWarnings("unchecked")
	public <T> T getId(Object entity, Class<T> idType) {
		Object id = getId(entity);
		Assert.isInstanceOf(idType, id, "ID mismatch");
		return (T) id;
	}

	/**
	 * Return the underlying {@link EntityManager} that's actually used to perform all
	 * operations.
	 * @return the entity manager
	 */
	public final EntityManager getEntityManager() {
		EntityManager manager = EntityManagerFactoryUtils
				.getTransactionalEntityManager(this.entityManagerFactory);
		Assert.state(manager != null, "No transactional EntityManager found");
		return manager;
	}

}
