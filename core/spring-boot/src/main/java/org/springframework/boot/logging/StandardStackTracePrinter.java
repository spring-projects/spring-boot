/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.logging;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;

import org.springframework.util.Assert;

/**
 * {@link StackTracePrinter} that prints a standard form stack trace. This printer
 * produces a result in a similar form to {@link Throwable#printStackTrace()}, but offers
 * more customization options.
 *
 * @author Phillip Webb
 * @since 3.5.0
 */
public final class StandardStackTracePrinter implements StackTracePrinter {

	private static final String DEFAULT_LINE_SEPARATOR = System.lineSeparator();

	private static final ToIntFunction<StackTraceElement> DEFAULT_FRAME_HASHER = (frame) -> Objects
		.hash(frame.getClassName(), frame.getMethodName(), frame.getLineNumber());

	private static final int UNLIMITED = Integer.MAX_VALUE;

	private final EnumSet<Option> options;

	private final int maximumLength;

	private final String lineSeparator;

	private final Predicate<Throwable> filter;

	private final BiPredicate<Integer, StackTraceElement> frameFilter;

	private final Function<Throwable, String> formatter;

	private final Function<StackTraceElement, String> frameFormatter;

	private final ToIntFunction<StackTraceElement> frameHasher;

	private StandardStackTracePrinter(EnumSet<Option> options, int maximumLength, String lineSeparator,
			Predicate<Throwable> filter, BiPredicate<Integer, StackTraceElement> frameFilter,
			Function<Throwable, String> formatter, Function<StackTraceElement, String> frameFormatter,
			ToIntFunction<StackTraceElement> frameHasher) {
		this.options = options;
		this.maximumLength = maximumLength;
		this.lineSeparator = (lineSeparator != null) ? lineSeparator : DEFAULT_LINE_SEPARATOR;
		this.filter = (filter != null) ? filter : (t) -> true;
		this.frameFilter = (frameFilter != null) ? frameFilter : (i, t) -> true;
		this.formatter = (formatter != null) ? formatter : Object::toString;
		this.frameFormatter = (frameFormatter != null) ? frameFormatter : Object::toString;
		this.frameHasher = frameHasher;
	}

	@Override
	public void printStackTrace(Throwable throwable, Appendable out) throws IOException {
		if (this.filter.test(throwable)) {
			Set<Throwable> seen = Collections.newSetFromMap(new IdentityHashMap<>());
			Output output = new Output(out);
			Print print = new Print("", "", output);
			printFullStackTrace(seen, print, new StackTrace(throwable), null);
		}
	}

	private void printFullStackTrace(Set<Throwable> seen, Print print, StackTrace stackTrace, StackTrace enclosing)
			throws IOException {
		if (stackTrace == null) {
			return;
		}
		if (!seen.add(stackTrace.throwable())) {
			String hashPrefix = stackTrace.hashPrefix(this.frameHasher);
			String throwable = this.formatter.apply(stackTrace.throwable());
			print.circularReference(hashPrefix, throwable);
			return;
		}
		StackTrace cause = stackTrace.cause();
		if (!hasOption(Option.ROOT_FIRST)) {
			printSingleStackTrace(seen, print, stackTrace, enclosing);
			printFullStackTrace(seen, print.withCausedByCaption(cause), cause, stackTrace);
		}
		else {
			printFullStackTrace(seen, print, cause, stackTrace);
			printSingleStackTrace(seen, print.withWrappedByCaption(cause), stackTrace, enclosing);
		}
	}

	private void printSingleStackTrace(Set<Throwable> seen, Print print, StackTrace stackTrace, StackTrace enclosing)
			throws IOException {
		String hashPrefix = stackTrace.hashPrefix(this.frameHasher);
		String throwable = this.formatter.apply(stackTrace.throwable());
		print.thrown(hashPrefix, throwable);
		printFrames(print, stackTrace, enclosing);
		if (!hasOption(Option.HIDE_SUPPRESSED)) {
			for (StackTrace suppressed : stackTrace.suppressed()) {
				printFullStackTrace(seen, print.withSuppressedCaption(), suppressed, stackTrace);
			}
		}
	}

