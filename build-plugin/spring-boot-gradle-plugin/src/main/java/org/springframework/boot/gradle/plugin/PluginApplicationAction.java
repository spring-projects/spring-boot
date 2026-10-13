/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.gradle.plugin;

import org.gradle.api.Action;
import org.gradle.api.Plugin;
import org.gradle.api.Project;

/**
 * An {@link Action} to be executed on a {@link Project} in response to a particular type
 * of {@link Plugin} being applied.
 *
 * @author Andy Wilkinson
 */
interface PluginApplicationAction extends Action<Project> {

	/**
	 * The class of the {@code Plugin} that, when applied, will trigger the execution of
	 * this action.
	 * @return the plugin class
	 * @throws ClassNotFoundException if the plugin class cannot be found
	 * @throws NoClassDefFoundError if an error occurs when defining the plugin class
	 */
	Class<? extends Plugin<? extends Project>> getPluginClass() throws ClassNotFoundException, NoClassDefFoundError;

}
