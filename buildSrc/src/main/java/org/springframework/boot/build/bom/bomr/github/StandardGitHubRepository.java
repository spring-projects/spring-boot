/*
 * Copyright 2012-2023 the original author or authors.
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

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException.Forbidden;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * Standard implementation of {@link GitHubRepository}.
 *
 * @author Andy Wilkinson
 */
final class StandardGitHubRepository implements GitHubRepository {

	private final RestTemplate rest;

	/**
     * Constructs a new StandardGitHubRepository with the specified RestTemplate.
     * 
     * @param restTemplate the RestTemplate to be used for making HTTP requests to the GitHub API
     */
    StandardGitHubRepository(RestTemplate restTemplate) {
		this.rest = restTemplate;
	}

	/**
     * Opens a new issue in the GitHub repository.
     * 
     * @param title    the title of the issue
     * @param body     the body of the issue
     * @param labels   the list of labels to be assigned to the issue
     * @param milestone the milestone to be assigned to the issue
     * @return the number of the newly created issue
     * @throws RestClientException if an error occurs while making the REST API call
     */
    @Override
	@SuppressWarnings("rawtypes")
	public int openIssue(String title, String body, List<String> labels, Milestone milestone) {
		Map<String, Object> requestBody = new HashMap<>();
		requestBody.put("title", title);
		if (milestone != null) {
			requestBody.put("milestone", milestone.getNumber());
		}
		if (!labels.isEmpty()) {
			requestBody.put("labels", labels);
		}
		requestBody.put("body", body);
		try {
			ResponseEntity<Map> response = this.rest.postForEntity("issues", requestBody, Map.class);
			// See gh-30304
			sleep(Duration.ofSeconds(3));
			return (Integer) response.getBody().get("number");
		}
		catch (RestClientException ex) {
			if (ex instanceof Forbidden forbidden) {
				System.out.println("Received 403 response with headers " + forbidden.getResponseHeaders());
			}
			throw ex;
		}
	}

	/**
     * Returns a set of labels associated with this repository.
     *
     * @return a set of labels
     */
    @Override
	public Set<String> getLabels() {
		return new HashSet<>(get("labels?per_page=100", (label) -> (String) label.get("name")));
	}

	/**
     * Retrieves a list of milestones for the repository.
     * 
     * @return a list of milestones
     */
    @Override
	public List<Milestone> getMilestones() {
		return get("milestones?per_page=100", (milestone) -> new Milestone((String) milestone.get("title"),
				(Integer) milestone.get("number"),
				(milestone.get("due_on") != null) ? OffsetDateTime.parse((String) milestone.get("due_on")) : null));
	}

	/**
     * Finds issues in the repository based on the given labels and milestone.
     * 
     * @param labels    the list of labels to filter the issues by
     * @param milestone the milestone to filter the issues by
     * @return a list of issues matching the given labels and milestone
     */
    @Override
	public List<Issue> findIssues(List<String> labels, Milestone milestone) {
		return get(
				"issues?per_page=100&state=all&labels=" + String.join(",", labels) + "&milestone="
						+ milestone.getNumber(),
				(issue) -> new Issue(this.rest, (Integer) issue.get("number"), (String) issue.get("title"),
						Issue.State.of((String) issue.get("state"))));
	}

	/**
     * Retrieves a list of objects from the specified endpoint using a GET request.
     * 
     * @param name the name of the endpoint to retrieve the objects from
     * @param mapper the function used to map the response body to the desired object type
     * @param <T> the type of the objects to retrieve
     * @return a list of objects retrieved from the endpoint
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
	private <T> List<T> get(String name, Function<Map<String, Object>, T> mapper) {
		ResponseEntity<List> response = this.rest.getForEntity(name, List.class);
		return ((List<Map<String, Object>>) response.getBody()).stream().map(mapper).toList();
	}

	/**
     * Suspends the execution of the current thread for the specified duration.
     *
     * @param duration the duration to sleep for
     * @throws IllegalArgumentException if the duration is negative
     * @throws InterruptedException if the thread is interrupted while sleeping
     */
    private static void sleep(Duration duration) {
		try {
			Thread.sleep(duration.toMillis());
		}
		catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
		}
	}

}
