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

package io.spring.concourse.releasescripts.artifactory.payload;

/**
 * Represents a request to promote artifacts from a sourceRepo to a targetRepo.
 *
 * @author Madhura Bhave
 */
public class PromotionRequest {

	private final String status;

	private final String sourceRepo;

	private final String targetRepo;

	public PromotionRequest(String status, String sourceRepo, String targetRepo) {
		this.status = status;
		this.sourceRepo = sourceRepo;
		this.targetRepo = targetRepo;
	}

	public String getTargetRepo() {
		return this.targetRepo;
	}

	public String getSourceRepo() {
		return this.sourceRepo;
	}

	public String getStatus() {
		return this.status;
	}

}
