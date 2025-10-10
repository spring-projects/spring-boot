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

package org.springframework.boot.data.ldap.test.autoconfigure;

import javax.naming.Name;

import org.jspecify.annotations.Nullable;

import org.springframework.ldap.odm.annotations.Entry;
import org.springframework.ldap.odm.annotations.Id;

/**
 * Example entry used with {@link DataLdapTest @DataLdapTest} tests.
 *
 * @author Eddú Meléndez
 */
@Entry(objectClasses = { "person", "top" })
public class ExampleEntry {

	@Id
	private @Nullable Name dn;

	public @Nullable Name getDn() {
		return this.dn;
	}

	public void setDn(@Nullable Name dn) {
		this.dn = dn;
	}

}
