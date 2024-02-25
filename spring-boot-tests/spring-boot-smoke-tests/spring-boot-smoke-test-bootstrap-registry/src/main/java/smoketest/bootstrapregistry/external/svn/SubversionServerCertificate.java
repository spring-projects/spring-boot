/*
 * Copyright 2012-2020 the original author or authors.
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

package smoketest.bootstrapregistry.external.svn;

import org.springframework.util.StringUtils;

/**
 * A certificate that can be used to provide a secure connection to the subversion server.
 *
 * @author Phillip Webb
 */
public class SubversionServerCertificate {

	private final String data;

	/**
     * Initializes a new instance of the SubversionServerCertificate class with the specified data.
     * 
     * @param data The certificate data to be stored.
     */
    SubversionServerCertificate(String data) {
		this.data = data;
	}

	/**
     * Returns a string representation of the object.
     * 
     * @return the string representation of the object
     */
    @Override
	public String toString() {
		return this.data;
	}

	/**
     * Creates a SubversionServerCertificate object from the given data.
     * 
     * @param data the data to create the SubversionServerCertificate from
     * @return a SubversionServerCertificate object if the data is not empty or null, otherwise null
     */
    public static SubversionServerCertificate of(String data) {
		return StringUtils.hasText(data) ? new SubversionServerCertificate(data) : null;
	}

}
