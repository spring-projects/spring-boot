/*
 * Copyright 2012-2022 the original author or authors.
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

/**
 * A client that can connect to a subversion server.
 *
 * @author Phillip Webb
 */
public class SubversionClient {

	private final SubversionServerCertificate serverCertificate;

	/**
     * Constructs a new SubversionClient object with the specified server certificate.
     * 
     * @param serverCertificate the server certificate to be used for authentication
     */
    public SubversionClient(SubversionServerCertificate serverCertificate) {
		this.serverCertificate = serverCertificate;
	}

	/**
     * Loads data from the specified location in the Subversion repository.
     * 
     * @param location the location in the Subversion repository to load data from
     * @return the loaded data along with the server certificate
     */
    public String load(String location) {
		return "data from svn / " + location + "[" + this.serverCertificate + "]";
	}

}
