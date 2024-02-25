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

import org.springframework.boot.context.config.ConfigDataResource;

/**
 * A subversion {@link ConfigDataResource}.
 *
 * @author Phillip Webb
 */
class SubversionConfigDataResource extends ConfigDataResource {

	private final String location;

	private final SubversionServerCertificate serverCertificate;

	/**
     * Constructs a new SubversionConfigDataResource with the specified location and server certificate.
     * 
     * @param location the location of the Subversion configuration data resource
     * @param serverCertificate the server certificate for the Subversion server
     */
    SubversionConfigDataResource(String location, String serverCertificate) {
		this.location = location;
		this.serverCertificate = SubversionServerCertificate.of(serverCertificate);
	}

	/**
     * Returns the location of the Subversion configuration data resource.
     *
     * @return the location of the Subversion configuration data resource
     */
    String getLocation() {
		return this.location;
	}

	/**
     * Returns the server certificate associated with this SubversionConfigDataResource.
     *
     * @return the server certificate
     */
    SubversionServerCertificate getServerCertificate() {
		return this.serverCertificate;
	}

	/**
     * Compares this SubversionConfigDataResource object to the specified object for equality.
     * 
     * @param obj the object to compare to
     * @return true if the specified object is equal to this SubversionConfigDataResource object, false otherwise
     */
    @Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		SubversionConfigDataResource other = (SubversionConfigDataResource) obj;
		return this.location.equals(other.location);
	}

	/**
     * Returns the hash code value for this SubversionConfigDataResource object.
     * The hash code is generated based on the location of the resource.
     *
     * @return the hash code value for this SubversionConfigDataResource object
     */
    @Override
	public int hashCode() {
		return this.location.hashCode();
	}

	/**
     * Returns the string representation of the location.
     *
     * @return the location as a string
     */
    @Override
	public String toString() {
		return this.location;
	}

}
