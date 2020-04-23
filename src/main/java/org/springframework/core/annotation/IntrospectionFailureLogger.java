/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.core.annotation;

import org.springframework.lang.Nullable;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Logger facade used to handle annotation introspection failures (in particular
 * {@code TypeNotPresentExceptions}). Allows annotation processing to continue,
 * assuming that when Class attribute values are not resolvable the annotation
 * should effectively disappear.
 * <p></p>
 * This class uses a stadard JRE {@link Logger} created via
 * {@code Logger.getLogger(MergedAnnotation.class.getName())} with no extra
 * {@link java.util.logging.Handler Handler} attached. I.e. it inherits its parent's
 * handler. Normally the parent should be the root logger. So if you want to change
 * the handler's log level, see {@link java.util.logging.LogManager LogManager} for
 * how to do that via properties file or command line.
 * <p></p>
 * Something like this could work if you want to do it during runtime:
 * <pre>{@code
 * // For the logger
 * Logger myLogger = Logger.getLogger(MergedAnnotation.class.getName())
 * myLogger.setLevel(Level.FINE);
 *
 * // For the parent's handlers
 * for (Handler handler : myLogger.getParent().getHandlers())
 *   handler.setLevel(logLevel);
 * }</pre>
 *
 * @author Phillip Webb
 * @since 5.2
 */
enum IntrospectionFailureLogger {

	DEBUG {
		@Override
		public boolean isEnabled() {
			return getLogger().isLoggable(Level.FINE);
		}
		@Override
		public void log(String message) {
			getLogger().fine(message);
		}
	},

	INFO {
		@Override
		public boolean isEnabled() {
			return getLogger().isLoggable(Level.INFO);
		}
		@Override
		public void log(String message) {
			getLogger().info(message);
		}
	};


	@Nullable
	private static Logger logger;


	void log(String message, @Nullable Object source, Exception ex) {
		String on = (source != null ? " on " + source : "");
		log(message + on + ": " + ex);
	}

	abstract boolean isEnabled();

	abstract void log(String message);


	private static Logger getLogger() {
		Logger logger = IntrospectionFailureLogger.logger;
		if (logger == null) {
			// TODO: Why MergedAnnotation and not IntrospectionFailureLogger? Spring bug?
			logger = Logger.getLogger(MergedAnnotation.class.getName());
			IntrospectionFailureLogger.logger = logger;
		}
		return logger;
	}

}
