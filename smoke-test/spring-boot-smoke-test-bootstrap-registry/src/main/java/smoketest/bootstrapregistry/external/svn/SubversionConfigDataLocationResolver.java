/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.bootstrapregistry.external.svn;

import java.util.Collections;
import java.util.List;

import org.springframework.boot.context.config.ConfigDataLocation;
import org.springframework.boot.context.config.ConfigDataLocationNotFoundException;
import org.springframework.boot.context.config.ConfigDataLocationResolver;
import org.springframework.boot.context.config.ConfigDataLocationResolverContext;
import org.springframework.boot.context.config.ConfigDataResourceNotFoundException;

/**
 * {@link ConfigDataLocationResolver} for subversion.
 *
 * @author Phillip Webb
 */
class SubversionConfigDataLocationResolver implements ConfigDataLocationResolver<SubversionConfigDataResource> {

	private static final String PREFIX = "svn:";

	@Override
	public boolean isResolvable(ConfigDataLocationResolverContext context, ConfigDataLocation location) {
		return location.hasPrefix(PREFIX);
	}

	@Override
	public List<SubversionConfigDataResource> resolve(ConfigDataLocationResolverContext context,
			ConfigDataLocation location)
			throws ConfigDataLocationNotFoundException, ConfigDataResourceNotFoundException {
		String serverCertificate = context.getBinder().bind("spring.svn.server.certificate", String.class).orElse(null);
		return Collections
			.singletonList(new SubversionConfigDataResource(location.getNonPrefixedValue(PREFIX), serverCertificate));
	}

}
