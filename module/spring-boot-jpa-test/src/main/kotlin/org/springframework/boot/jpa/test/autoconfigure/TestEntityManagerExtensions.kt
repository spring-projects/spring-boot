/*
 * Copyright 2012-present the original author or authors.
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
