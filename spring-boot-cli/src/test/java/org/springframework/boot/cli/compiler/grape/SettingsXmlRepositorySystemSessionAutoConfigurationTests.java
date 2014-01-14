/*
 * Copyright 2012-2014 the original author or authors.
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

import static org.junit.Assert.assertNotNull;

/**
 * Tests for {@link SettingsXmlRepositorySystemSessionAutoConfiguration}.
 * 
 * @author Andy Wilkinson
 */
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
