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

package org.springframework.boot.aether;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.repository.Authentication;
import org.eclipse.aether.repository.RemoteRepository;
import org.junit.Test;

import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link AetherEngine}.
 *
 * @author Andy Wilkinson
 */
public class AetherEngineTests {

	private AetherEngine createEngine(RepositoryConfiguration... additionalRepositories) {
		List<RepositoryConfiguration> repositoryConfigurations = new ArrayList<RepositoryConfiguration>();
		repositoryConfigurations.add(new RepositoryConfiguration("central",
				URI.create("http://repo1.maven.org/maven2"), false));
		repositoryConfigurations.addAll(Arrays.asList(additionalRepositories));
		return AetherEngine.create(repositoryConfigurations,
				new DependencyManagementContext());
	}

	@Test
	public void proxySelector() {
		doWithCustomUserHome(new Runnable() {

			@Override
			public void run() {
				AetherEngine grapeEngine = createEngine();

				DefaultRepositorySystemSession session = (DefaultRepositorySystemSession) ReflectionTestUtils
						.getField(grapeEngine, "session");

				assertThat(session.getProxySelector() instanceof CompositeProxySelector)
						.isTrue();
			}

		});
	}

	@Test
	public void repositoryMirrors() {
		doWithCustomUserHome(new Runnable() {

			@SuppressWarnings("unchecked")
			@Override
			public void run() {
				AetherEngine grapeEngine = createEngine();

				List<RemoteRepository> repositories = (List<RemoteRepository>) ReflectionTestUtils
						.getField(grapeEngine, "repositories");
				assertThat(repositories).hasSize(1);
				assertThat(repositories.get(0).getId()).isEqualTo("central-mirror");
			}
		});
	}

	@Test
	public void repositoryAuthentication() {
		doWithCustomUserHome(new Runnable() {

			@SuppressWarnings("unchecked")
			@Override
			public void run() {
				AetherEngine grapeEngine = createEngine();

				List<RemoteRepository> repositories = (List<RemoteRepository>) ReflectionTestUtils
						.getField(grapeEngine, "repositories");
				assertThat(repositories).hasSize(1);
				Authentication authentication = repositories.get(0).getAuthentication();
				assertThat(authentication).isNotNull();
			}
		});
	}

	private void doWithCustomUserHome(Runnable action) {
		doWithSystemProperty("user.home",
				new File("src/test/resources").getAbsolutePath(), action);
	}

	private void doWithSystemProperty(String key, String value, Runnable action) {
		String previousValue = setOrClearSystemProperty(key, value);
		try {
			action.run();
		}
		finally {
			setOrClearSystemProperty(key, previousValue);
		}
	}

	private String setOrClearSystemProperty(String key, String value) {
		if (value != null) {
			return System.setProperty(key, value);
		}
		return System.clearProperty(key);
	}
}
