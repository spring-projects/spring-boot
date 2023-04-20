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

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * {@link ConfigurationProperties @ConfigurationProperties} for Sonatype.
 *
 * @author Madhura Bhave
 */
@ConfigurationProperties(prefix = "sonatype")
public class SonatypeProperties {

	@JsonProperty("username")
	private String userToken;

	@JsonProperty("password")
	private String passwordToken;

	/**
	 * URL of the Nexus instance used to publish releases.
	 */
	private String url;

	/**
	 * ID of the staging profile used to publish releases.
	 */
	private String stagingProfileId;

	/**
	 * Time between requests made to determine if the closing of a staging repository has
	 * completed.
	 */
	private Duration pollingInterval = Duration.ofSeconds(15);

	/**
	 * Number of threads used to upload artifacts to the staging repository.
	 */
	private int uploadThreads = 8;

	/**
	 * Regular expression patterns of artifacts to exclude
	 */
	private List<String> exclude = new ArrayList<>();

	public String getUserToken() {
		return this.userToken;
	}

	public void setUserToken(String userToken) {
		this.userToken = userToken;
	}

	public String getPasswordToken() {
		return this.passwordToken;
	}

	public void setPasswordToken(String passwordToken) {
		this.passwordToken = passwordToken;
	}

	public String getUrl() {
		return this.url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getStagingProfileId() {
		return this.stagingProfileId;
	}

	public void setStagingProfileId(String stagingProfileId) {
		this.stagingProfileId = stagingProfileId;
	}

	public Duration getPollingInterval() {
		return this.pollingInterval;
	}

	public void setPollingInterval(Duration pollingInterval) {
		this.pollingInterval = pollingInterval;
	}

	public int getUploadThreads() {
		return this.uploadThreads;
	}

	public void setUploadThreads(int uploadThreads) {
		this.uploadThreads = uploadThreads;
	}

	public List<String> getExclude() {
		return this.exclude;
	}

	public void setExclude(List<String> exclude) {
		this.exclude = exclude;
	}

}
