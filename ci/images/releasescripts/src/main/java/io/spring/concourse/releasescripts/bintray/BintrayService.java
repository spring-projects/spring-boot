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

package io.spring.concourse.releasescripts.bintray;

import java.net.URI;
import java.time.Duration;
import java.util.HashSet;
import java.util.Set;

import io.spring.concourse.releasescripts.ReleaseInfo;
import io.spring.concourse.releasescripts.sonatype.SonatypeProperties;
import io.spring.concourse.releasescripts.sonatype.SonatypeService;
import org.awaitility.core.ConditionTimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import static org.awaitility.Awaitility.waitAtMost;

/**
 * Central class for interacting with Bintray's REST API.
 *
 * @author Madhura Bhave
 */
@Component
public class BintrayService {

	private static final Logger logger = LoggerFactory.getLogger(BintrayService.class);

	private static final String BINTRAY_URL = "https://api.bintray.com/";

	private static final String GRADLE_PLUGIN_REQUEST = "[ { \"name\": \"gradle-plugin\", \"values\": [\"org.springframework.boot:org.springframework.boot:spring-boot-gradle-plugin\"] } ]";

	private final RestTemplate restTemplate;

	private final BintrayProperties bintrayProperties;

	private final SonatypeProperties sonatypeProperties;

	private final SonatypeService sonatypeService;

	public BintrayService(RestTemplateBuilder builder, BintrayProperties bintrayProperties,
			SonatypeProperties sonatypeProperties, SonatypeService sonatypeService) {
		this.bintrayProperties = bintrayProperties;
		this.sonatypeProperties = sonatypeProperties;
		this.sonatypeService = sonatypeService;
		String username = bintrayProperties.getUsername();
		String apiKey = bintrayProperties.getApiKey();
		if (StringUtils.hasLength(username)) {
			builder = builder.basicAuthentication(username, apiKey);
		}
		this.restTemplate = builder.build();
	}

	public boolean isDistributionStarted(ReleaseInfo releaseInfo) {
		logger.debug("Checking if distribution is started");
		RequestEntity<Void> request = getPackageFilesRequest(releaseInfo, 1);
		try {
			logger.debug("Checking Bintray");
			this.restTemplate.exchange(request, PackageFile[].class).getBody();
			return true;
		}
		catch (HttpClientErrorException ex) {
			if (ex.getStatusCode() != HttpStatus.NOT_FOUND) {
				throw ex;
			}
			return false;
		}
	}

	public boolean isDistributionComplete(ReleaseInfo releaseInfo, Set<String> requiredDigests, Duration timeout) {
		return isDistributionComplete(releaseInfo, requiredDigests, timeout, Duration.ofSeconds(20));
	}

	public boolean isDistributionComplete(ReleaseInfo releaseInfo, Set<String> requiredDigests, Duration timeout,
			Duration pollInterval) {
		logger.debug("Checking if distribution is complete");
		RequestEntity<Void> request = getPackageFilesRequest(releaseInfo, 0);
		try {
			waitAtMost(timeout).with().pollDelay(Duration.ZERO).pollInterval(pollInterval).until(() -> {
				logger.debug("Checking Bintray");
				try {
					PackageFile[] published = this.restTemplate.exchange(request, PackageFile[].class).getBody();
					return hasPublishedAll(published, requiredDigests);
				}
				catch (HttpClientErrorException.NotFound ex) {
					return false;
				}
			});
		}
		catch (ConditionTimeoutException ex) {
			logger.debug("Timeout checking Bintray");
			return false;
		}
		return true;
	}

	private boolean hasPublishedAll(PackageFile[] published, Set<String> requiredDigests) {
		if (published == null || published.length == 0) {
			logger.debug("Bintray returned no published files");
			return false;
		}
		Set<String> remaining = new HashSet<>(requiredDigests);
		for (PackageFile publishedFile : published) {
			logger.debug(
					"Found published file " + publishedFile.getName() + " with digest " + publishedFile.getSha256());
			remaining.remove(publishedFile.getSha256());
		}
		if (remaining.isEmpty()) {
			logger.debug("Found all required digests");
			return true;
		}
		logger.debug(remaining.size() + " digests have not been published:");
		remaining.forEach(logger::debug);
		return false;
	}

	private RequestEntity<Void> getPackageFilesRequest(ReleaseInfo releaseInfo, int includeUnpublished) {
		return RequestEntity.get(URI.create(BINTRAY_URL + "packages/" + this.bintrayProperties.getSubject() + "/"
				+ this.bintrayProperties.getRepo() + "/" + releaseInfo.getGroupId() + "/versions/"
				+ releaseInfo.getVersion() + "/files?include_unpublished=" + includeUnpublished)).build();
	}

	/**
	 * Add attributes to Spring Boot's Gradle plugin.
	 * @param releaseInfo the release information
	 */
	public void publishGradlePlugin(ReleaseInfo releaseInfo) {
		logger.debug("Publishing Gradle plugin");
		RequestEntity<String> requestEntity = RequestEntity
				.post(URI.create(BINTRAY_URL + "packages/" + this.bintrayProperties.getSubject() + "/"
						+ this.bintrayProperties.getRepo() + "/" + releaseInfo.getGroupId() + "/versions/"
						+ releaseInfo.getVersion() + "/attributes"))
				.contentType(MediaType.APPLICATION_JSON).body(GRADLE_PLUGIN_REQUEST);
		try {
			this.restTemplate.exchange(requestEntity, Object.class);
			logger.debug("Publishing Gradle plugin complete");
		}
		catch (HttpClientErrorException ex) {
			logger.info("Failed to add attribute to Gradle plugin");
			throw ex;
		}
	}

	/**
	 * Sync artifacts from Bintray to Maven Central.
	 * @param releaseInfo the release information
	 */
	public void syncToMavenCentral(ReleaseInfo releaseInfo) {
		logger.info("Calling Bintray to sync to Sonatype");
		if (this.sonatypeService.artifactsPublished(releaseInfo)) {
			logger.info("Artifacts already published");
			return;
		}
		RequestEntity<SonatypeProperties> requestEntity = RequestEntity
				.post(URI.create(String.format(BINTRAY_URL + "maven_central_sync/%s/%s/%s/versions/%s",
						this.bintrayProperties.getSubject(), this.bintrayProperties.getRepo(), releaseInfo.getGroupId(),
						releaseInfo.getVersion())))
				.contentType(MediaType.APPLICATION_JSON).body(this.sonatypeProperties);
		try {
			this.restTemplate.exchange(requestEntity, Object.class);
			logger.debug("Sync complete");
		}
		catch (HttpClientErrorException ex) {
			logger.info("Failed to sync");
			throw ex;
		}
	}

}
