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

package io.spring.concourse.releasescripts.sonatype;

import io.spring.concourse.releasescripts.ReleaseInfo;
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
 */
@Component
public class SonatypeService {

	private static final Logger logger = LoggerFactory.getLogger(SonatypeService.class);

	private static final String SONATYPE_REPOSITORY_URI = "https://oss.sonatype.org/service/local/repositories/releases/content/org/springframework/boot/spring-boot/";

	private final RestTemplate restTemplate;

	public SonatypeService(RestTemplateBuilder builder, SonatypeProperties sonatypeProperties) {
		String username = sonatypeProperties.getUserToken();
		String password = sonatypeProperties.getPasswordToken();
		if (StringUtils.hasLength(username)) {
			builder = builder.basicAuthentication(username, password);
		}
		this.restTemplate = builder.build();
	}

	/**
	 * Checks if artifacts are already published to Maven Central.
	 * @return true if artifacts are published
	 * @param releaseInfo the release information
	 */
	public boolean artifactsPublished(ReleaseInfo releaseInfo) {
		try {
			ResponseEntity<?> entity = this.restTemplate
					.getForEntity(String.format(SONATYPE_REPOSITORY_URI + "%s/spring-boot-%s.jar.sha1",
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

}
