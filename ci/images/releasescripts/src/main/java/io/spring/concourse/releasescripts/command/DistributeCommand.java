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

package io.spring.concourse.releasescripts.command;

import java.io.File;
import java.nio.file.Files;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.spring.concourse.releasescripts.ReleaseInfo;
import io.spring.concourse.releasescripts.ReleaseType;
import io.spring.concourse.releasescripts.artifactory.ArtifactoryService;
import io.spring.concourse.releasescripts.artifactory.payload.BuildInfoResponse;
import io.spring.concourse.releasescripts.artifactory.payload.BuildInfoResponse.Artifact;
import io.spring.concourse.releasescripts.artifactory.payload.BuildInfoResponse.BuildInfo;
import io.spring.concourse.releasescripts.artifactory.payload.BuildInfoResponse.Module;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.ApplicationArguments;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

/**
 * Command used to deploy builds from Artifactory to Bintray.
 *
 * @author Madhura Bhave
 * @author Phillip Webb
 */
@Component
public class DistributeCommand implements Command {

	private static final Logger logger = LoggerFactory.getLogger(DistributeCommand.class);

	private final ArtifactoryService artifactoryService;

	private final ObjectMapper objectMapper;

	private final List<Pattern> optionalDeployments;

	public DistributeCommand(ArtifactoryService artifactoryService, ObjectMapper objectMapper,
			DistributeProperties distributeProperties) {
		this.artifactoryService = artifactoryService;
		this.objectMapper = objectMapper;
		this.optionalDeployments = distributeProperties.getOptionalDeployments().stream().map(Pattern::compile)
				.collect(Collectors.toList());
	}

	@Override
	public void run(ApplicationArguments args) throws Exception {
		logger.debug("Running 'distribute' command");
		List<String> nonOptionArgs = args.getNonOptionArgs();
		Assert.state(!nonOptionArgs.isEmpty(), "No command argument specified");
		Assert.state(nonOptionArgs.size() == 3, "Release type or build info not specified");
		String releaseType = nonOptionArgs.get(1);
		ReleaseType type = ReleaseType.from(releaseType);
		if (!ReleaseType.RELEASE.equals(type)) {
			logger.info("Skipping distribution of " + type + " type");
			return;
		}
		String buildInfoLocation = nonOptionArgs.get(2);
		logger.debug("Loading build-info from " + buildInfoLocation);
		byte[] content = Files.readAllBytes(new File(buildInfoLocation).toPath());
		BuildInfoResponse buildInfoResponse = this.objectMapper.readValue(content, BuildInfoResponse.class);
		BuildInfo buildInfo = buildInfoResponse.getBuildInfo();
		logger.debug("Loading build info:");
		for (Module module : buildInfo.getModules()) {
			logger.debug(module.getId());
			for (Artifact artifact : module.getArtifacts()) {
				logger.debug(artifact.getSha256() + " " + artifact.getName());
			}
		}
		ReleaseInfo releaseInfo = ReleaseInfo.from(buildInfo);
		Set<String> artifactDigests = buildInfo.getArtifactDigests(this::isIncluded);
		this.artifactoryService.distribute(type.getRepo(), releaseInfo, artifactDigests);
	}

	private boolean isIncluded(Artifact artifact) {
		String path = artifact.getName();
		for (Pattern optionalDeployment : this.optionalDeployments) {
			if (optionalDeployment.matcher(path).matches()) {
				return false;
			}
		}
		return true;
	}

}
