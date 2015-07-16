package org.springframework.boot.actuate.info;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.springframework.boot.actuate.info.ScmGitPropertiesInfoProvider.GitInfo.Commit;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;

public class ScmGitPropertiesInfoProviderTest {

	
	@Test
	public void provide_HasBadFormatButExists_EmptyInfoReturned() throws Exception {
		Resource resource = new ByteArrayResource("GARBAGE".getBytes());
		ScmGitPropertiesInfoProvider scmGitPropertiesInfoProvider = new ScmGitPropertiesInfoProvider(resource);

		Info actual = scmGitPropertiesInfoProvider.provide();
        assertThat(actual, is(not(nullValue())));
        assertThat((String) actual.get("branch"), is(nullValue()));
        Commit actualCommit = (Commit) actual.get("commit");
		assertThat(actualCommit, is(not(nullValue())));
		assertThat(actualCommit.getId(), is(equalTo("")));
		assertThat(actualCommit.getTime(), is(nullValue()));
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
        assertThat(actual, is(not(nullValue())));
        assertThat((String) actual.get("branch"), is(equalTo("develop")));
        Commit actualCommit = (Commit) actual.get("commit");
		assertThat(actualCommit, is(not(nullValue())));
		assertThat(actualCommit.getId(), is(equalTo("e02a4f3")));
		assertThat(actualCommit.getTime(), is(equalTo("2013-04-24T08:42:13+0100")));
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
        assertThat(actual, is(not(nullValue())));
        assertThat((String) actual.get("branch"), is(equalTo("develop")));
        Commit actualCommit = (Commit) actual.get("commit");
		assertThat(actualCommit, is(not(nullValue())));
		assertThat(actualCommit.getId(), is(equalTo("e02a4f3")));
		assertThat(actualCommit.getTime(), is(nullValue()));
	}

	
	@Test
	public void provide_DoesNotExists_NullReturned() throws Exception {
		
		Resource resource = mock(Resource.class);
		when(resource.exists())
			.thenReturn(false);
		ScmGitPropertiesInfoProvider scmGitPropertiesInfoProvider = new ScmGitPropertiesInfoProvider(resource);

		Info actual = scmGitPropertiesInfoProvider.provide();
        assertThat(actual, is(nullValue()));
	}

}
