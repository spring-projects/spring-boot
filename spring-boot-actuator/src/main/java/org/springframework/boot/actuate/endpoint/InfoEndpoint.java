/*
 * Copyright 2012-2013 the original author or authors.
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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.Assert;

/**
 * {@link Endpoint} to expose arbitrary application information.
 * 
 * @author Dave Syer
 */
@ConfigurationProperties(name = "endpoints.info", ignoreUnknownFields = false)
public class InfoEndpoint extends AbstractEndpoint<Map<String, Object>> {

	private Map<String, ? extends Object> info;

	/**
	 * Create a new {@link InfoEndpoint} instance.
	 * 
	 * @param info the info to expose
	 */
	public InfoEndpoint(Map<String, ? extends Object> info) {
		super("/info", true);
		Assert.notNull(info, "Info must not be null");
		this.info = info;
	}

	@Override
	public Map<String, Object> invoke() {
		Map<String, Object> info = new LinkedHashMap<String, Object>(this.info);
		info.putAll(getAdditionalInfo());
		return info;
	}

	protected Map<String, Object> getAdditionalInfo() {
		return Collections.emptyMap();
	}

}
