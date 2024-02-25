/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.cli.command.encodepassword;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import joptsimple.OptionSet;
import joptsimple.OptionSpec;

import org.springframework.boot.cli.command.Command;
import org.springframework.boot.cli.command.HelpExample;
import org.springframework.boot.cli.command.OptionParsingCommand;
import org.springframework.boot.cli.command.options.OptionHandler;
import org.springframework.boot.cli.command.status.ExitStatus;
import org.springframework.boot.cli.util.Log;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.password.Pbkdf2PasswordEncoder;
import org.springframework.util.StringUtils;

/**
 * {@link Command} to encode passwords for use with Spring Security.
 *
 * @author Phillip Webb
 * @author Moritz Halbritter
 * @since 2.0.0
 */
public class EncodePasswordCommand extends OptionParsingCommand {

	private static final Map<String, Supplier<PasswordEncoder>> ENCODERS;

	static {
		Map<String, Supplier<PasswordEncoder>> encoders = new LinkedHashMap<>();
		encoders.put("default", PasswordEncoderFactories::createDelegatingPasswordEncoder);
		encoders.put("bcrypt", BCryptPasswordEncoder::new);
		encoders.put("pbkdf2", Pbkdf2PasswordEncoder::defaultsForSpringSecurity_v5_8);
		ENCODERS = Collections.unmodifiableMap(encoders);
	}

	/**
	 * Constructor for the EncodePasswordCommand class.
	 * @param commandName The name of the command.
	 * @param commandDescription The description of the command.
	 * @param optionHandler The option handler for the command.
	 */
	public EncodePasswordCommand() {
		super("encodepassword", "Encode a password for use with Spring Security", new EncodePasswordOptionHandler());
	}

	/**
	 * Returns the usage help for the EncodePasswordCommand.
	 * @return the usage help string in the format "[options] <password to encode>"
	 */
	@Override
	public String getUsageHelp() {
		return "[options] <password to encode>";
	}

	/**
	 * Returns a collection of help examples for the getExamples() method.
	 * @return a collection of HelpExample objects representing different usage examples
	 */
	@Override
	public Collection<HelpExample> getExamples() {
		List<HelpExample> examples = new ArrayList<>();
		examples.add(new HelpExample("To encode a password with the default (bcrypt) encoder",
				"spring encodepassword mypassword"));
		examples.add(new HelpExample("To encode a password with pbkdf2", "spring encodepassword -a pbkdf2 mypassword"));
		return examples;
	}

	/**
	 * EncodePasswordOptionHandler class.
	 */
	private static final class EncodePasswordOptionHandler extends OptionHandler {

		private OptionSpec<String> algorithm;

		/**
		 * Sets the options for encoding the password.
		 * @param algorithm The algorithm to use. Supported algorithms: algorithm, a. The
		 * default algorithm uses bcrypt.
		 * @return The EncodePasswordOptionHandler instance.
		 */
		@Override
		protected void options() {
			this.algorithm = option(Arrays.asList("algorithm", "a"),
					"The algorithm to use. Supported algorithms: "
							+ StringUtils.collectionToDelimitedString(ENCODERS.keySet(), ", ")
							+ ". The default algorithm uses bcrypt")
				.withRequiredArg()
				.defaultsTo("default");
		}

		/**
		 * Executes the run method with the given options.
		 * @param options the options provided to the method
		 * @return the exit status of the method
		 */
		@Override
		protected ExitStatus run(OptionSet options) {
			if (options.nonOptionArguments().size() != 1) {
				Log.error("A single password option must be provided");
				return ExitStatus.ERROR;
			}
			String algorithm = options.valueOf(this.algorithm);
			String password = (String) options.nonOptionArguments().get(0);
			Supplier<PasswordEncoder> encoder = ENCODERS.get(algorithm);
			if (encoder == null) {
				Log.error("Unknown algorithm, valid options are: "
						+ StringUtils.collectionToCommaDelimitedString(ENCODERS.keySet()));
				return ExitStatus.ERROR;
			}
			Log.info(encoder.get().encode(password));
			return ExitStatus.OK;
		}

	}

}
