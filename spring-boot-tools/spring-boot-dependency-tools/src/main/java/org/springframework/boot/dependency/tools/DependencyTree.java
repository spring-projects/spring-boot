/*
 * Copyright 2012-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.dependency.tools;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Dependency tree information that can be loaded from the output of
 * {@literal mvn dependency:tree}.
 * 
 * @author Phillip Webb
 * @since 1.1.0
 * @see DependencyNode
 */
class DependencyTree implements Iterable<DependencyNode> {

	private final DependencyNode root;

	/**
	 * Create a new {@link DependencyTree} instance for the given input stream.
	 * @param inputStream input stream containing content from
	 * {@literal mvn dependency:tree} (the stream will be closed).
	 */
	public DependencyTree(InputStream inputStream) {
		try {
			this.root = parse(inputStream);
		}
		catch (IOException ex) {
			throw new IllegalStateException(ex);
		}
	}

	private DependencyNode parse(InputStream inputStream) throws IOException {
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
			Parser parser = new Parser();
			String line;
			while ((line = reader.readLine()) != null) {
				parser.append(line);
			}
			return parser.getRoot();
		}
		finally {
			inputStream.close();
		}
	}

	@Override
	public Iterator<DependencyNode> iterator() {
		return getDependencies().iterator();
	}

	/**
	 * @return the root node for the tree.
	 */
	public DependencyNode getRoot() {
		return this.root;
	}

	/**
	 * @return the dependencies of the root node.
	 */
	public List<DependencyNode> getDependencies() {
		return this.root.getDependencies();
	}

	/**
	 * Return the node at the specified index.
	 * @param index the index (multiple indexes can be used to traverse the tree)
	 * @return the node at the specified index
	 */
	public DependencyNode get(int... index) {
		DependencyNode rtn = this.root;
		for (int i : index) {
			rtn = rtn.getDependencies().get(i);
		}
		return rtn;
	}

	private static class Parser {

		private static final int INDENT = 3;

		private static final Set<Character> PREFIX_CHARS = new HashSet<Character>(
				Arrays.asList(' ', '+', '-', '\\', '|'));

		private static final Pattern LINE_PATTERN = Pattern
				.compile("[(]?([^:]*):([^:]*):([^:]*):([^:\\s]*)");

		private Deque<DependencyNode> stack = new ArrayDeque<DependencyNode>();

		public void append(String line) {
			int depth = getDepth(line);
			String data = line.substring(depth * INDENT);
			if (depth == 0) {
				this.stack.push(createNode(data));
			}
			else {
				while (depth < this.stack.size()) {
					this.stack.pop();
				}
				if (depth > this.stack.size()) {
					this.stack.push(this.stack.peek().getLastDependency());
				}
				this.stack.peek().addDependency(createNode(data));
			}
		}

		private int getDepth(String line) {
			for (int i = 0; i < line.length(); i++) {
				if (!Parser.PREFIX_CHARS.contains(line.charAt(i))) {
					return i / INDENT;
				}
			}
			return 0;
		}

		private DependencyNode createNode(String line) {
			Matcher matcher = LINE_PATTERN.matcher(line);
			if (!matcher.find()) {
				throw new IllegalStateException("Unable to parese line " + line);
			}
			return new DependencyNode(matcher.group(1), matcher.group(2),
					matcher.group(4));
		}

		public DependencyNode getRoot() {
			return this.stack.getLast();
		}

	}

}
