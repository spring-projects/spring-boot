/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.bootstrapregistry.app;

import org.jspecify.annotations.Nullable;
import smoketest.bootstrapregistry.external.svn.SubversionClient;
import smoketest.bootstrapregistry.external.svn.SubversionServerCertificate;

public class MySubversionClient extends SubversionClient {

	public MySubversionClient(@Nullable SubversionServerCertificate serverCertificate) {
		super(serverCertificate);
	}

	@Override
	public String load(String location) {
		return "my-" + super.load(location);
	}

}
