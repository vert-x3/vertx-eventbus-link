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

import ch.qos.logback.classic.Logger;
import com.zaxxer.nuprocess.NuAbstractProcessHandler;
import com.zaxxer.nuprocess.NuProcess;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.regex.Pattern;

public class VertxProcessHandler extends NuAbstractProcessHandler {

  private static final Pattern NEWLINE = Pattern.compile("\\r?\\n");

  static {
    try {
      FileUtils.deleteDirectory(new File("target", "logs"));
    } catch (IOException ignore) {
    }
  }

  private final CompletableFuture<Void> ready;

  private NuProcess nuProcess;
  private Logger log;

  private String outPending;
  private String errPending;

  public VertxProcessHandler(CompletableFuture<Void> ready) {
    this.ready = ready;
  }

  @Override
  public void onStart(NuProcess nuProcess) {
    this.nuProcess = nuProcess;
    log = LoggingUtil.createLogger(nuProcess.getPID());
  }

  @Override
  public void onStdout(ByteBuffer buffer, boolean closed) {
    if (closed) {
      if (outPending != null) {
        onStdout(outPending);
      }
      return;
    }
    outPending = onBuffer(buffer, outPending, this::onStdout);
  }

  private void onStdout(String line) {
    log.info(line);
    if (line.contains("Succeeded in deploying verticle")) {
      ready.complete(null);
    }
  }

  @Override
  public void onStderr(ByteBuffer buffer, boolean closed) {
    if (closed) {
      if (errPending != null) {
        onStdout(errPending);
      }
      return;
    }
    errPending = onBuffer(buffer, errPending, this::onStdout);
  }

  @Override
  public void onExit(int statusCode) {
    log.info("Exited with status {}", statusCode);
  }

  private static String onBuffer(ByteBuffer buffer, String pending, Consumer<String> consumer) {
    String[] strings = split(buffer);
    if (strings.length == 1) {
      pending = (pending == null) ? strings[0]:(pending + strings[0]);
    } else {
      for (int i = 0; i < strings.length - 1; i++) {
        String string = strings[i];
        if (i == 0 && pending != null) {
          string = pending + string;
          pending = null;
        }
        consumer.accept(string);
      }
      String last = strings[strings.length - 1];
      if (!last.isEmpty()) {
        pending = last;
      }
    }
    return pending;
  }

  private static String[] split(ByteBuffer buffer) {
    byte[] bytes = new byte[buffer.remaining()];
    buffer.get(bytes);
    return NEWLINE.split(new String(bytes), -1);
  }
}
