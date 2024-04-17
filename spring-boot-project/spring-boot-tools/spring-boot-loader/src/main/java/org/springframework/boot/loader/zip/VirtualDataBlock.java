/*
 * Copyright 2012-2024 the original author or authors.
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

package org.springframework.boot.loader.zip;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Collection;

/**
 * A virtual {@link DataBlock} build from a collection of other {@link DataBlock}
 * instances.
 *
 * @author Phillip Webb
 */
class VirtualDataBlock implements DataBlock {

	private DataBlock[] parts;

	private long[] offsets;

	private long size;

	private volatile int lastReadPart = 0;

	/**
	 * Create a new {@link VirtualDataBlock} instance. The {@link #setParts(Collection)}
	 * method must be called before the data block can be used.
	 */
	protected VirtualDataBlock() {
	}

	/**
	 * Create a new {@link VirtualDataBlock} backed by the given parts.
	 * @param parts the parts that make up the virtual data block
	 * @throws IOException in I/O error
	 */
	VirtualDataBlock(Collection<? extends DataBlock> parts) throws IOException {
		setParts(parts);
	}

	/**
	 * Set the parts that make up the virtual data block.
	 * @param parts the data block parts
	 * @throws IOException on I/O error
	 */
	protected void setParts(Collection<? extends DataBlock> parts) throws IOException {
		this.parts = parts.toArray(DataBlock[]::new);
		this.offsets = new long[parts.size()];
		long size = 0;
		int i = 0;
		for (DataBlock part : parts) {
			this.offsets[i++] = size;
			size += part.size();
		}
		this.size = size;

	}

	@Override
	public long size() throws IOException {
		return this.size;
	}

	@Override
	public int read(ByteBuffer dst, long pos) throws IOException {
		if (pos < 0 || pos >= this.size) {
			return -1;
		}
		int lastReadPart = this.lastReadPart;
		int partIndex = 0;
		long offset = 0;
		int result = 0;
		if (pos >= this.offsets[lastReadPart]) {
			partIndex = lastReadPart;
			offset = this.offsets[lastReadPart];
		}
		while (partIndex < this.parts.length) {
			DataBlock part = this.parts[partIndex];
			while (pos >= offset && pos < offset + part.size()) {
				int count = part.read(dst, pos - offset);
				result += Math.max(count, 0);
				if (count <= 0 || !dst.hasRemaining()) {
					this.lastReadPart = partIndex;
					return result;
				}
				pos += count;
			}
			offset += part.size();
			partIndex++;
		}
		return result;

	}

}
