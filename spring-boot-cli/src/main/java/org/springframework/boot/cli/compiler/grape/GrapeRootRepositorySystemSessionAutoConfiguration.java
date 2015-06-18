/*
 * Copyright 2012-2015 the original author or authors.
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

import java.io.File;

import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.LocalRepositoryManager;
import org.springframework.util.StringUtils;

/**
 * Honours the configuration of {@code grape.root} by customizing the session's local
 * repository location.
 *
 * @author Andy Wilkinson
 * @since 1.2.5
 */
public class GrapeRootRepositorySystemSessionAutoConfiguration implements
		RepositorySystemSessionAutoConfiguration {

	@Override
	public void apply(DefaultRepositorySystemSession session,
			RepositorySystem repositorySystem) {
		String grapeRoot = System.getProperty("grape.root");
		if (StringUtils.hasLength(grapeRoot)) {
			configureLocalRepository(session, repositorySystem, grapeRoot);
		}
	}

	private void configureLocalRepository(DefaultRepositorySystemSession session,
			RepositorySystem repositorySystem, String grapeRoot) {
		File repositoryDir = new File(grapeRoot, "repository");
		LocalRepository localRepository = new LocalRepository(repositoryDir);
		LocalRepositoryManager localRepositoryManager = repositorySystem
				.newLocalRepositoryManager(session, localRepository);
		session.setLocalRepositoryManager(localRepositoryManager);
	}

}
