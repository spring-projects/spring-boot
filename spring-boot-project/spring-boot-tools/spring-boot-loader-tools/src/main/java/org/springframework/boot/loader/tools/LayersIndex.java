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

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * Index describing the layer to which each entry in a jar belongs.
 *
 * @author Madhura Bhave
 * @author Andy Wilkinson
 * @since 2.3.0
 */
public class LayersIndex {

	private final Iterable<Layer> layers;

	private final MultiValueMap<Layer, String> index = new LinkedMultiValueMap<>();

	/**
	 * Create a new {@link LayersIndex} backed by the given layers.
	 * @param layers the layers in the index
	 */
	public LayersIndex(Iterable<Layer> layers) {
		this.layers = layers;
	}

	/**
	 * Add an item to the index.
	 * @param layer the layer of the item
	 * @param name the name of the item
	 */
	public void add(Layer layer, String name) {
		this.index.add(layer, name);
	}

	/**
	 * Write the layer index to an output stream.
	 * @param out the destination stream
	 * @throws IOException on IO error
	 */
	public void writeTo(OutputStream out) throws IOException {
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8));
		for (Layer layer : this.layers) {
			List<String> names = this.index.get(layer);
			if (names != null) {
				for (String name : names) {
					writer.write(layer.toString());
					writer.write(" ");
					writer.write(name);
					writer.write("\n");
				}
			}
		}
		writer.flush();

	}

}
