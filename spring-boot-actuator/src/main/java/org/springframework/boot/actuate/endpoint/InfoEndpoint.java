/*
 * Copyright 2012-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.actuate.endpoint;

import java.util.Map;

import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.actuate.info.InfoProvider;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.Assert;

/**
 * {@link Endpoint} to expose arbitrary application information.
 *
 * The information, which the {@link InfoEndpoint} can provide can be customized to
 * display any information, however initially the info endpoint will provide git version
 * information (if available) and environment information,whose entries are prefixed with
 * info.
 *
 * In order to add additional information to the endpoint, one has to implement a class,
 * which implements the {@link org.springframework.boot.actuate.info.InfoProvider}
 * interface and register it in the application context. The InfoEndpoint will
 * automatically pick it up, when it is being instantiated.
 *
 * The standard InfoProvider for GIT is registered as the scmInfoProvider, and the
 * registration can be changed in case standard provider does not meet ones requirements.
 *
 * @see org.springframework.boot.actuate.info.ScmGitPropertiesInfoProvider
 * @see org.springframework.boot.actuate.info.EnvironmentInfoProvider
 *
 * @author Dave Syer
 * @author Meang Akira Tanaka
 */
@ConfigurationProperties(prefix = "endpoints.info", ignoreUnknownFields = false)
public class InfoEndpoint extends AbstractEndpoint<Info> {

	private final Map<String, InfoProvider> infoProviders;

	/**
	 * Create a new {@link InfoEndpoint} instance.
	 *
	 * @param infoProviders the infoProviders to be used
	 */
	public InfoEndpoint(Map<String, InfoProvider> infoProviders) {
		super("info", false);
		Assert.notNull(infoProviders, "Info providers must not be null");
		this.infoProviders = infoProviders;
	}

	@Override
	public Info invoke() {
		Info result = new Info();
		for (InfoProvider provider : this.infoProviders.values()) {
			Info info = provider.provide();
			if (info != null) {
				result.put(provider.name(), info);
			}
		}
		return result;
	}
}
