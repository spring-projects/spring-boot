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

package io.spring.concourse.releasescripts.sonatype;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonCreator.Mode;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.spring.concourse.releasescripts.ReleaseInfo;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

/**
 * Central class for interacting with Sonatype.
 *
 * @author Madhura Bhave
 * @author Andy Wilkinson
 */
@Component
public class SonatypeService {

	private static final Logger logger = LoggerFactory.getLogger(SonatypeService.class);

	private static final String NEXUS_REPOSITORY_PATH = "/service/local/repositories/releases/content/org/springframework/boot/spring-boot/";

	private static final String NEXUS_STAGING_PATH = "/service/local/staging/";

	private final ArtifactCollector artifactCollector;

	private final RestTemplate restTemplate;

	private final String stagingProfileId;

	private final Duration pollingInterval;

	private final int threads;

	public SonatypeService(RestTemplateBuilder builder, SonatypeProperties sonatypeProperties) {
		String username = sonatypeProperties.getUserToken();
		String password = sonatypeProperties.getPasswordToken();
		if (StringUtils.hasLength(username)) {
			builder = builder.basicAuthentication(username, password);
		}
		this.restTemplate = builder.rootUri(sonatypeProperties.getUrl()).build();
		this.stagingProfileId = sonatypeProperties.getStagingProfileId();
		this.pollingInterval = sonatypeProperties.getPollingInterval();
		this.threads = sonatypeProperties.getUploadThreads();

		this.artifactCollector = new ArtifactCollector(sonatypeProperties.getExclude());
	}

	/**
	 * Publishes the release by creating a staging repository and deploying to it the
	 * artifacts at the given {@code artifactsRoot}. The repository is then closed and,
	 * upon successfully closure, it is released.
	 * @param releaseInfo the release information
	 * @param artifactsRoot the root directory of the artifacts to stage
	 */
	public void publish(ReleaseInfo releaseInfo, Path artifactsRoot) {
		if (artifactsPublished(releaseInfo)) {
			return;
		}
		logger.info("Creating staging repository");
		String buildId = releaseInfo.getBuildNumber();
		String repositoryId = createStagingRepository(buildId);
		Collection<DeployableArtifact> artifacts = this.artifactCollector.collectArtifacts(artifactsRoot);
		logger.info("Staging repository {} created. Deploying {} artifacts", repositoryId, artifacts.size());
		deploy(artifacts, repositoryId);
		logger.info("Deploy complete. Closing staging repository");
		close(repositoryId);
		logger.info("Staging repository closed");
		release(repositoryId, buildId);
		logger.info("Staging repository released");
	}

	private boolean artifactsPublished(ReleaseInfo releaseInfo) {
		try {
			ResponseEntity<?> entity = this.restTemplate
					.getForEntity(String.format(NEXUS_REPOSITORY_PATH + "%s/spring-boot-%s.jar.sha1",
							releaseInfo.getVersion(), releaseInfo.getVersion()), byte[].class);
			if (HttpStatus.OK.equals(entity.getStatusCode())) {
				logger.info("Already published to Sonatype.");
				return true;
			}
		}
		catch (HttpClientErrorException ex) {

		}
		return false;
	}

	private String createStagingRepository(String buildId) {
		Map<String, Object> body = new HashMap<>();
		body.put("data", Collections.singletonMap("description", buildId));
		PromoteResponse response = this.restTemplate.postForObject(
				String.format(NEXUS_STAGING_PATH + "profiles/%s/start", this.stagingProfileId), body,
				PromoteResponse.class);
		String repositoryId = response.data.stagedRepositoryId;
		return repositoryId;
	}

