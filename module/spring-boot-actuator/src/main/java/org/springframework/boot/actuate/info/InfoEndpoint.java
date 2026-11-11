/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.actuate.info;

import java.util.List;
import java.util.Map;

import org.springframework.boot.actuate.endpoint.OperationResponseBody;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.util.Assert;

/**
 * {@link Endpoint @Endpoint} to expose arbitrary application information.
 *
 * @author Dave Syer
 * @author Meang Akira Tanaka
 * @author Stephane Nicoll
 * @since 2.0.0
 */
@Endpoint(id = "info")
public class InfoEndpoint {

	private final List<InfoContributor> infoContributors;

	/**
	 * Create a new {@link InfoEndpoint} instance.
	 * @param infoContributors the info contributors to use
	 */
	public InfoEndpoint(List<InfoContributor> infoContributors) {
		Assert.notNull(infoContributors, "'infoContributors' must not be null");
		this.infoContributors = infoContributors;
	}

	@ReadOperation
	public Map<String, Object> info() {
		Info.Builder builder = new Info.Builder();
		for (InfoContributor contributor : this.infoContributors) {
			contributor.contribute(builder);
		}
		return OperationResponseBody.of(builder.build().getDetails());
	}

}
