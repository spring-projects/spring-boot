/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.build.bom.bomr.github;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException.Forbidden;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Standard implementation of {@link GitHubRepository}.
 *
 * @author Andy Wilkinson
 */
final class StandardGitHubRepository implements GitHubRepository {

	private final RestClient rest;

	StandardGitHubRepository(RestClient restClient) {
		this.rest = restClient;
	}

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
			ResponseEntity<Map> response = this.rest.post()
				.uri("issues")
				.body(requestBody)
				.retrieve()
				.toEntity(Map.class);
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

	@Override
	public Set<String> getLabels() {
		return new HashSet<>(get("labels?per_page=100", (label) -> (String) label.get("name")));
	}

	@Override
	public List<Milestone> getMilestones() {
		return get("milestones?per_page=100", (milestone) -> new Milestone((String) milestone.get("title"),
				(Integer) milestone.get("number"),
				(milestone.get("due_on") != null) ? OffsetDateTime.parse((String) milestone.get("due_on")) : null));
	}

	@Override
	public List<Issue> findIssues(List<String> labels, Milestone milestone) {
		return get(
				"issues?per_page=100&state=all&labels=" + String.join(",", labels) + "&milestone="
						+ milestone.getNumber(),
				(issue) -> new Issue(this.rest, (Integer) issue.get("number"), (String) issue.get("title"),
						Issue.State.of((String) issue.get("state"))));
	}

	@Override
	@SuppressWarnings("rawtypes")
	public String getContent(String path, String ref) {
		Map body = this.rest.get().uri("contents/" + path + "?ref=" + ref).retrieve().body(Map.class);
		return base64Decode((String) body.get("content"));

	}

	private String base64Decode(String encoded) {
		return new String(Base64.getDecoder().decode(encoded.replace("\n", "").getBytes(StandardCharsets.UTF_8)),
				StandardCharsets.UTF_8);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private <T> List<T> get(String name, Function<Map<String, Object>, T> mapper) {
		ResponseEntity<List> response = this.rest.get().uri(name).retrieve().toEntity(List.class);
		return ((List<Map<String, Object>>) response.getBody()).stream().map(mapper).toList();
	}

	private static void sleep(Duration duration) {
		try {
			Thread.sleep(duration.toMillis());
		}
		catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
		}
	}

}