	private void deploy(Collection<DeployableArtifact> artifacts, String repositoryId) {
		ExecutorService executor = Executors.newFixedThreadPool(this.threads);
		try {
			CompletableFuture.allOf(artifacts.stream()
					.map((artifact) -> CompletableFuture.runAsync(() -> deploy(artifact, repositoryId), executor))
					.toArray(CompletableFuture[]::new)).get(60, TimeUnit.MINUTES);
		}
		catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			throw new RuntimeException("Interrupted during artifact deploy");
		}
		catch (ExecutionException ex) {
			throw new RuntimeException("Deploy failed", ex);
		}
		catch (TimeoutException ex) {
			throw new RuntimeException("Deploy timed out", ex);
		}
		finally {
			executor.shutdown();
		}
	}

	private void deploy(DeployableArtifact deployableArtifact, String repositoryId) {
		try {
			this.restTemplate.put(
					NEXUS_STAGING_PATH + "deployByRepositoryId/" + repositoryId + "/" + deployableArtifact.getPath(),
					deployableArtifact.getResource());
			logger.info("Deloyed {}", deployableArtifact.getPath());
		}
		catch (HttpClientErrorException ex) {
			logger.error("Failed to deploy {}. Error response: {}", deployableArtifact.getPath(),
					ex.getResponseBodyAsString());
			throw ex;
		}
	}

	private void close(String stagedRepositoryId) {
		Map<String, Object> body = new HashMap<>();
		body.put("data", Collections.singletonMap("stagedRepositoryId", stagedRepositoryId));
		this.restTemplate.postForEntity(String.format(NEXUS_STAGING_PATH + "profiles/%s/finish", this.stagingProfileId),
				body, Void.class);
		logger.info("Close requested. Awaiting result");
		while (true) {
			StagingRepository repository = this.restTemplate
					.getForObject(NEXUS_STAGING_PATH + "repository/" + stagedRepositoryId, StagingRepository.class);
			if (!repository.transitioning) {
				if ("open".equals(repository.type)) {
					logFailures(stagedRepositoryId);
					throw new RuntimeException("Close failed");
				}
				return;
			}
			try {
				Thread.sleep(this.pollingInterval.toMillis());
			}
			catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
				throw new RuntimeException("Interrupted while waiting for staging repository to close", ex);
			}
		}
	}

	private void logFailures(String stagedRepositoryId) {
		try {
			StagingRepositoryActivity[] activities = this.restTemplate.getForObject(
					NEXUS_STAGING_PATH + "repository/" + stagedRepositoryId + "/activity",
					StagingRepositoryActivity[].class);
			List<String> failureMessages = Stream.of(activities).flatMap((activity) -> activity.events.stream())
					.filter((event) -> event.severity > 0).flatMap((event) -> event.properties.stream())
					.filter((property) -> "failureMessage".equals(property.name))
					.map((property) -> "    " + property.value).collect(Collectors.toList());
			if (failureMessages.isEmpty()) {
				logger.error("Close failed for unknown reasons");
			}
			logger.error("Close failed:\n{}", Strings.join(failureMessages, '\n'));
		}
		catch (Exception ex) {
			logger.error("Failed to determine causes of close failure", ex);
		}
	}

	private void release(String stagedRepositoryId, String buildId) {
		Map<String, Object> data = new HashMap<>();
		data.put("stagedRepositoryIds", Arrays.asList(stagedRepositoryId));
		data.put("description", "Releasing " + buildId);
		data.put("autoDropAfterRelease", true);
		Map<String, Object> body = Collections.singletonMap("data", data);
		this.restTemplate.postForEntity(NEXUS_STAGING_PATH + "bulk/promote", body, Void.class);
	}

	private static final class PromoteResponse {

		private final Data data;

		@JsonCreator(mode = Mode.PROPERTIES)
		private PromoteResponse(@JsonProperty("data") Data data) {
			this.data = data;
		}

		private static final class Data {

			private final String stagedRepositoryId;

			@JsonCreator(mode = Mode.PROPERTIES)
			Data(@JsonProperty("stagedRepositoryId") String stagedRepositoryId) {
				this.stagedRepositoryId = stagedRepositoryId;
			}

		}

	}

	private static final class StagingRepository {

		private final String type;

		private final boolean transitioning;

		private StagingRepository(String type, boolean transitioning) {
			this.type = type;
			this.transitioning = transitioning;
		}

	}

	private static final class StagingRepositoryActivity {

		private final List<Event> events;

		@JsonCreator
		private StagingRepositoryActivity(@JsonProperty("events") List<Event> events) {
			this.events = events;
		}

		private static class Event {

			private final List<Property> properties;

			private final int severity;

			@JsonCreator
			public Event(@JsonProperty("name") String name, @JsonProperty("properties") List<Property> properties,
					@JsonProperty("severity") int severity) {
				this.properties = properties;
				this.severity = severity;
			}

			private static class Property {

				private final String name;

				private final String value;

				@JsonCreator
				private Property(@JsonProperty("name") String name, @JsonProperty("value") String value) {
					this.name = name;
					this.value = value;
				}

			}

		}

	}

}
