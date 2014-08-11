package org.springframework.boot;

import java.io.PrintStream;

/**
 * Interface class for writing a Banner programmatically
 * 
 * @author Michael Stummvoll
 */
public interface Banner {
	/**
	 * Write the banner to the specified print stream.
	 * 
	 * @param printStream
	 *            the output print stream
	 */
	void write(PrintStream out);
}
