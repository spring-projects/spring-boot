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

package io.spring.concourse.releasescripts.sdkman;

import java.net.URI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * Central class for interacting with SDKMAN's API.
 *
 * @author Madhura Bhave
 * @author Moritz Halbritter
 */
@Component
public class SdkmanService {

	private static final Logger logger = LoggerFactory.getLogger(SdkmanService.class);

	private static final String SDKMAN_URL = "https://vendors.sdkman.io/";

	private static final String DOWNLOAD_URL = "https://repo.maven.apache.org/maven2/org/springframework/boot/spring-boot-cli/"
			+ "%s/spring-boot-cli-%s-bin.zip";

	private static final String CHANGELOG_URL = "https://github.com/spring-projects/spring-boot/releases/tag/v%s";

	private static final String SPRING_BOOT = "springboot";

	private final RestTemplate restTemplate;

	private final SdkmanProperties properties;

	private final String CONSUMER_KEY_HEADER = "Consumer-Key";

	private final String CONSUMER_TOKEN_HEADER = "Consumer-Token";

	public SdkmanService(RestTemplateBuilder builder, SdkmanProperties properties) {
		this.restTemplate = builder.build();
		this.properties = properties;
	}

	public void publish(String version, boolean makeDefault) {
		release(version);
		if (makeDefault) {
			makeDefault(version);
		}
		broadcast(version);
	}

	private void broadcast(String version) {
		BroadcastRequest broadcastRequest = new BroadcastRequest(version, String.format(CHANGELOG_URL, version));
		RequestEntity<BroadcastRequest> broadcastEntity = RequestEntity.post(URI.create(SDKMAN_URL + "announce/struct"))
				.header(CONSUMER_KEY_HEADER, this.properties.getConsumerKey())
				.header(CONSUMER_TOKEN_HEADER, this.properties.getConsumerToken())
				.contentType(MediaType.APPLICATION_JSON).body(broadcastRequest);
		this.restTemplate.exchange(broadcastEntity, String.class);
		logger.debug("Broadcast complete");
	}

	private void makeDefault(String version) {
		logger.debug("Making this version the default");
		Request request = new Request(version);
		RequestEntity<Request> requestEntity = RequestEntity.put(URI.create(SDKMAN_URL + "default"))
				.header(CONSUMER_KEY_HEADER, this.properties.getConsumerKey())
				.header(CONSUMER_TOKEN_HEADER, this.properties.getConsumerToken())
				.contentType(MediaType.APPLICATION_JSON).body(request);
		this.restTemplate.exchange(requestEntity, String.class);
		logger.debug("Make default complete");
	}

	private void release(String version) {
		ReleaseRequest releaseRequest = new ReleaseRequest(version, String.format(DOWNLOAD_URL, version, version));
		RequestEntity<ReleaseRequest> releaseEntity = RequestEntity.post(URI.create(SDKMAN_URL + "release"))
				.header(CONSUMER_KEY_HEADER, this.properties.getConsumerKey())
				.header(CONSUMER_TOKEN_HEADER, this.properties.getConsumerToken())
				.contentType(MediaType.APPLICATION_JSON).body(releaseRequest);
		this.restTemplate.exchange(releaseEntity, String.class);
		logger.debug("Release complete");
	}

	static class Request {

		private final String candidate = SPRING_BOOT;

		private final String version;

		Request(String version) {
			this.version = version;
		}

		public String getCandidate() {
			return this.candidate;
		}

		public String getVersion() {
			return this.version;
		}

	}

	static class ReleaseRequest extends Request {

		private final String url;

		ReleaseRequest(String version, String url) {
			super(version);
			this.url = url;
		}

		public String getUrl() {
			return this.url;
		}

	}

	static class BroadcastRequest extends Request {

		private final String hashtag = SPRING_BOOT;

		private final String url;

		BroadcastRequest(String version, String url) {
			super(version);
			this.url = url;
		}

		public String getHashtag() {
			return this.hashtag;
		}

		public String getUrl() {
			return url;
		}

	}

}
