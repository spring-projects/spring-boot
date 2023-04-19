/*
 * Copyright 2023-2023 the original author or authors.
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

package org.springframework.boot.build;

import java.util.ArrayList;
import java.util.List;

import org.gradle.api.JavaVersion;
import org.gradle.api.Project;
import org.gradle.api.internal.IConventionAware;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.plugins.ide.eclipse.EclipseWtpPlugin;
import org.gradle.plugins.ide.eclipse.model.EclipseModel;
import org.gradle.plugins.ide.eclipse.model.Facet;

/**
 * Conventions that are applied in the presence of the {WarPlugin}. When the plugin is
 * applied:
 * <ul>
 * <li>Update Eclipse WTP Plugin facets to use Servlet 5.0</li>
 * </ul>
 *
 * @author Phillip Webb
 */
public class WarConventions {

	void apply(Project project) {
		project.getPlugins().withType(EclipseWtpPlugin.class, (wtp) -> {
			project.getTasks().getByName(EclipseWtpPlugin.ECLIPSE_WTP_FACET_TASK_NAME).doFirst((task) -> {
				EclipseModel eclipseModel = project.getExtensions().getByType(EclipseModel.class);
				((IConventionAware) eclipseModel.getWtp().getFacet()).getConventionMapping()
					.map("facets", () -> getFacets(project));
			});
		});
	}

	private List<Facet> getFacets(Project project) {
		JavaVersion javaVersion = project.getExtensions().getByType(JavaPluginExtension.class).getSourceCompatibility();
		List<Facet> facets = new ArrayList<>();
		facets.add(new Facet(Facet.FacetType.fixed, "jst.web", null));
		facets.add(new Facet(Facet.FacetType.installed, "jst.web", "5.0"));
		facets.add(new Facet(Facet.FacetType.installed, "jst.java", javaVersion.toString()));
		return facets;
	}

}
