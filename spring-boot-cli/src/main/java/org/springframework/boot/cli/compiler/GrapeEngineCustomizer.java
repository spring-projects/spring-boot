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

import java.util.concurrent.TimeUnit;

import org.apache.ivy.Ivy;
import org.apache.ivy.core.cache.ArtifactOrigin;
import org.apache.ivy.core.event.IvyEvent;
import org.apache.ivy.core.event.IvyListener;
import org.apache.ivy.core.event.resolve.EndResolveEvent;
import org.apache.ivy.core.module.descriptor.Artifact;
import org.apache.ivy.core.module.id.ArtifactId;
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

	@SuppressWarnings("unchecked")
	public void customize() {
		Ivy ivy = this.engine.getIvyInstance();
		IvySettings settings = this.engine.getSettings();

		final DownloadingLog downloadingLog = new DownloadingLog();

		ivy.getLoggerEngine().pushLogger(downloadingLog);
		ChainResolver resolver = (ChainResolver) settings.getResolver("downloadGrapes");

		// Add an early resolver for spring snapshots that doesn't try to locate
		// anything non-spring
		ChainResolver earlySpringResolver = new ChainResolver() {
			@Override
			public ArtifactOrigin locate(Artifact artifact) {
				try {
					ArtifactId artifactId = artifact.getId().getArtifactId();
					ModuleId moduleId = artifactId.getModuleId();
					if (moduleId.getOrganisation().startsWith("org.springframework")) {
						return super.locate(artifact);
					}
				}
				catch (Exception ex) {
					return null;
				}
				return null;
			}
		};
		earlySpringResolver.setSettings(settings);
		earlySpringResolver.setReturnFirst(true);
		addSpringResolvers(earlySpringResolver);
		resolver.getResolvers().add(0, earlySpringResolver);

		// Add spring resolvers again, but this time without any filtering
		addSpringResolvers(resolver);
		ivy.getEventManager().addIvyListener(new IvyListener() {
			@Override
			public void progress(IvyEvent event) {
				if (event instanceof EndResolveEvent) {
					downloadingLog.finished();
				}
			}
		});
	}

	private void addSpringResolvers(ChainResolver chain) {
		chain.add(newResolver("spring-snapshot", "http://repo.springsource.org/snapshot"));
		chain.add(newResolver("spring-milestone",
				"http://repo.springsource.org/milestone"));
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
			if (!this.finished && System.currentTimeMillis() - this.startTime > INITIAL_DELAY) {
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
