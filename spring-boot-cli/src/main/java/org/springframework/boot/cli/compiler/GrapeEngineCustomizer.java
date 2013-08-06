/*
 * Copyright 2012-2013 the original author or authors.
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

package org.springframework.boot.cli.compiler;

import groovy.grape.GrapeEngine;
import groovy.grape.GrapeIvy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.ivy.Ivy;
import org.apache.ivy.core.cache.ArtifactOrigin;
import org.apache.ivy.core.event.IvyEvent;
import org.apache.ivy.core.event.IvyListener;
import org.apache.ivy.core.event.resolve.EndResolveEvent;
import org.apache.ivy.core.module.descriptor.Artifact;
import org.apache.ivy.core.module.id.ModuleId;
import org.apache.ivy.core.settings.IvySettings;
import org.apache.ivy.plugins.resolver.ChainResolver;
import org.apache.ivy.plugins.resolver.DependencyResolver;
import org.apache.ivy.plugins.resolver.IBiblioResolver;
import org.apache.ivy.util.AbstractMessageLogger;
import org.apache.ivy.util.MessageLogger;
import org.springframework.boot.cli.Log;

/**
 * Customizes the groovy grape engine to download from Spring repos and provide simple log
 * progress feedback.
 * 
 * @author Phillip Webb
 */
class GrapeEngineCustomizer {

	private GrapeIvy engine;

	public GrapeEngineCustomizer(GrapeEngine engine) {
		this.engine = (GrapeIvy) engine;
	}

	public void customize() {
		Ivy ivy = this.engine.getIvyInstance();
		IvySettings settings = this.engine.getSettings();
		addDownloadingLogSupport(ivy);
		setupResolver(settings);
	}

	private void addDownloadingLogSupport(Ivy ivy) {
		final DownloadingLog downloadingLog = new DownloadingLog();
		ivy.getLoggerEngine().pushLogger(downloadingLog);
		ivy.getEventManager().addIvyListener(new IvyListener() {
			@Override
			public void progress(IvyEvent event) {
				if (event instanceof EndResolveEvent) {
					downloadingLog.finished();
				}
			}
		});
	}

	@SuppressWarnings("unchecked")
	private void setupResolver(IvySettings settings) {
		ChainResolver grapesResolver = (ChainResolver) settings
				.getResolver("downloadGrapes");

		SpringBootResolver springBootResolver = new SpringBootResolver(
				grapesResolver.getResolvers());
		springBootResolver.setSettings(settings);
		springBootResolver.setName("springBoot");

		if (!Boolean.getBoolean("disableSpringSnapshotRepos")) {
			springBootResolver.addSpringSnapshotResolver(newResolver("spring-snapshot",
					"http://repo.springsource.org/snapshot"));
			springBootResolver.addSpringSnapshotResolver(newResolver("spring-milestone",
					"http://repo.springsource.org/milestone"));
		}

		grapesResolver.getResolvers().clear();
		grapesResolver.getResolvers().add(springBootResolver);
	}

	private DependencyResolver newResolver(String name, String root) {
		IBiblioResolver resolver = new IBiblioResolver();
		resolver.setName(name);
		resolver.setRoot(root);
		resolver.setM2compatible(true);
		resolver.setSettings(this.engine.getSettings());
		return resolver;
	}

	/**
	 * {@link DependencyResolver} that is optimized for Spring Boot.
	 */
	private static class SpringBootResolver extends ChainResolver {

		private static final Object SPRING_BOOT_GROUP_ID = "org.springframework.boot";

		private static final String STARTER_PREFIX = "spring-boot-starter";

		private static final Object SOURCE_TYPE = "source";

		private static final Object JAVADOC_TYPE = "javadoc";

		private static final Set<String> POM_ONLY_DEPENDENCIES;
		static {
			Set<String> dependencies = new HashSet<String>();
			dependencies.add("spring-boot-dependencies");
			dependencies.add("spring-boot-parent");
			dependencies.add("spring-boot-starters");
			POM_ONLY_DEPENDENCIES = Collections.unmodifiableSet(dependencies);
		}

		private final List<DependencyResolver> springSnapshotResolvers = new ArrayList<DependencyResolver>();

		public SpringBootResolver(List<DependencyResolver> resolvers) {
			for (DependencyResolver resolver : resolvers) {
				add(resolver);
			}
		}

		public void addSpringSnapshotResolver(DependencyResolver resolver) {
			add(resolver);
			this.springSnapshotResolvers.add(resolver);
		}

		@Override
		public ArtifactOrigin locate(Artifact artifact) {
			if (isUnresolvable(artifact)) {
				return null;
			}
			if (isSpringSnapshot(artifact)) {
				for (DependencyResolver resolver : this.springSnapshotResolvers) {
					ArtifactOrigin origin = resolver.locate(artifact);
					if (origin != null) {
						return origin;
					}
				}
			}
			return super.locate(artifact);
		}

		private boolean isUnresolvable(Artifact artifact) {
			try {
				ModuleId moduleId = artifact.getId().getArtifactId().getModuleId();
				if (SPRING_BOOT_GROUP_ID.equals(moduleId.getOrganisation())) {
					// Skip any POM only deps if they are not pom ext
					if (POM_ONLY_DEPENDENCIES.contains(moduleId.getName())
							&& !("pom".equalsIgnoreCase(artifact.getId().getExt()))) {
						return true;
					}

					// Skip starter javadoc and source
					if (moduleId.getName().startsWith(STARTER_PREFIX)
							&& (SOURCE_TYPE.equals(artifact.getType()) || JAVADOC_TYPE
									.equals(artifact.getType()))) {
						return true;
					}
				}
				return false;
			}
			catch (Exception ex) {
				return false;
			}
		}

		private boolean isSpringSnapshot(Artifact artifact) {
			try {
				ModuleId moduleId = artifact.getId().getArtifactId().getModuleId();
				String revision = artifact.getModuleRevisionId().getRevision();
				return (SPRING_BOOT_GROUP_ID.equals(moduleId.getOrganisation()) && (revision
						.endsWith("SNAPSHOT") || revision.contains("M")));
			}
			catch (Exception ex) {
				return false;
			}
		}

	}

	/**
	 * {@link MessageLogger} to provide simple progress information.
	 */
	private static class DownloadingLog extends AbstractMessageLogger {

		private static final long INITIAL_DELAY = TimeUnit.SECONDS.toMillis(3);

		private static final long PROGRESS_DELAY = TimeUnit.SECONDS.toMillis(1);

		private long startTime = System.currentTimeMillis();

		private long lastProgressTime = System.currentTimeMillis();

		private boolean started;

		private boolean finished;

		@Override
		public void log(String msg, int level) {
			logDownloadingMessage();
		}

		@Override
		public void rawlog(String msg, int level) {
		}

		@Override
		protected void doProgress() {
			logDownloadingMessage();
		}

		@Override
		protected void doEndProgress(String msg) {
		}

		private void logDownloadingMessage() {
			if (!this.finished
					&& System.currentTimeMillis() - this.startTime > INITIAL_DELAY) {
				if (!this.started) {
					this.started = true;
					Log.infoPrint("Downloading dependencies..");
					this.lastProgressTime = System.currentTimeMillis();
				}
				else if (System.currentTimeMillis() - this.lastProgressTime > PROGRESS_DELAY) {
					Log.infoPrint(".");
					this.lastProgressTime = System.currentTimeMillis();
				}
			}
		}

		public void finished() {
			if (!this.finished) {
				this.finished = true;
				if (this.started) {
					Log.info("");
				}
			}
		}

	}

}
