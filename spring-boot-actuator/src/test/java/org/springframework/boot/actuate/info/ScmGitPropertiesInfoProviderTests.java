/*
 * Copyright 2012-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.actuate.info;

import org.junit.Test;

import org.springframework.boot.actuate.info.ScmGitPropertiesInfoProvider.GitInfo.Commit;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

public class ScmGitPropertiesInfoProviderTests {


	@Test
	public void provide_HasBadFormatButExists_EmptyInfoReturned() throws Exception {
		Resource resource = new ByteArrayResource("GARBAGE".getBytes());
		ScmGitPropertiesInfoProvider scmGitPropertiesInfoProvider = new ScmGitPropertiesInfoProvider(resource);

		Info actual = scmGitPropertiesInfoProvider.provide();
		assertThat(actual).isNotNull();
		assertThat((String) actual.get("branch")).isNull();
		Commit actualCommit = actual.get("commit");
		assertThat(actualCommit).isNotNull();
		assertThat(actualCommit.getId()).isEqualTo("");
		assertThat(actualCommit.getTime()).isNull();
	}

	@Test
	public void provide_HasValidFormat_ExpectedDataReturned() throws Exception {
		String gitProperties = "git.commit.id.abbrev=e02a4f3\r\n"
				+ "git.commit.user.email=dsyer@vmware.com\r\n"
				+ "git.commit.message.full=Update Spring\r\n"
				+ "git.commit.id=e02a4f3b6f452cdbf6dd311f1362679eb4c31ced\r\n"
				+ "git.commit.message.short=Update Spring\r\n"
				+ "git.commit.user.name=Dave Syer\r\n"
				+ "git.build.user.name=Dave Syer\r\n"
				+ "git.build.user.email=dsyer@vmware.com\r\n"
				+ "git.branch=develop\r\n"
				+ "git.commit.time=2013-04-24T08\\:42\\:13+0100\r\n"
				+ "git.build.time=2013-05-23T09\\:26\\:42+0100\r\n";

		Resource resource = new ByteArrayResource(gitProperties.getBytes());
		ScmGitPropertiesInfoProvider scmGitPropertiesInfoProvider = new ScmGitPropertiesInfoProvider(resource);

		Info actual = scmGitPropertiesInfoProvider.provide();
		assertThat(actual).isNotNull();
		assertThat((String) actual.get("branch")).isEqualTo("develop");
		Commit actualCommit = actual.get("commit");
		assertThat(actualCommit).isNotNull();
		assertThat(actualCommit.getId()).isEqualTo("e02a4f3");
		assertThat(actualCommit.getTime()).isEqualTo("2013-04-24T08:42:13+0100");
	}


	@Test
	public void provide_HasValidFormatButMissingCommitTime_ExpectedDataReturnedWithoutCommitTime() throws Exception {
		String gitProperties = "git.commit.id.abbrev=e02a4f3\r\n"
				+ "git.commit.user.email=dsyer@vmware.com\r\n"
				+ "git.commit.message.full=Update Spring\r\n"
				+ "git.commit.id=e02a4f3b6f452cdbf6dd311f1362679eb4c31ced\r\n"
				+ "git.commit.message.short=Update Spring\r\n"
				+ "git.commit.user.name=Dave Syer\r\n"
				+ "git.build.user.name=Dave Syer\r\n"
				+ "git.build.user.email=dsyer@vmware.com\r\n"
				+ "git.branch=develop\r\n"
				+ "git.build.time=2013-05-23T09\\:26\\:42+0100\r\n";

		Resource resource = new ByteArrayResource(gitProperties.getBytes());
		ScmGitPropertiesInfoProvider scmGitPropertiesInfoProvider = new ScmGitPropertiesInfoProvider(resource);

		Info actual = scmGitPropertiesInfoProvider.provide();
		assertThat(actual).isNotNull();
		assertThat((String) actual.get("branch")).isEqualTo("develop");
		Commit actualCommit = (Commit) actual.get("commit");
		assertThat(actualCommit).isNotNull();
		assertThat(actualCommit.getId()).isEqualTo("e02a4f3");
		assertThat(actualCommit.getTime()).isNull();
	}

	@Test
	public void provide_DoesNotExists_NullReturned() throws Exception {
		Resource resource = mock(Resource.class);
		given(resource.exists()).willReturn(false);
		ScmGitPropertiesInfoProvider scmGitPropertiesInfoProvider = new ScmGitPropertiesInfoProvider(resource);

		Info actual = scmGitPropertiesInfoProvider.provide();
		assertThat(actual).isNull();
	}

}
