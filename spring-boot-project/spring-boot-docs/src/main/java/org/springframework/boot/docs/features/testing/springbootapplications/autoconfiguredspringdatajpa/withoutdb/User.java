/*
 * Copyright 2012-2021 the original author or authors.
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

package org.springframework.boot.docs.features.testing.springbootapplications.autoconfiguredspringdatajpa.withoutdb;

/**
 * User class.
 */
class User {

	/**
     * Constructs a new User object with the specified username and employee number.
     *
     * @param username the username of the user
     * @param employeeNumber the employee number of the user
     */
    User(String username, String employeeNumber) {
	}

	/**
     * Returns the employee number of the user.
     *
     * @return the employee number of the user, or null if not available
     */
    String getEmployeeNumber() {
		return null;
	}

	/**
     * Returns the username of the user.
     *
     * @return the username of the user
     */
    String getUsername() {
		return null;
	}

}
