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

package org.springframework.boot.convert;

import java.io.IOException;

import org.springframework.boot.origin.Origin;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.io.InputStreamSource;
import org.springframework.core.io.Resource;
import org.springframework.util.FileCopyUtils;

/**
 * {@link Converter} to convert from an {@link InputStreamSource} to a {@code byte[]}.
 *
 * @author Phillip Webb
 */
class InputStreamSourceToByteArrayConverter implements Converter<InputStreamSource, byte[]> {

	@Override
	public byte[] convert(InputStreamSource source) {
		try {
			return FileCopyUtils.copyToByteArray(source.getInputStream());
		}
		catch (IOException ex) {
			throw new IllegalStateException("Unable to read from " + getName(source), ex);
		}
	}

	private String getName(InputStreamSource source) {
		Origin origin = Origin.from(source);
		if (origin != null) {
			return origin.toString();
		}
		if (source instanceof Resource resource) {
			return resource.getDescription();
		}
		return "input stream source";
	}

}
