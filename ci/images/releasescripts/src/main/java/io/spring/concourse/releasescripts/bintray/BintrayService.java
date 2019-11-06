/*
 * Copyright 2012-2019 the original author or authors.
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
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import io.spring.concourse.releasescripts.ReleaseInfo;
import io.spring.concourse.releasescripts.sonatype.SonatypeProperties;
import io.spring.concourse.releasescripts.sonatype.SonatypeService;
import io.spring.concourse.releasescripts.system.ConsoleLogger;
import org.awaitility.core.ConditionTimeoutException;

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

	private static final String BINTRAY_URL = "https://api.bintray.com/";

	private static final String GRADLE_PLUGIN_REQUEST = "[ { \"name\": \"gradle-plugin\", \"values\": [\"org.springframework.boot:org.springframework.boot:spring-boot-gradle-plugin\"] } ]";

	private final RestTemplate restTemplate;

	private final BintrayProperties bintrayProperties;

	private final SonatypeProperties sonatypeProperties;

	private final SonatypeService sonatypeService;

	private static final ConsoleLogger console = new ConsoleLogger();

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

	public boolean isDistributionComplete(ReleaseInfo releaseInfo) {
		RequestEntity<Void> allFilesRequest = getRequest(releaseInfo, 1);
		Object[] allFiles = waitAtMost(5, TimeUnit.MINUTES).with().pollDelay(20, TimeUnit.SECONDS).until(() -> {
			try {
				return this.restTemplate.exchange(allFilesRequest, Object[].class).getBody();
			}
			catch (HttpClientErrorException ex) {
				if (ex.getStatusCode() != HttpStatus.NOT_FOUND) {
					throw ex;
				}
				return null;
			}
		}, Objects::nonNull);
		RequestEntity<Void> publishedFilesRequest = getRequest(releaseInfo, 0);
		try {
			waitAtMost(40, TimeUnit.MINUTES).with().pollDelay(20, TimeUnit.SECONDS).until(() -> {
				Object[] publishedFiles = this.restTemplate.exchange(publishedFilesRequest, Object[].class).getBody();
				return allFiles.length == publishedFiles.length;
			});
		}
		catch (ConditionTimeoutException ex) {
			return false;
		}
		return true;
	}

	private RequestEntity<Void> getRequest(ReleaseInfo releaseInfo, int includeUnpublished) {
		return RequestEntity.get(URI.create(BINTRAY_URL + "packages/" + this.bintrayProperties.getSubject() + "/"
				+ this.bintrayProperties.getRepo() + "/" + releaseInfo.getGroupId() + "/versions/"
				+ releaseInfo.getVersion() + "/files?include_unpublished=" + includeUnpublished)).build();
	}

	/**
	 * Add attributes to Spring Boot's Gradle plugin.
	 * @param releaseInfo the release information
	 */
	public void publishGradlePlugin(ReleaseInfo releaseInfo) {
		RequestEntity<String> requestEntity = RequestEntity
				.post(URI.create(BINTRAY_URL + "packages/" + this.bintrayProperties.getSubject() + "/"
						+ this.bintrayProperties.getRepo() + "/" + releaseInfo.getGroupId() + "/versions/"
						+ releaseInfo.getVersion() + "/attributes"))
				.contentType(MediaType.APPLICATION_JSON).body(GRADLE_PLUGIN_REQUEST);
		try {
			this.restTemplate.exchange(requestEntity, Object.class);
		}
		catch (HttpClientErrorException ex) {
			console.log("Failed to add attribute to gradle plugin.");
			throw ex;
		}
	}

	/**
	 * Sync artifacts from Bintray to Maven Central.
	 * @param releaseInfo the release information
	 */
	public void syncToMavenCentral(ReleaseInfo releaseInfo) {
		console.log("Calling Bintray to sync to Sonatype");
		if (this.sonatypeService.artifactsPublished(releaseInfo)) {
			return;
		}
		RequestEntity<SonatypeProperties> requestEntity = RequestEntity
				.post(URI.create(String.format(BINTRAY_URL + "maven_central_sync/%s/%s/%s/versions/%s",
						this.bintrayProperties.getSubject(), this.bintrayProperties.getRepo(), releaseInfo.getGroupId(),
						releaseInfo.getVersion())))
				.contentType(MediaType.APPLICATION_JSON).body(this.sonatypeProperties);
		try {
			this.restTemplate.exchange(requestEntity, Object.class);
		}
		catch (HttpClientErrorException ex) {
			console.log("Failed to sync.");
			throw ex;
		}
	}

}