	private void printFrames(Print print, StackTrace stackTrace, StackTrace enclosing) throws IOException {
		int commonFrames = (!hasOption(Option.SHOW_COMMON_FRAMES)) ? stackTrace.commonFramesCount(enclosing) : 0;
		int filteredFrames = 0;
		for (int i = 0; i < stackTrace.frames().length - commonFrames; i++) {
			StackTraceElement element = stackTrace.frames()[i];
			if (!this.frameFilter.test(i, element)) {
				filteredFrames++;
				continue;
			}
			print.omittedFilteredFrames(filteredFrames);
			filteredFrames = 0;
			print.at(this.frameFormatter.apply(element));
		}
		print.omittedFilteredFrames(filteredFrames);
		if (commonFrames != 0) {
			print.omittedCommonFrames(commonFrames);
		}
	}

	/**
	 * Return a new {@link StandardStackTracePrinter} from this one that will print all
	 * common frames rather the replacing them with the {@literal "... N more"} message.
	 * @return a new {@link StandardStackTracePrinter} instance
	 */
	public StandardStackTracePrinter withCommonFrames() {
		return withOption(Option.SHOW_COMMON_FRAMES);
	}

	/**
	 * Return a new {@link StandardStackTracePrinter} from this one that will not print
	 * {@link Throwable#getSuppressed() suppressed} items.
	 * @return a new {@link StandardStackTracePrinter} instance
	 */
	public StandardStackTracePrinter withoutSuppressed() {
		return withOption(Option.HIDE_SUPPRESSED);
	}

	/**
	 * Return a new {@link StandardStackTracePrinter} from this one that will use ellipses
	 * to truncate output longer that the specified length.
	 * @param maximumLength the maximum length that can be printed
	 * @return a new {@link StandardStackTracePrinter} instance
	 */
	public StandardStackTracePrinter withMaximumLength(int maximumLength) {
		Assert.isTrue(maximumLength > 0, "'maximumLength' must be positive");
		return new StandardStackTracePrinter(this.options, maximumLength, this.lineSeparator, this.filter,
				this.frameFilter, this.formatter, this.frameFormatter, this.frameHasher);
	}

	/**
	 * Return a new {@link StandardStackTracePrinter} from this one that filter frames
	 * (including caused and suppressed) deeper then the specified maximum.
	 * @param maximumThrowableDepth the maximum throwable depth
	 * @return a new {@link StandardStackTracePrinter} instance
	 */
	public StandardStackTracePrinter withMaximumThrowableDepth(int maximumThrowableDepth) {
		Assert.isTrue(maximumThrowableDepth > 0, "'maximumThrowableDepth' must be positive");
		return withFrameFilter((index, element) -> index < maximumThrowableDepth);
	}

	/**
	 * Return a new {@link StandardStackTracePrinter} from this one that will only include
	 * throwables (excluding caused and suppressed) that match the given predicate.
	 * @param predicate the predicate used to filter the throwable
	 * @return a new {@link StandardStackTracePrinter} instance
	 */
	public StandardStackTracePrinter withFilter(Predicate<Throwable> predicate) {
		Assert.notNull(predicate, "'predicate' must not be null");
		return new StandardStackTracePrinter(this.options, this.maximumLength, this.lineSeparator,
				this.filter.and(predicate), this.frameFilter, this.formatter, this.frameFormatter, this.frameHasher);
	}

	/**
	 * Return a new {@link StandardStackTracePrinter} from this one that will only include
	 * frames that match the given predicate.
	 * @param predicate the predicate used to filter frames
	 * @return a new {@link StandardStackTracePrinter} instance
	 */
	public StandardStackTracePrinter withFrameFilter(BiPredicate<Integer, StackTraceElement> predicate) {
		Assert.notNull(predicate, "'predicate' must not be null");
		return new StandardStackTracePrinter(this.options, this.maximumLength, this.lineSeparator, this.filter,
				this.frameFilter.and(predicate), this.formatter, this.frameFormatter, this.frameHasher);
	}

	/**
	 * Return a new {@link StandardStackTracePrinter} from this one that print the stack
	 * trace using the specified line separator.
	 * @param lineSeparator the line separator to use
	 * @return a new {@link StandardStackTracePrinter} instance
	 */
	public StandardStackTracePrinter withLineSeparator(String lineSeparator) {
		Assert.notNull(lineSeparator, "'lineSeparator' must not be null");
		return new StandardStackTracePrinter(this.options, this.maximumLength, lineSeparator, this.filter,
				this.frameFilter, this.formatter, this.frameFormatter, this.frameHasher);
	}

