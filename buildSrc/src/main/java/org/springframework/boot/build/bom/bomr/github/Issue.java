/*
 * Copyright 2012-2021 the original author or authors.
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

package org.springframework.boot.build.bom.bomr.github;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.web.client.RestTemplate;

/**
 * Minimal representation of a GitHub issue.
 *
 * @author Andy Wilkinson
 */
public class Issue {

	private final RestTemplate rest;

	private final int number;

	private final String title;

	Issue(RestTemplate rest, int number, String title) {
		this.rest = rest;
		this.number = number;
		this.title = title;
	}

	public int getNumber() {
		return this.number;
	}

	public String getTitle() {
		return this.title;
	}

	/**
	 * Labels the issue with the given {@code labels}. Any existing labels are removed.
	 * @param labels the labels to apply to the issue
	 */
	public void label(List<String> labels) {
		Map<String, List<String>> body = Collections.singletonMap("labels", labels);
		this.rest.put("issues/" + this.number + "/labels", body);
	}

}
