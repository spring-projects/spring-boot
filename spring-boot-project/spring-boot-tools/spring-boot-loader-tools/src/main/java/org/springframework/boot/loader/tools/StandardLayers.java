/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.loader.tools;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

/**
 * Base class for the standard set of {@link Layers}. Defines the following layers:
 * <ol>
 * <li>"dependencies" - For non snapshot dependencies</li>
 * <li>"spring-boot-loader" - For classes from {@code spring-boot-loader} used to launch a
 * fat jar</li>
 * <li>"snapshot-dependencies" - For snapshot dependencies</li>
 * <li>"application" - For application classes and resources</li>
 * </ol>
 *
 * @author Madhura Bhave
 * @author Phillip Webb
 * @since 2.3.0
 */
public abstract class StandardLayers implements Layers {

	/**
	 * The dependencies layer.
	 */
	public static final Layer DEPENDENCIES = new Layer("dependencies");

	/**
	 * The spring boot loader layer.
	 */
	public static final Layer SPRING_BOOT_LOADER = new Layer("spring-boot-loader");

	/**
	 * The snapshot dependencies layer.
	 */
	public static final Layer SNAPSHOT_DEPENDENCIES = new Layer("snapshot-dependencies");

	/**
	 * The application layer.
	 */
	public static final Layer APPLICATION = new Layer("application");

	private static final List<Layer> LAYERS;
	static {
		List<Layer> layers = new ArrayList<>();
		layers.add(DEPENDENCIES);
		layers.add(SPRING_BOOT_LOADER);
		layers.add(SNAPSHOT_DEPENDENCIES);
		layers.add(APPLICATION);
		LAYERS = Collections.unmodifiableList(layers);
	}

	@Override
	public Iterator<Layer> iterator() {
		return LAYERS.iterator();
	}

	@Override
	public Stream<Layer> stream() {
		return LAYERS.stream();
	}

}
