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

package io.spring.concourse.releasescripts.command;

import java.io.File;
import java.nio.file.Files;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.spring.concourse.releasescripts.ReleaseInfo;
import io.spring.concourse.releasescripts.ReleaseType;
import io.spring.concourse.releasescripts.artifactory.payload.BuildInfoResponse;
import io.spring.concourse.releasescripts.artifactory.payload.BuildInfoResponse.BuildInfo;
import io.spring.concourse.releasescripts.sonatype.SonatypeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.ApplicationArguments;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

/**
 * Command used to publish a release to Maven Central.
 *
 * @author Andy Wilkinson
 */
@Component
public class PublishToCentralCommand implements Command {

	private static final Logger logger = LoggerFactory.getLogger(PublishToCentralCommand.class);

	private final SonatypeService sonatype;

	private final ObjectMapper objectMapper;

	public PublishToCentralCommand(SonatypeService sonatype, ObjectMapper objectMapper) {
		this.sonatype = sonatype;
		this.objectMapper = objectMapper;
	}

	@Override
	public String getName() {
		return "publishToCentral";
	}

	@Override
	public void run(ApplicationArguments args) throws Exception {
		List<String> nonOptionArgs = args.getNonOptionArgs();
		Assert.state(nonOptionArgs.size() == 4,
				"Release type, build info location, or artifacts location not specified");
		String releaseType = nonOptionArgs.get(1);
		ReleaseType type = ReleaseType.from(releaseType);
		if (!ReleaseType.RELEASE.equals(type)) {
			return;
		}
		String buildInfoLocation = nonOptionArgs.get(2);
		logger.debug("Loading build-info from " + buildInfoLocation);
		byte[] content = Files.readAllBytes(new File(buildInfoLocation).toPath());
		BuildInfoResponse buildInfoResponse = this.objectMapper.readValue(content, BuildInfoResponse.class);
		BuildInfo buildInfo = buildInfoResponse.getBuildInfo();
		String artifactsLocation = nonOptionArgs.get(3);
		this.sonatype.publish(ReleaseInfo.from(buildInfo), new File(artifactsLocation).toPath());
	}

}
