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

package org.springframework.boot.logging.logback;

import java.util.ArrayList;
import java.util.List;

import ch.qos.logback.core.joran.action.Action;
import ch.qos.logback.core.joran.event.InPlayListener;
import ch.qos.logback.core.joran.event.SaxEvent;
import ch.qos.logback.core.joran.spi.ActionException;
import ch.qos.logback.core.joran.spi.InterpretationContext;
import ch.qos.logback.core.joran.spi.Interpreter;
import ch.qos.logback.core.util.OptionHelper;
import org.xml.sax.Attributes;

import org.springframework.core.env.Environment;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Logback {@link Action} to support {@code <springProfile>} tags. Allows section of a
 * logback configuration to only be enabled when a specific profile is active.
 *
 * @author Phillip Webb
 * @author Eddú Meléndez
 */
class SpringProfileAction extends Action implements InPlayListener {

	private final Environment environment;

	private int depth = 0;

	private boolean acceptsProfile;

	private List<SaxEvent> events;

	SpringProfileAction(Environment environment) {
		this.environment = environment;
	}

	@Override
	public void begin(InterpretationContext ic, String name, Attributes attributes)
			throws ActionException {
		this.depth++;
		if (this.depth != 1) {
			return;
		}
		ic.pushObject(this);
		this.acceptsProfile = acceptsProfiles(ic, attributes);
		this.events = new ArrayList<SaxEvent>();
		ic.addInPlayListener(this);
	}

	private boolean acceptsProfiles(InterpretationContext ic, Attributes attributes) {
		String[] profileNames = StringUtils.trimArrayElements(StringUtils
				.commaDelimitedListToStringArray(attributes.getValue(NAME_ATTRIBUTE)));
		if (this.environment == null || profileNames.length == 0) {
			return false;
		}
		for (int i = 0; i < profileNames.length; i++) {
			profileNames[i] = OptionHelper.substVars(profileNames[i], ic, this.context);
		}
		return this.environment.acceptsProfiles(profileNames);
	}

	@Override
	public void end(InterpretationContext ic, String name) throws ActionException {
		this.depth--;
		if (this.depth != 0) {
			return;
		}
		ic.removeInPlayListener(this);
		verifyAndPop(ic);
		if (this.acceptsProfile) {
			addEventsToPlayer(ic);
		}
	}

	private void verifyAndPop(InterpretationContext ic) {
		Object o = ic.peekObject();
		Assert.state(o != null, "Unexpected null object on stack");
		Assert.isInstanceOf(SpringProfileAction.class, o, "logback stack error");
		Assert.state(o == this, "ProfileAction different than current one on stack");
		ic.popObject();
	}

	private void addEventsToPlayer(InterpretationContext ic) {
		Interpreter interpreter = ic.getJoranInterpreter();
		this.events.remove(0);
		this.events.remove(this.events.size() - 1);
		interpreter.getEventPlayer().addEventsDynamically(this.events, 1);
	}

	@Override
	public void inPlay(SaxEvent event) {
		this.events.add(event);
	}

}
