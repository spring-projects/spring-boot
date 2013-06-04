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
package org.springframework.bootstrap.config;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author Dave Syer
 * 
 */
public class JacksonParser implements JsonParser {

	@Override
	public Map<String, Object> parseMap(String json) {
		try {
			@SuppressWarnings("unchecked")
			Map<String, Object> map = new ObjectMapper().readValue(json, Map.class);
			return map;
		} catch (Exception e) {
			throw new IllegalArgumentException("Cannot parse JSON", e);
		}
	}

	@Override
	public List<Object> parseList(String json) {
		try {
			@SuppressWarnings("unchecked")
			List<Object> list = new ObjectMapper().readValue(json, List.class);
			return list;
		} catch (Exception e) {
			throw new IllegalArgumentException("Cannot parse JSON", e);
		}
	}

}
