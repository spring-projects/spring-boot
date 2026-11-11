/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.jarmode.tools;

import java.util.Iterator;
import java.util.zip.ZipEntry;

import org.springframework.boot.loader.jarmode.JarModeErrorException;

/**
 * Provides information about the jar layers.
 *
 * @author Phillip Webb
 * @author Moritz Halbritter
 * @see ExtractCommand
 * @see ListCommand
 */
interface Layers extends Iterable<String> {

	/**
	 * Return the jar layers in the order that they should be added (starting with the
	 * least frequently changed layer).
	 */
	@Override
	Iterator<String> iterator();

	/**
	 * Return the layer that a given entry is in.
	 * @param entry the entry to check
	 * @return the layer that the entry is in
	 */
	default String getLayer(ZipEntry entry) {
		return getLayer(entry.getName());
	}

	/**
	 * Return the layer that the entry with the given name is in.
	 * @param entryName the name of the entry to check
	 * @return the layer that the entry is in
	 */
	String getLayer(String entryName);

	/**
	 * Return the name of the application layer.
	 * @return the name of the application layer
	 */
	String getApplicationLayerName();

	/**
	 * Return a {@link Layers} instance for the currently running application.
	 * @param context the command context
	 * @return a new layers instance
	 */
	static Layers get(Context context) {
		IndexedLayers indexedLayers = IndexedLayers.get(context);
		if (indexedLayers == null) {
			throw new JarModeErrorException("Layers are not enabled");
		}
		return indexedLayers;
	}

}
