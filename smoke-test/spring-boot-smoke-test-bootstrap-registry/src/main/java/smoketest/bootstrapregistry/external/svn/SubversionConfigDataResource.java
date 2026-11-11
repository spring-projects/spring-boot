/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.bootstrapregistry.external.svn;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.context.config.ConfigDataResource;

/**
 * A subversion {@link ConfigDataResource}.
 *
 * @author Phillip Webb
 */
class SubversionConfigDataResource extends ConfigDataResource {

	private final String location;

	private final @Nullable SubversionServerCertificate serverCertificate;

	SubversionConfigDataResource(String location, @Nullable String serverCertificate) {
		this.location = location;
		this.serverCertificate = SubversionServerCertificate.of(serverCertificate);
	}

	String getLocation() {
		return this.location;
	}

	@Nullable SubversionServerCertificate getServerCertificate() {
		return this.serverCertificate;
	}

	@Override
	public boolean equals(@Nullable Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		SubversionConfigDataResource other = (SubversionConfigDataResource) obj;
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