	/**
	 * Return a new {@link StandardStackTracePrinter} from this one uses the specified
	 * formatter to create a string representation of a throwable.
	 * @param formatter the formatter to use
	 * @return a new {@link StandardStackTracePrinter} instance
	 * @see #withLineSeparator(String)
	 */
	public StandardStackTracePrinter withFormatter(Function<Throwable, String> formatter) {
		Assert.notNull(formatter, "'formatter' must not be null");
		return new StandardStackTracePrinter(this.options, this.maximumLength, this.lineSeparator, this.filter,
				this.frameFilter, formatter, this.frameFormatter, this.frameHasher);
	}

	/**
	 * Return a new {@link StandardStackTracePrinter} from this one uses the specified
	 * formatter to create a string representation of a frame.
	 * @param frameFormatter the frame formatter to use
	 * @return a new {@link StandardStackTracePrinter} instance
	 * @see #withLineSeparator(String)
	 */
	public StandardStackTracePrinter withFrameFormatter(Function<StackTraceElement, String> frameFormatter) {
		Assert.notNull(frameFormatter, "'frameFormatter' must not be null");
		return new StandardStackTracePrinter(this.options, this.maximumLength, this.lineSeparator, this.filter,
				this.frameFilter, this.formatter, frameFormatter, this.frameHasher);
	}

	/**
	 * Return a new {@link StandardStackTracePrinter} from this one that generates and
	 * prints hashes for each stacktrace.
	 * @return a new {@link StandardStackTracePrinter} instance
	 */
	public StandardStackTracePrinter withHashes() {
		return withHashes(true);
	}

	/**
	 * Return a new {@link StandardStackTracePrinter} from this one that changes if hashes
	 * should be generated and printed for each stacktrace.
	 * @param hashes if hashes should be added
	 * @return a new {@link StandardStackTracePrinter} instance
	 */
	public StandardStackTracePrinter withHashes(boolean hashes) {
		return withHashes((!hashes) ? null : DEFAULT_FRAME_HASHER);
	}

	public StandardStackTracePrinter withHashes(ToIntFunction<StackTraceElement> frameHasher) {
		return new StandardStackTracePrinter(this.options, this.maximumLength, this.lineSeparator, this.filter,
				this.frameFilter, this.formatter, this.frameFormatter, frameHasher);
	}

	private StandardStackTracePrinter withOption(Option option) {
		EnumSet<Option> options = EnumSet.copyOf(this.options);
		options.add(option);
		return new StandardStackTracePrinter(options, this.maximumLength, this.lineSeparator, this.filter,
				this.frameFilter, this.formatter, this.frameFormatter, this.frameHasher);
	}

	private boolean hasOption(Option option) {
		return this.options.contains(option);
	}

	/**
	 * Return a {@link StandardStackTracePrinter} that prints the stack trace with the
	 * root exception last (the same as {@link Throwable#printStackTrace()}).
	 * @return a {@link StandardStackTracePrinter} that prints the stack trace root last
	 */
	public static StandardStackTracePrinter rootLast() {
		return new StandardStackTracePrinter(EnumSet.noneOf(Option.class), UNLIMITED, null, null, null, null, null,
				null);
	}

	/**
	 * Return a {@link StandardStackTracePrinter} that prints the stack trace with the
	 * root exception first (the opposite of {@link Throwable#printStackTrace()}).
	 * @return a {@link StandardStackTracePrinter} that prints the stack trace root first
	 */
	public static StandardStackTracePrinter rootFirst() {
		return new StandardStackTracePrinter(EnumSet.of(Option.ROOT_FIRST), UNLIMITED, null, null, null, null, null,
				null);
	}

	/**
	 * Options supported by this printer.
	 */
	private enum Option {

		ROOT_FIRST, SHOW_COMMON_FRAMES, HIDE_SUPPRESSED

	}

	/**
	 * Prints the actual line output.
	 */
	private record Print(String indent, String caption, Output output) {

		void circularReference(String hashPrefix, String throwable) throws IOException {
			this.output.println(this.indent, this.caption + "[CIRCULAR REFERENCE: " + hashPrefix + throwable + "]");
		}

