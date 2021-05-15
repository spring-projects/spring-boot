/*
 * Copyright 2012-2020 the original author or authors.
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

/**
 * Standard implementation of {@link GitHubRepository}.
 *
 * @author Andy Wilkinson
 */
final class StandardGitHubRepository implements GitHubRepository {

	private final RestTemplate rest;

	StandardGitHubRepository(RestTemplate restTemplate) {
		this.rest = restTemplate;
	}

	@Override
	@SuppressWarnings("rawtypes")
	public int openIssue(String title, List<String> labels, Milestone milestone) {
		Map<String, Object> body = new HashMap<>();
		body.put("title", title);
		if (milestone != null) {
			body.put("milestone", milestone.getNumber());
		}
		if (!labels.isEmpty()) {
			body.put("labels", labels);
		}
		ResponseEntity<Map> response = this.rest.postForEntity("issues", body, Map.class);
		return (Integer) response.getBody().get("number");
	}

	@Override
	public List<String> getLabels() {
		return get("labels?per_page=100", (label) -> (String) label.get("name"));
	}

	@Override
	public List<Milestone> getMilestones() {
		return get("milestones?per_page=100",
				(milestone) -> new Milestone((String) milestone.get("title"), (Integer) milestone.get("number")));
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private <T> List<T> get(String name, Function<Map<String, Object>, T> mapper) {
		ResponseEntity<List> response = this.rest.getForEntity(name, List.class);
		List<Map<String, Object>> body = response.getBody();
		return body.stream().map(mapper).collect(Collectors.toList());
	}

}
