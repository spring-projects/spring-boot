/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.jpa.test.autoconfigure

/**
 * Extension for [TestEntityManager.find] providing a `find<MyEntity>(...)`
 * variant leveraging Kotlin reified type parameters.
 *
 * @param primaryKey the primary key of the entity
 * @author Beom Su
 * @since 4.1.0
 */
inline fun <reified E : Any> TestEntityManager.find(primaryKey: Any): E? =
		find(E::class.java, primaryKey)

/**
 * Extension for [TestEntityManager.persistAndGetId] providing a
 * `persistAndGetId<Long>(...)` variant leveraging Kotlin reified type parameters.
 *
 * @param entity the source entity
 * @author Beom Su
 * @since 4.1.0
 */
inline fun <reified T : Any> TestEntityManager.persistAndGetId(entity: Any): T? =
		persistAndGetId(entity, T::class.java)

/**
 * Extension for [TestEntityManager.getId] providing a `getId<MyEntity>(...)`
 * variant leveraging Kotlin reified type parameters.
 *
 * @param entity the source entity
 * @author Beom Su
 * @since 4.1.0
 */
inline fun <reified T : Any> TestEntityManager.getId(entity: Any): T? =
		getId(entity, T::class.java)
