package org.springframework.boot.cli.compiler.grape;

import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.apache.maven.settings.building.SettingsBuildingException;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.repository.LocalRepositoryManager;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.boot.cli.compiler.grape.SettingsXmlRepositorySystemSessionAutoConfiguration;

import static org.junit.Assert.assertNotNull;

@RunWith(MockitoJUnitRunner.class)
public class SettingsXmlRepositorySystemSessionAutoConfigurationTests {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Mock
	private RepositorySystem repositorySystem;

	@Mock
	LocalRepositoryManager localRepositoryManager;

	@Test
	public void basicSessionCustomization() throws SettingsBuildingException {
		DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();

		new SettingsXmlRepositorySystemSessionAutoConfiguration().apply(session,
				this.repositorySystem);

		assertNotNull(session.getMirrorSelector());
		assertNotNull(session.getProxySelector());
	}
}
