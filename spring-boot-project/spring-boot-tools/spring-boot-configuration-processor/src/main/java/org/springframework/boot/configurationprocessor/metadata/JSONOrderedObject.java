/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.configurationprocessor.metadata;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.boot.configurationprocessor.json.JSONException;
import org.springframework.boot.configurationprocessor.json.JSONObject;

/**
 * Extension to {@link JSONObject} that remembers the order of inserts.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 */
@SuppressWarnings("rawtypes")
class JSONOrderedObject extends JSONObject {

	private Set<String> keys = new LinkedHashSet<>();

	@Override
	public JSONObject put(String key, Object value) throws JSONException {
		this.keys.add(key);
		return super.put(key, value);
	}

	@Override
	public Iterator keys() {
		return this.keys.iterator();
	}

}
