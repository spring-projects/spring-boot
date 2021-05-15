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

package io.spring.concourse.releasescripts.artifactory;

import java.net.URI;
import java.time.Duration;
import java.util.Set;

import io.spring.concourse.releasescripts.ReleaseInfo;
import io.spring.concourse.releasescripts.artifactory.payload.BuildInfoResponse;
import io.spring.concourse.releasescripts.artifactory.payload.DistributionRequest;
import io.spring.concourse.releasescripts.artifactory.payload.PromotionRequest;
import io.spring.concourse.releasescripts.bintray.BintrayService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

/**
 * Central class for interacting with Artifactory's REST API.
 *
 * @author Madhura Bhave
 */
@Component
public class ArtifactoryService {

	private static final Logger logger = LoggerFactory.getLogger(ArtifactoryService.class);

	private static final String ARTIFACTORY_URL = "https://repo.spring.io";

	private static final String PROMOTION_URL = ARTIFACTORY_URL + "/api/build/promote/";

	private static final String BUILD_INFO_URL = ARTIFACTORY_URL + "/api/build/";

	private static final String DISTRIBUTION_URL = ARTIFACTORY_URL + "/api/build/distribute/";

	private static final String STAGING_REPO = "libs-staging-local";

	private final RestTemplate restTemplate;

	private final BintrayService bintrayService;

	public ArtifactoryService(RestTemplateBuilder builder, ArtifactoryProperties artifactoryProperties,
			BintrayService bintrayService) {
		this.bintrayService = bintrayService;
		String username = artifactoryProperties.getUsername();
		String password = artifactoryProperties.getPassword();
		if (StringUtils.hasLength(username)) {
			builder = builder.basicAuthentication(username, password);
		}
		this.restTemplate = builder.build();
	}

	/**
	 * Move artifacts to a target repository in Artifactory.
	 * @param targetRepo the targetRepo
	 * @param releaseInfo the release information
	 */
	public void promote(String targetRepo, ReleaseInfo releaseInfo) {
		PromotionRequest request = getPromotionRequest(targetRepo);
		String buildName = releaseInfo.getBuildName();
		String buildNumber = releaseInfo.getBuildNumber();
		logger.info("Promoting " + buildName + "/" + buildNumber + " to " + request.getTargetRepo());
		RequestEntity<PromotionRequest> requestEntity = RequestEntity
				.post(URI.create(PROMOTION_URL + buildName + "/" + buildNumber)).contentType(MediaType.APPLICATION_JSON)
				.body(request);
		try {
			this.restTemplate.exchange(requestEntity, String.class);
			logger.debug("Promotion complete");
		}
		catch (HttpClientErrorException ex) {
			boolean isAlreadyPromoted = isAlreadyPromoted(buildName, buildNumber, request.getTargetRepo());
			if (isAlreadyPromoted) {
				logger.info("Already promoted.");
			}
			else {
				logger.info("Promotion failed.");
				throw ex;
			}
		}
	}

	private boolean isAlreadyPromoted(String buildName, String buildNumber, String targetRepo) {
		try {
			logger.debug("Checking if alreay promoted");
			ResponseEntity<BuildInfoResponse> entity = this.restTemplate
					.getForEntity(BUILD_INFO_URL + buildName + "/" + buildNumber, BuildInfoResponse.class);
			BuildInfoResponse.Status status = entity.getBody().getBuildInfo().getStatuses()[0];
			logger.debug("Returned repository " + status.getRepository() + " expecting " + targetRepo);
			return status.getRepository().equals(targetRepo);
		}
		catch (HttpClientErrorException ex) {
			logger.debug("Client error, assuming not promoted");
			return false;
		}
	}

	/**
	 * Deploy builds from Artifactory to Bintray.
	 * @param sourceRepo the source repo in Artifactory.
	 * @param releaseInfo the resease info
	 * @param artifactDigests the artifact digests
	 */
	public void distribute(String sourceRepo, ReleaseInfo releaseInfo, Set<String> artifactDigests) {
		logger.debug("Attempting distribute via Artifactory");
		if (!this.bintrayService.isDistributionStarted(releaseInfo)) {
			startDistribute(sourceRepo, releaseInfo);
		}
		if (!this.bintrayService.isDistributionComplete(releaseInfo, artifactDigests, Duration.ofMinutes(60))) {
			throw new DistributionTimeoutException("Distribution timed out.");
		}
	}

	private void startDistribute(String sourceRepo, ReleaseInfo releaseInfo) {
		DistributionRequest request = new DistributionRequest(new String[] { sourceRepo });
		RequestEntity<DistributionRequest> requestEntity = RequestEntity
				.post(URI.create(DISTRIBUTION_URL + releaseInfo.getBuildName() + "/" + releaseInfo.getBuildNumber()))
				.contentType(MediaType.APPLICATION_JSON).body(request);
		try {
			this.restTemplate.exchange(requestEntity, Object.class);
			logger.debug("Distribute call completed");
		}
		catch (HttpClientErrorException ex) {
			logger.info("Failed to distribute.");
			throw ex;
		}
	}

	private PromotionRequest getPromotionRequest(String targetRepo) {
		return new PromotionRequest("staged", STAGING_REPO, targetRepo);
	}

}