		void thrown(String hashPrefix, String throwable) throws IOException {
			this.output.println(this.indent, this.caption + hashPrefix + throwable);
		}

		void at(String frame) throws IOException {
			this.output.println(this.indent, "\tat " + frame);
		}

		void omittedFilteredFrames(int filteredFrameCount) throws IOException {
			if (filteredFrameCount > 0) {
				this.output.println(this.indent, "\t... " + filteredFrameCount + " filtered");
			}
		}

		void omittedCommonFrames(int commonFrameCount) throws IOException {
			this.output.println(this.indent, "\t... " + commonFrameCount + " more");
		}

		Print withCausedByCaption(StackTrace causedBy) {
			return withCaption(causedBy != null, "", "Caused by: ");
		}

		Print withWrappedByCaption(StackTrace wrappedBy) {
			return withCaption(wrappedBy != null, "", "Wrapped by: ");
		}

		public Print withSuppressedCaption() {
			return withCaption(true, "\t", "Suppressed: ");
		}

		private Print withCaption(boolean test, String extraIndent, String caption) {
			return (test) ? new Print(this.indent + extraIndent, caption, this.output) : this;
		}

	}

	/**
	 * Line-by-line output.
	 */
	private class Output {

		private static final String ELLIPSIS = "...";

		private final Appendable out;

		private int remaining;

		Output(Appendable out) {
			this.out = out;
			this.remaining = StandardStackTracePrinter.this.maximumLength - ELLIPSIS.length();
		}

		void println(String indent, String string) throws IOException {
			if (this.remaining > 0) {
				String line = indent + string + StandardStackTracePrinter.this.lineSeparator;
				if (line.length() > this.remaining) {
					line = line.substring(0, this.remaining) + ELLIPSIS;
				}
				this.out.append(line);
				this.remaining -= line.length();
			}
		}

	}

	/**
	 * Holds the stacktrace for a specific throwable and caches things that are expensive
	 * to calculate.
	 */
	private static final class StackTrace {

		private final Throwable throwable;

		private final StackTraceElement[] frames;

		private StackTrace[] suppressed;

		private StackTrace cause;

		private Integer hash;

		private String hashPrefix;

		private StackTrace(Throwable throwable) {
			this.throwable = throwable;
			this.frames = (throwable != null) ? throwable.getStackTrace() : null;
		}

		Throwable throwable() {
			return this.throwable;
		}

		StackTraceElement[] frames() {
			return this.frames;
		}

		int commonFramesCount(StackTrace other) {
			if (other == null) {
				return 0;
			}
			int index = this.frames.length - 1;
			int otherIndex = other.frames.length - 1;
			while (index >= 0 && otherIndex >= 0 && this.frames[index].equals(other.frames[otherIndex])) {
				index--;
				otherIndex--;
			}
			return this.frames.length - 1 - index;
		}

		StackTrace[] suppressed() {
			if (this.suppressed == null && this.throwable != null) {
				this.suppressed = Arrays.stream(this.throwable.getSuppressed())
					.map(StackTrace::new)
					.toArray(StackTrace[]::new);
			}
			return this.suppressed;
		}

		StackTrace cause() {
			if (this.cause == null && this.throwable != null) {
				Throwable cause = this.throwable.getCause();
				this.cause = (cause != null) ? new StackTrace(cause) : null;
			}
			return this.cause;
		}

		String hashPrefix(ToIntFunction<StackTraceElement> frameHasher) {
			if (frameHasher == null || throwable() == null) {
				return "";
			}
			this.hashPrefix = (this.hashPrefix != null) ? this.hashPrefix
					: String.format("<#%08x> ", hash(new HashSet<>(), frameHasher));
			return this.hashPrefix;
		}

		private int hash(HashSet<Throwable> seen, ToIntFunction<StackTraceElement> frameHasher) {
			if (this.hash != null) {
				return this.hash;
			}
			int hash = 0;
			if (cause() != null && seen.add(cause().throwable())) {
				hash = cause().hash(seen, frameHasher);
			}
			hash = 31 * hash + throwable().getClass().getName().hashCode();
			for (StackTraceElement frame : frames()) {
				hash = 31 * hash + frameHasher.applyAsInt(frame);
			}
			this.hash = hash;
			return hash;
		}

	}

}
