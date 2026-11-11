/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.bootstrapregistry.external.svn;

import org.jspecify.annotations.Nullable;

import org.springframework.util.StringUtils;

/**
 * A certificate that can be used to provide a secure connection to the subversion server.
 *
 * @author Phillip Webb
 */
public class SubversionServerCertificate {

	private final String data;

	SubversionServerCertificate(String data) {
		this.data = data;
	}

	@Override
	public String toString() {
		return this.data;
	}

	public static @Nullable SubversionServerCertificate of(@Nullable String data) {
		return StringUtils.hasText(data) ? new SubversionServerCertificate(data) : null;
	}

}
