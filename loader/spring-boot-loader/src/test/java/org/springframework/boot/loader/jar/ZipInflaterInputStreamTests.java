/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.loader.jar;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ZipInflaterInputStream}.
 *
 * @author Ganesh Tiwari
 */
class ZipInflaterInputStreamTests {

	@Test
	void bufferIsSizedFromCompressedSize() throws IOException {
		byte[] content = new byte[32 * 1024];
		byte[] compressed = deflate(content);
		RecordingInputStream source = new RecordingInputStream(compressed);
		try (ZipInflaterInputStream input = new ZipInflaterInputStream(source, new Inflater(true), content.length,
				compressed.length)) {
			assertThat(input.readAllBytes()).isEqualTo(content);
		}
		assertThat(source.requestedLengths).first().isEqualTo(compressed.length + 2);
	}

	@Test
	void bufferIsSizedFromCompressedSizeWhenUncompressedSizeIsLarge() throws IOException {
		byte[] content = new byte[256 * 1024];
		byte[] compressed = deflate(content);
		assertThat(compressed.length).isLessThan(65536);
		RecordingInputStream source = new RecordingInputStream(compressed);
		try (ZipInflaterInputStream input = new ZipInflaterInputStream(source, new Inflater(true), content.length,
				compressed.length)) {
			assertThat(input.readAllBytes()).isEqualTo(content);
		}
		assertThat(source.requestedLengths).first().isEqualTo(compressed.length + 2);
	}

	@Test
	void bufferIsLimitedWhenCompressedSizeIsLarge() throws IOException {
		byte[] content = new byte[128 * 1024];
		new Random(0).nextBytes(content);
		byte[] compressed = deflate(content);
		assertThat(compressed.length).isGreaterThan(65536);
		RecordingInputStream source = new RecordingInputStream(compressed);
		try (ZipInflaterInputStream input = new ZipInflaterInputStream(source, new Inflater(true), content.length,
				compressed.length)) {
			assertThat(input.readAllBytes()).isEqualTo(content);
			assertThat(input.available()).isZero();
		}
		assertThat(source.requestedLengths).allMatch((length) -> length == 8192);
	}

	@Test
	void bufferIsSizedFromUncompressedSizeWhenCompressedSizeIsNotProvided() throws IOException {
		byte[] content = new byte[1024];
		byte[] compressed = deflate(content);
		RecordingInputStream source = new RecordingInputStream(compressed);
		try (ZipInflaterInputStream input = new ZipInflaterInputStream(source, new Inflater(true), content.length)) {
			assertThat(input.readAllBytes()).isEqualTo(content);
		}
		assertThat(source.requestedLengths).first().isEqualTo(content.length + 2);
	}

	@Test
	void availableTracksRemainingUncompressedBytes() throws IOException {
		byte[] content = new byte[10 * 1024];
		byte[] compressed = deflate(content);
		try (ZipInflaterInputStream input = new ZipInflaterInputStream(new ByteArrayInputStream(compressed),
				new Inflater(true), content.length, compressed.length)) {
			assertThat(input.available()).isEqualTo(content.length);
			assertThat(input.readNBytes(1000)).hasSize(1000);
			assertThat(input.available()).isEqualTo(content.length - 1000);
			input.readAllBytes();
			assertThat(input.available()).isZero();
		}
	}

	private static byte[] deflate(byte[] content) {
		Deflater deflater = new Deflater(Deflater.DEFAULT_COMPRESSION, true);
		try {
			deflater.setInput(content);
			deflater.finish();
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			byte[] buffer = new byte[4096];
			while (!deflater.finished()) {
				out.write(buffer, 0, deflater.deflate(buffer));
			}
			return out.toByteArray();
		}
		finally {
			deflater.end();
		}
	}

	/**
	 * {@link InputStream} that records the length requested by each bulk read.
	 */
	static class RecordingInputStream extends ByteArrayInputStream {

		final List<Integer> requestedLengths = new ArrayList<>();

		RecordingInputStream(byte[] bytes) {
			super(bytes);
		}

		@Override
		public synchronized int read(byte[] b, int off, int len) {
			this.requestedLengths.add(len);
			return super.read(b, off, len);
		}

	}

}
