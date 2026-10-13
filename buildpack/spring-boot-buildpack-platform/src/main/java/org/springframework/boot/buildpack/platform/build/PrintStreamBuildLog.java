/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.buildpack.platform.build;

import java.io.PrintStream;
import java.util.function.Consumer;

import org.springframework.boot.buildpack.platform.docker.TotalProgressBar;
import org.springframework.boot.buildpack.platform.docker.TotalProgressEvent;

/**
 * {@link BuildLog} implementation that prints output to a {@link PrintStream}.
 *
 * @author Phillip Webb
 * @see BuildLog#to(PrintStream)
 */
class PrintStreamBuildLog extends AbstractBuildLog {

	private final PrintStream out;

	PrintStreamBuildLog(PrintStream out) {
		this.out = out;
	}

	@Override
	protected void log(String message) {
		this.out.println(message);
	}

	@Override
	protected Consumer<TotalProgressEvent> getProgressConsumer(String prefix) {
		return new TotalProgressBar(prefix, '.', false, this.out);
	}

}
