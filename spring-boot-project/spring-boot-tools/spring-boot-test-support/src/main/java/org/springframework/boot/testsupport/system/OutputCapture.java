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

package org.springframework.boot.testsupport.system;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

import org.springframework.util.Assert;

/**
 * Provides support for capturing {@link System#out System.out} and {@link System#err
 * System.err}.
 *
 * @author Madhura Bhave
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
class OutputCapture implements CapturedOutput {

	private final Deque<SystemCapture> systemCaptures = new ArrayDeque<>();

	/**
	 * Push a new system capture session onto the stack.
	 */
	final void push() {
		this.systemCaptures.addLast(new SystemCapture());
	}

	/**
	 * Pop the last system capture session from the stack.
	 */
	final void pop() {
		this.systemCaptures.removeLast().release();
	}

	/**
	 * Compares this OutputCapture object with the specified object for equality.
	 * @param obj the object to compare with
	 * @return true if the specified object is equal to this OutputCapture object, false
	 * otherwise
	 */
	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (obj instanceof CharSequence) {
			return getAll().equals(obj.toString());
		}
		return false;
	}

	/**
	 * Returns the hash code value for this OutputCapture object.
	 * @return the hash code value for this object
	 */
	@Override
	public int hashCode() {
		return toString().hashCode();
	}

	/**
	 * Returns a string representation of the OutputCapture object.
	 * @return a string representation of the OutputCapture object
	 */
	@Override
	public String toString() {
		return getAll();
	}

	/**
	 * Return all content (both {@link System#out System.out} and {@link System#err
	 * System.err}) in the order that it was captured.
	 * @return all captured output
	 */
	@Override
	public String getAll() {
		return get((type) -> true);
	}

	/**
	 * Return {@link System#out System.out} content in the order that it was captured.
	 * @return {@link System#out System.out} captured output
	 */
	@Override
	public String getOut() {
		return get(Type.OUT::equals);
	}

	/**
	 * Return {@link System#err System.err} content in the order that it was captured.
	 * @return {@link System#err System.err} captured output
	 */
	@Override
	public String getErr() {
		return get(Type.ERR::equals);
	}

	/**
	 * Resets the current capture session, clearing its captured output.
	 */
	void reset() {
		this.systemCaptures.peek().reset();
	}

	/**
	 * Retrieves the captured output based on the provided filter.
	 * @param filter the filter to apply on the captured output
	 * @return the captured output as a string
	 * @throws IllegalStateException if no system captures are found
	 */
	private String get(Predicate<Type> filter) {
		Assert.state(!this.systemCaptures.isEmpty(),
				"No system captures found. Please check your output capture registration.");
		StringBuilder builder = new StringBuilder();
		for (SystemCapture systemCapture : this.systemCaptures) {
			systemCapture.append(builder, filter);
		}
		return builder.toString();
	}

	/**
	 * A capture session that captures {@link System#out System.out} and {@link System#err
	 * System.err}.
	 */
	private static class SystemCapture {

		private final PrintStreamCapture out;

		private final PrintStreamCapture err;

		private final Object monitor = new Object();

		private final List<CapturedString> capturedStrings = new ArrayList<>();

		/**
		 * Initializes the SystemCapture object by redirecting the standard output and
		 * error streams to custom PrintStreamCapture objects.
		 *
		 * The standard output stream is redirected to a PrintStreamCapture object named
		 * 'out', which captures the output and invokes the 'captureOut' method. The
		 * standard error stream is redirected to a PrintStreamCapture object named 'err',
		 * which captures the output and invokes the 'captureErr' method.
		 *
		 * @see PrintStreamCapture
		 * @see System#setOut(java.io.PrintStream)
		 * @see System#setErr(java.io.PrintStream)
		 */
		SystemCapture() {
			this.out = new PrintStreamCapture(System.out, this::captureOut);
			this.err = new PrintStreamCapture(System.err, this::captureErr);
			System.setOut(this.out);
			System.setErr(this.err);
		}

		/**
		 * Releases the captured output and error streams and restores the original
		 * streams.
		 */
		void release() {
			System.setOut(this.out.getParent());
			System.setErr(this.err.getParent());
		}

		/**
		 * Captures the output string and adds it to the list of captured strings.
		 * @param string the output string to be captured
		 */
		private void captureOut(String string) {
			synchronized (this.monitor) {
				this.capturedStrings.add(new CapturedString(Type.OUT, string));
			}
		}

		/**
		 * Captures an error string and adds it to the list of captured strings.
		 * @param string the error string to be captured
		 */
		private void captureErr(String string) {
			synchronized (this.monitor) {
				this.capturedStrings.add(new CapturedString(Type.ERR, string));
			}
		}

		/**
		 * Appends captured strings to the provided StringBuilder based on the given
		 * filter.
		 * @param builder The StringBuilder to append the captured strings to.
		 * @param filter The filter to determine which captured strings to append.
		 */
		void append(StringBuilder builder, Predicate<Type> filter) {
			synchronized (this.monitor) {
				for (CapturedString stringCapture : this.capturedStrings) {
					if (filter.test(stringCapture.getType())) {
						builder.append(stringCapture);
					}
				}
			}
		}

		/**
		 * Resets the captured strings by clearing the list of captured strings. This
		 * method is synchronized to ensure thread safety.
		 */
		void reset() {
			synchronized (this.monitor) {
				this.capturedStrings.clear();
			}
		}

	}

	/**
	 * A {@link PrintStream} implementation that captures written strings.
	 */
	private static class PrintStreamCapture extends PrintStream {

		private final PrintStream parent;

		/**
		 * Constructs a new PrintStreamCapture object with the specified parent
		 * PrintStream and copy Consumer.
		 * @param parent the parent PrintStream to capture output from
		 * @param copy the Consumer to copy captured output to
		 */
		PrintStreamCapture(PrintStream parent, Consumer<String> copy) {
			super(new OutputStreamCapture(getSystemStream(parent), copy));
			this.parent = parent;
		}

		/**
		 * Returns the parent PrintStream object of this PrintStreamCapture object.
		 * @return the parent PrintStream object
		 */
		PrintStream getParent() {
			return this.parent;
		}

		/**
		 * Returns the system stream of the given PrintStream object. If the given
		 * PrintStream object is an instance of PrintStreamCapture, it will traverse
		 * through the parent PrintStreamCapture objects until it finds a
		 * non-PrintStreamCapture parent. The non-PrintStreamCapture parent will be
		 * returned as the system stream.
		 * @param printStream the PrintStream object to get the system stream from
		 * @return the system stream of the given PrintStream object
		 */
		private static PrintStream getSystemStream(PrintStream printStream) {
			while (printStream instanceof PrintStreamCapture printStreamCapture) {
				printStream = printStreamCapture.getParent();
			}
			return printStream;
		}

	}

	/**
	 * An {@link OutputStream} implementation that captures written strings.
	 */
	private static class OutputStreamCapture extends OutputStream {

		private final PrintStream systemStream;

		private final Consumer<String> copy;

		/**
		 * Constructs an OutputStreamCapture object with the specified systemStream and
		 * copy consumer.
		 * @param systemStream the PrintStream to capture the output from
		 * @param copy the consumer to copy the captured output to
		 */
		OutputStreamCapture(PrintStream systemStream, Consumer<String> copy) {
			this.systemStream = systemStream;
			this.copy = copy;
		}

		/**
		 * Writes a single byte to the output stream.
		 * @param b the byte to be written
		 * @throws IOException if an I/O error occurs
		 */
		@Override
		public void write(int b) throws IOException {
			write(new byte[] { (byte) (b & 0xFF) });
		}

		/**
		 * Writes a portion of an array of bytes to this output stream. The write
		 * operation is performed by copying the specified portion of the byte array,
		 * converting it into a string, and passing it to the copy consumer. The specified
		 * portion is then written to the underlying system output stream.
		 * @param b the data.
		 * @param off the start offset in the data.
		 * @param len the number of bytes to write.
		 * @throws IOException if an I/O error occurs.
		 */
		@Override
		public void write(byte[] b, int off, int len) throws IOException {
			this.copy.accept(new String(b, off, len));
			this.systemStream.write(b, off, len);
		}

		/**
		 * Flushes the output stream.
		 * @throws IOException if an I/O error occurs.
		 */
		@Override
		public void flush() throws IOException {
			this.systemStream.flush();
		}

	}

	/**
	 * A captured string that forms part of the full output.
	 */
	private static class CapturedString {

		private final Type type;

		private final String string;

		/**
		 * Constructs a new CapturedString object with the specified type and string.
		 * @param type the type of the captured string
		 * @param string the captured string
		 */
		CapturedString(Type type, String string) {
			this.type = type;
			this.string = string;
		}

		/**
		 * Returns the type of the CapturedString object.
		 * @return the type of the CapturedString object
		 */
		Type getType() {
			return this.type;
		}

		/**
		 * Returns a string representation of the CapturedString object.
		 * @return the string representation of the CapturedString object
		 */
		@Override
		public String toString() {
			return this.string;
		}

	}

	/**
	 * Types of content that can be captured.
	 */
	private enum Type {

		OUT, ERR

	}

}
