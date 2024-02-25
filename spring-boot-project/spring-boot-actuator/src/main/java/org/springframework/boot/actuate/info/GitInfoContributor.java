/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot.actuate.info;

import java.time.Instant;
import java.util.Map;
import java.util.Properties;

import org.springframework.aot.hint.BindingReflectionHintsRegistrar;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.boot.actuate.info.GitInfoContributor.GitInfoContributorRuntimeHints;
import org.springframework.boot.info.GitProperties;
import org.springframework.context.annotation.ImportRuntimeHints;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.env.PropertySource;

/**
 * An {@link InfoContributor} that exposes {@link GitProperties}.
 *
 * @author Stephane Nicoll
 * @since 1.4.0
 */
@ImportRuntimeHints(GitInfoContributorRuntimeHints.class)
public class GitInfoContributor extends InfoPropertiesInfoContributor<GitProperties> {

	/**
     * Constructs a new GitInfoContributor with the specified GitProperties and default mode.
     *
     * @param properties the GitProperties object containing the Git information
     */
    public GitInfoContributor(GitProperties properties) {
		this(properties, Mode.SIMPLE);
	}

	/**
     * Constructs a new GitInfoContributor with the specified GitProperties and Mode.
     * 
     * @param properties the GitProperties object to use
     * @param mode the Mode object to use
     */
    public GitInfoContributor(GitProperties properties, Mode mode) {
		super(properties, mode);
	}

	/**
     * Contributes Git information to the provided Info.Builder object.
     * 
     * @param builder the Info.Builder object to which the Git information is contributed
     */
    @Override
	public void contribute(Info.Builder builder) {
		builder.withDetail("git", generateContent());
	}

	/**
     * Converts the GitInfoContributor's properties to a simple PropertySource.
     * 
     * @return the converted PropertySource
     */
    @Override
	protected PropertySource<?> toSimplePropertySource() {
		Properties props = new Properties();
		copyIfSet(props, "branch");
		String commitId = getProperties().getShortCommitId();
		if (commitId != null) {
			props.put("commit.id", commitId);
		}
		copyIfSet(props, "commit.time");
		return new PropertiesPropertySource("git", props);
	}

	/**
	 * Post-process the content to expose. By default, well known keys representing dates
	 * are converted to {@link Instant} instances.
	 * @param content the content to expose
	 */
	@Override
	protected void postProcessContent(Map<String, Object> content) {
		replaceValue(getNestedMap(content, "commit"), "time", getProperties().getCommitTime());
		replaceValue(getNestedMap(content, "build"), "time", getProperties().getInstant("build.time"));
	}

	/**
     * GitInfoContributorRuntimeHints class.
     */
    static class GitInfoContributorRuntimeHints implements RuntimeHintsRegistrar {

		private final BindingReflectionHintsRegistrar bindingRegistrar = new BindingReflectionHintsRegistrar();

		/**
         * Registers the runtime hints for GitInfoContributor.
         * 
         * @param hints the runtime hints to be registered
         * @param classLoader the class loader to be used for reflection
         */
        @Override
		public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
			this.bindingRegistrar.registerReflectionHints(hints.reflection(), GitProperties.class);
		}

	}

}
