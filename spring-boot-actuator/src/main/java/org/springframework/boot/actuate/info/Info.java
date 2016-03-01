/*
 * Copyright 2012-2015 the original author or authors.
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

package org.springframework.boot.actuate.info;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * Carries information from a specific info provider.
 *
 * @author Meang Akira Tanaka
 * @since 1.3.0
 * @see org.springframework.boot.actuate.endpoint.InfoEndpoint
 */
@JsonInclude(Include.NON_EMPTY)
public final class Info {

	private final Map<String, Object> details = new HashMap<String, Object>();

	public Info() {
	}

	public Info(Map<String, Object> details) {
		this.details.putAll(details);
	}

	/**
	 * Return the content.
	 * @return the details of the info or an empty map.
	 */
	@JsonAnyGetter
	public Map<String, Object> getDetails() {
		return this.details;
	}

	public void put(String infoId, Object value) {
		this.details.put(infoId, value);
	}

	@SuppressWarnings("unchecked")
	public <T> T get(String infoId) {
		return (T) this.details.get(infoId);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (obj != null && obj instanceof Info) {
			Info other = (Info) obj;
			return this.details.equals(other.details);
		}
		return false;
	}

	@Override
	public int hashCode() {
		return this.details.hashCode();
	}

	@Override
	public String toString() {
		return getDetails().toString();
	}
}
