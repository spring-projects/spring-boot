/*
 * Copyright 2012-2024 the original author or authors.
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

package org.springframework.boot.autoconfigure.ldap;

import org.springframework.boot.autoconfigure.service.connection.ConnectionDetails;

/**
 * Details required to establish a connection to a Ldap service.
 *
 * @author Philipp Kessler
 * @since 3.3.0
 */
public interface LdapConnectionDetails extends ConnectionDetails {

	/**
	 * LDAP URLs of the server.
	 * @return list of the LDAP urls to use
	 */
	String[] getUrls();

	/**
	 * Base suffix from which all operations should originate.
	 * @return base suffix from which all operations should originate or null.
	 */
	default String getBase() {
		return null;
	}

	/**
	 * Login username of the server.
	 * @return login username of the server or null.
	 */
	default String getUsername() {
		return null;
	}

	/**
	 * Login password of the server.
	 * @return login password of the server or null.
	 */
	default String getPassword() {
		return null;
	}

}
