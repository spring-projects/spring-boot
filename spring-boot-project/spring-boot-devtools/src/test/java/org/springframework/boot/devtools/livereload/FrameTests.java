/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.devtools.livereload;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link Frame}.
 *
 * @author Phillip Webb
 */
public class FrameTests {

	@Test
	public void payloadMustNotBeNull() {
		assertThatIllegalArgumentException().isThrownBy(() -> new Frame((String) null))
				.withMessageContaining("Payload must not be null");
	}

	@Test
	public void typeMustNotBeNull() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new Frame((Frame.Type) null))
				.withMessageContaining("Type must not be null");
	}

	@Test
	public void textPayload() {
		Frame frame = new Frame("abc");
		assertThat(frame.getType()).isEqualTo(Frame.Type.TEXT);
		assertThat(frame.getPayload()).isEqualTo("abc".getBytes());
	}

	@Test
	public void typedPayload() {
		Frame frame = new Frame(Frame.Type.CLOSE);
		assertThat(frame.getType()).isEqualTo(Frame.Type.CLOSE);
		assertThat(frame.getPayload()).isEqualTo(new byte[] {});
	}

	@Test
	public void writeSmallPayload() throws Exception {
		String payload = createString(1);
		Frame frame = new Frame(payload);
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		frame.write(bos);
		assertThat(bos.toByteArray()).isEqualTo(new byte[] { (byte) 0x81, 0x01, 0x41 });
	}

	@Test
	public void writeLargePayload() throws Exception {
		String payload = createString(126);
		Frame frame = new Frame(payload);
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		frame.write(bos);
		byte[] bytes = bos.toByteArray();
		assertThat(bytes.length).isEqualTo(130);
		assertThat(bytes[0]).isEqualTo((byte) 0x81);
		assertThat(bytes[1]).isEqualTo((byte) 0x7E);
		assertThat(bytes[2]).isEqualTo((byte) 0x00);
		assertThat(bytes[3]).isEqualTo((byte) 126);
		assertThat(bytes[4]).isEqualTo((byte) 0x41);
		assertThat(bytes[5]).isEqualTo((byte) 0x41);
	}

	@Test
	public void readFragmentedNotSupported() throws Exception {
		byte[] bytes = new byte[] { 0x0F };
		assertThatIllegalStateException()
				.isThrownBy(() -> Frame.read(newConnectionInputStream(bytes)))
				.withMessageContaining("Fragmented frames are not supported");
	}

	@Test
	public void readLargeFramesNotSupported() throws Exception {
		byte[] bytes = new byte[] { (byte) 0x80, (byte) 0xFF };
		assertThatIllegalStateException()
				.isThrownBy(() -> Frame.read(newConnectionInputStream(bytes)))
				.withMessageContaining("Large frames are not supported");
	}

	@Test
	public void readSmallTextFrame() throws Exception {
		byte[] bytes = new byte[] { (byte) 0x81, (byte) 0x02, 0x41, 0x41 };
		Frame frame = Frame.read(newConnectionInputStream(bytes));
		assertThat(frame.getType()).isEqualTo(Frame.Type.TEXT);
		assertThat(frame.getPayload()).isEqualTo(new byte[] { 0x41, 0x41 });
	}

	@Test
	public void readMaskedTextFrame() throws Exception {
		byte[] bytes = new byte[] { (byte) 0x81, (byte) 0x82, 0x0F, 0x0F, 0x0F, 0x0F,
				0x4E, 0x4E };
		Frame frame = Frame.read(newConnectionInputStream(bytes));
		assertThat(frame.getType()).isEqualTo(Frame.Type.TEXT);
		assertThat(frame.getPayload()).isEqualTo(new byte[] { 0x41, 0x41 });
	}

	@Test
	public void readLargeTextFrame() throws Exception {
		byte[] bytes = new byte[134];
		Arrays.fill(bytes, (byte) 0x4E);
		bytes[0] = (byte) 0x81;
		bytes[1] = (byte) 0xFE;
		bytes[2] = 0x00;
		bytes[3] = 126;
		bytes[4] = 0x0F;
		bytes[5] = 0x0F;
		bytes[6] = 0x0F;
		bytes[7] = 0x0F;
		Frame frame = Frame.read(newConnectionInputStream(bytes));
		assertThat(frame.getType()).isEqualTo(Frame.Type.TEXT);
		assertThat(frame.getPayload()).isEqualTo(createString(126).getBytes());
	}

	@Test
	public void readContinuation() throws Exception {
		byte[] bytes = new byte[] { (byte) 0x80, (byte) 0x00 };
		Frame frame = Frame.read(newConnectionInputStream(bytes));
		assertThat(frame.getType()).isEqualTo(Frame.Type.CONTINUATION);
	}

	@Test
	public void readBinary() throws Exception {
		byte[] bytes = new byte[] { (byte) 0x82, (byte) 0x00 };
		Frame frame = Frame.read(newConnectionInputStream(bytes));
		assertThat(frame.getType()).isEqualTo(Frame.Type.BINARY);
	}

	@Test
	public void readClose() throws Exception {
		byte[] bytes = new byte[] { (byte) 0x88, (byte) 0x00 };
		Frame frame = Frame.read(newConnectionInputStream(bytes));
		assertThat(frame.getType()).isEqualTo(Frame.Type.CLOSE);
	}

	@Test
	public void readPing() throws Exception {
		byte[] bytes = new byte[] { (byte) 0x89, (byte) 0x00 };
		Frame frame = Frame.read(newConnectionInputStream(bytes));
		assertThat(frame.getType()).isEqualTo(Frame.Type.PING);
	}

	@Test
	public void readPong() throws Exception {
		byte[] bytes = new byte[] { (byte) 0x8A, (byte) 0x00 };
		Frame frame = Frame.read(newConnectionInputStream(bytes));
		assertThat(frame.getType()).isEqualTo(Frame.Type.PONG);
	}

	private ConnectionInputStream newConnectionInputStream(byte[] bytes) {
		return new ConnectionInputStream(new ByteArrayInputStream(bytes));
	}

	private String createString(int length) {
		char[] chars = new char[length];
		Arrays.fill(chars, 'A');
		return new String(chars);
	}

}
