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

package io.spring.concourse.releasescripts.command;

import java.io.File;
import java.nio.file.Files;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.spring.concourse.releasescripts.ReleaseInfo;
import io.spring.concourse.releasescripts.ReleaseType;
import io.spring.concourse.releasescripts.artifactory.payload.BuildInfoResponse;
import io.spring.concourse.releasescripts.bintray.BintrayService;

import org.springframework.boot.ApplicationArguments;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

/**
 * Command used to sync artifacts to Maven Central.
 *
 * @author Madhura Bhave
 */
@Component
public class SyncToCentralCommand implements Command {

	private static final String SYNC_TO_CENTRAL_COMMAND = "syncToCentral";

	private final BintrayService service;

	private final ObjectMapper objectMapper;

	public SyncToCentralCommand(BintrayService service, ObjectMapper objectMapper) {
		this.service = service;
		this.objectMapper = objectMapper;
	}

	@Override
	public String getName() {
		return SYNC_TO_CENTRAL_COMMAND;
	}

	@Override
	public void run(ApplicationArguments args) throws Exception {
		List<String> nonOptionArgs = args.getNonOptionArgs();
		Assert.state(!nonOptionArgs.isEmpty(), "No command argument specified");
		Assert.state(nonOptionArgs.size() == 3, "Release type or build info not specified");
		String releaseType = nonOptionArgs.get(1);
		ReleaseType type = ReleaseType.from(releaseType);
		if (!ReleaseType.RELEASE.equals(type)) {
			return;
		}
		String buildInfoLocation = nonOptionArgs.get(2);
		byte[] content = Files.readAllBytes(new File(buildInfoLocation).toPath());
		BuildInfoResponse buildInfoResponse = this.objectMapper.readValue(content, BuildInfoResponse.class);
		ReleaseInfo releaseInfo = ReleaseInfo.from(buildInfoResponse.getBuildInfo());
		this.service.syncToMavenCentral(releaseInfo);
	}

}
