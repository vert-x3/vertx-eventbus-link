/*
 * Copyright 2020 Red Hat, Inc.
 *
 * Red Hat licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package io.vertx.eblink.itest;

import ch.qos.logback.classic.AsyncAppender;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.FileAppender;
import org.slf4j.LoggerFactory;

public class LoggingUtil {

  private LoggingUtil() {
    // Utility class
  }

  public static Logger createLogger(int pid) {
    LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();

    PatternLayoutEncoder ple = new PatternLayoutEncoder();
    ple.setPattern("%msg%n");
    ple.setContext(lc);
    ple.start();

    FileAppender<ILoggingEvent> fileAppender = new FileAppender<>();
    fileAppender.setContext(lc);
    fileAppender.setFile("target/logs/" + pid);
    fileAppender.setEncoder(ple);
    fileAppender.start();

    AsyncAppender asyncAppender = new AsyncAppender();
    asyncAppender.setContext(lc);
    asyncAppender.addAppender(fileAppender);
    asyncAppender.start();

    Logger logger = (Logger) LoggerFactory.getLogger("node" + "-" + pid);
    logger.addAppender(asyncAppender);
    logger.setLevel(Level.INFO);
    logger.setAdditive(false);

    return logger;
  }
}
