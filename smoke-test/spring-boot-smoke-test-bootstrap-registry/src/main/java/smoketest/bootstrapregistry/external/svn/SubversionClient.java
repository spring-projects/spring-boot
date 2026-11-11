/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.bootstrapregistry.external.svn;

import org.jspecify.annotations.Nullable;

/**
 * A client that can connect to a subversion server.
 *
 * @author Phillip Webb
 */
public class SubversionClient {

	private final @Nullable SubversionServerCertificate serverCertificate;

	public SubversionClient(@Nullable SubversionServerCertificate serverCertificate) {
		this.serverCertificate = serverCertificate;
	}

	public String load(String location) {
		return "data from svn / " + location + "[" + this.serverCertificate + "]";
	}

}
