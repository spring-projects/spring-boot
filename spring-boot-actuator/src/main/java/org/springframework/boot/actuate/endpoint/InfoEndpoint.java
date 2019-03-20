/*
 * Copyright 2012-2016 the original author or authors.
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

package org.springframework.boot.actuate.endpoint;

import java.util.List;
import java.util.Map;

import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.Assert;

/**
 * {@link Endpoint} to expose arbitrary application information.
 *
 * @author Dave Syer
 * @author Meang Akira Tanaka
 * @author Stephane Nicoll
 */
@ConfigurationProperties(prefix = "endpoints.info")
public class InfoEndpoint extends AbstractEndpoint<Map<String, Object>> {

	private final List<InfoContributor> infoContributors;

	/**
	 * Create a new {@link InfoEndpoint} instance.
	 * @param infoContributors the info contributors to use
	 */
	public InfoEndpoint(List<InfoContributor> infoContributors) {
		super("info", false);
		Assert.notNull(infoContributors, "Info contributors must not be null");
		this.infoContributors = infoContributors;
	}

	@Override
	public Map<String, Object> invoke() {
		Info.Builder builder = new Info.Builder();
		for (InfoContributor contributor : this.infoContributors) {
			contributor.contribute(builder);
		}
		Info build = builder.build();
		return build.getDetails();
	}

}
