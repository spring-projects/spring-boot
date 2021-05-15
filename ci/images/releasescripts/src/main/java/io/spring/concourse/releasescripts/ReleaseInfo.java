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

package io.spring.concourse.releasescripts;

import io.spring.concourse.releasescripts.artifactory.payload.BuildInfoResponse;

import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.util.StringUtils;

/**
 * Properties corresponding to the release.
 *
 * @author Madhura Bhave
 */
public class ReleaseInfo {

	private String buildName;

	private String buildNumber;

	private String groupId;

	private String version;

	public static ReleaseInfo from(BuildInfoResponse.BuildInfo buildInfo) {
		ReleaseInfo info = new ReleaseInfo();
		PropertyMapper propertyMapper = PropertyMapper.get();
		propertyMapper.from(buildInfo.getName()).to(info::setBuildName);
		propertyMapper.from(buildInfo.getNumber()).to(info::setBuildNumber);
		String[] moduleInfo = StringUtils.delimitedListToStringArray(buildInfo.getModules()[0].getId(), ":");
		propertyMapper.from(moduleInfo[0]).to(info::setGroupId);
		propertyMapper.from(moduleInfo[2]).to(info::setVersion);
		return info;
	}

	public String getBuildName() {
		return this.buildName;
	}

	public void setBuildName(String buildName) {
		this.buildName = buildName;
	}

	public String getBuildNumber() {
		return this.buildNumber;
	}

	public void setBuildNumber(String buildNumber) {
		this.buildNumber = buildNumber;
	}

	public String getGroupId() {
		return this.groupId;
	}

	public void setGroupId(String groupId) {
		this.groupId = groupId;
	}

	public String getVersion() {
		return this.version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

}
