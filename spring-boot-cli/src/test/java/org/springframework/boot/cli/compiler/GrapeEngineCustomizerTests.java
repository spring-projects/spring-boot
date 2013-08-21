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

import groovy.grape.GrapeIvy;
import groovy.grape.IvyGrabRecord;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.apache.ivy.core.module.id.ModuleId;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.report.ResolveReport;
import org.apache.ivy.plugins.resolver.ChainResolver;
import org.apache.ivy.plugins.resolver.IBiblioResolver;
import org.apache.ivy.util.FileUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Dave Syer
 */
public class GrapeEngineCustomizerTests {

	@Rule
	public ExpectedException expected = ExpectedException.none();
	private String level;

	@Before
	public void setup() {
		this.level = System.getProperty("ivy.message.logger.level");
		System.setProperty("ivy.message.logger.level", "3");
		System.setProperty("disableSpringSnapshotRepos", "true");
	}

	@After
	public void shutdown() {
		if (this.level == null) {
			System.clearProperty("ivy.message.logger.level");
		}
		else {
			System.setProperty("ivy.message.logger.level", this.level);
		}
	}

	@Test
	public void vanillaEngineWithPomExistsAndJarDoesToo() throws Exception {
		GrapeIvy engine = new GrapeIvy();
		prepareFoo(engine, true);
		ResolveReport report = resolveFoo(engine, "foo", "foo", "1.0");
		assertFalse(report.hasError());
	}

	@Test
	public void vanillaEngineWithPomExistsButJarDoesNot() throws Exception {
		GrapeIvy engine = new GrapeIvy();
		prepareFoo(engine, false);
		this.expected.expectMessage("Error grabbing Grapes");
		ResolveReport report = resolveFoo(engine, "foo", "foo", "1.0");
		assertTrue(report.hasError());
	}

	@SuppressWarnings("unchecked")
	@Test
	public void customizedEngineWithPomExistsButJarCanBeResolved() throws Exception {

		GrapeIvy engine = new GrapeIvy();
		GrapeEngineCustomizer customizer = new GrapeEngineCustomizer(engine);
		ChainResolver grapesResolver = (ChainResolver) engine.getSettings().getResolver(
				"downloadGrapes");

		// Add a resolver that will actually resolve the artifact
		IBiblioResolver resolver = new IBiblioResolver();
		resolver.setName("target");
		resolver.setRoot("file:" + System.getProperty("user.dir") + "/target/repository");
		resolver.setM2compatible(true);
		resolver.setSettings(engine.getSettings());
		grapesResolver.getResolvers().add(resolver);

		// Allow resolvers to be customized
		customizer.customize();
		prepareFoo(engine, false);
		prepareFoo(engine, "target/repository/foo/foo/1.0", true);
		ResolveReport report = resolveFoo(engine, "foo", "foo", "1.0");
		assertFalse(report.hasError());

	}

	@Test
	public void customizedEngineWithPomExistsButJarCannotBeResolved() throws Exception {

		GrapeIvy engine = new GrapeIvy();
		GrapeEngineCustomizer customizer = new GrapeEngineCustomizer(engine);

		// Allow resolvers to be customized
		customizer.customize();
		prepareFoo(engine, false);
		this.expected.expectMessage("Error grabbing Grapes");
		ResolveReport report = resolveFoo(engine, "foo", "foo", "1.0");
		assertFalse(report.hasError());

	}

	private ResolveReport resolveFoo(GrapeIvy engine, String group, String artifact,
			String version) {
		Map<String, Object> args = new HashMap<String, Object>();
		args.put("autoDownload", true);
		IvyGrabRecord record = new IvyGrabRecord();
		record.setConf(Arrays.asList("default"));
		record.setForce(true);
		record.setTransitive(true);
		record.setExt("");
		record.setType("");
		record.setMrid(new ModuleRevisionId(new ModuleId(group, artifact), version));
		ResolveReport report = engine.getDependencies(args, record);
		return report;
	}

	private void prepareFoo(GrapeIvy engine, boolean includeJar) throws IOException {
		prepareFoo(engine, System.getProperty("user.home")
				+ "/.m2/repository/foo/foo/1.0", includeJar);
	}

	private void prepareFoo(GrapeIvy engine, String root, boolean includeJar)
			throws IOException {
		File maven = new File(root);
		FileUtil.forceDelete(maven);
		FileUtil.copy(new File("src/test/resources/foo.pom"), new File(maven,
				"foo-1.0.pom"), null);
		if (includeJar) {
			FileUtil.copy(new File("src/test/resources/foo.jar"), new File(maven,
					"foo-1.0.jar"), null);
		}
		File ivy = new File(engine.getGrapeCacheDir() + "/foo");
		FileUtil.forceDelete(ivy);
	}

}
