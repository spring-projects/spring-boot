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

package org.springframework.boot.cli.compiler.grape;

import java.io.File;
import java.util.Arrays;

import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.LocalRepositoryManager;
import org.eclipse.aether.repository.ProxySelector;
import org.eclipse.aether.util.repository.JreProxySelector;

import org.springframework.util.StringUtils;

/**
 * A {@link RepositorySystemSessionAutoConfiguration} that, in the absence of any
 * configuration, applies sensible defaults.
 *
 * @author Andy Wilkinson
 */
public class DefaultRepositorySystemSessionAutoConfiguration implements RepositorySystemSessionAutoConfiguration {

	@Override
	public void apply(DefaultRepositorySystemSession session, RepositorySystem repositorySystem) {

		if (session.getLocalRepositoryManager() == null) {
			LocalRepository localRepository = new LocalRepository(getM2RepoDirectory());
			LocalRepositoryManager localRepositoryManager = repositorySystem.newLocalRepositoryManager(session,
					localRepository);
			session.setLocalRepositoryManager(localRepositoryManager);
		}

		ProxySelector existing = session.getProxySelector();
		if (existing == null || !(existing instanceof CompositeProxySelector)) {
			JreProxySelector fallback = new JreProxySelector();
			ProxySelector selector = (existing != null) ? new CompositeProxySelector(Arrays.asList(existing, fallback))
					: fallback;
			session.setProxySelector(selector);
		}
	}

	private File getM2RepoDirectory() {
		return new File(getDefaultM2HomeDirectory(), "repository");
	}

	private File getDefaultM2HomeDirectory() {
		String mavenRoot = System.getProperty("maven.home");
		if (StringUtils.hasLength(mavenRoot)) {
			return new File(mavenRoot);
		}
		return new File(System.getProperty("user.home"), ".m2");
	}

}
