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

package org.springframework.boot.context.config;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;

import org.springframework.boot.env.VolumeMountDirectoryPropertySource;

/**
 * {@link ConfigDataLoader} for directory locations mounted as volumes.
 *
 * @author Madhura Bhave
 * @author Phillip Webb
 */
class VolumeMountConfigDataLoader implements ConfigDataLoader<VolumeMountConfigDataLocation> {

	@Override
	public ConfigData load(VolumeMountConfigDataLocation location) throws IOException {
		Path path = location.getPath();
		String name = "Volume mount config '" + path + "'";
		VolumeMountDirectoryPropertySource source = new VolumeMountDirectoryPropertySource(name, path);
		return new ConfigData(Collections.singletonList(source));
	}

}
