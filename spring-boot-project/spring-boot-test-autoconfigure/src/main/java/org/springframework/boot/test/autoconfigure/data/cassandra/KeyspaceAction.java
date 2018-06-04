/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.test.autoconfigure.data.cassandra;


/**
 * Enum identifying any keyspace actions to take at startup.
 *
 * @author Dmytro Nosan
 */
public enum KeyspaceAction {

	/**
	 * Take no keyspace actions.
	 */
	NONE,

	/**
	 * Create keyspace. Fail if a keyspace already exists.
	 */
	CREATE,

	/**
	 * Create keyspace. Avoid keyspace creation if the keyspace already exists.
	 */
	CREATE_IF_NOT_EXISTS,

	/**
	 * Create keyspace, dropping the keyspace first if it exists.
	 */
	RECREATE


}
