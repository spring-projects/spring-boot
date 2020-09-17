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

import org.springframework.boot.context.config.ConfigDataLocation;

/**
 * A subversion {@link ConfigDataLocation}.
 *
 * @author Phillip Webb
 */
class SubversionConfigDataLocation extends ConfigDataLocation {

	private final String location;

	private final SubversionServerCertificate serverCertificate;

	SubversionConfigDataLocation(String location, String serverCertificate) {
		this.location = location;
		this.serverCertificate = SubversionServerCertificate.of(serverCertificate);
	}

	String getLocation() {
		return this.location;
	}

	SubversionServerCertificate getServerCertificate() {
		return this.serverCertificate;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		SubversionConfigDataLocation other = (SubversionConfigDataLocation) obj;
		return this.location.equals(other.location);
	}

	@Override
	public int hashCode() {
		return this.location.hashCode();
	}

	@Override
	public String toString() {
		return this.location;
	}

}
