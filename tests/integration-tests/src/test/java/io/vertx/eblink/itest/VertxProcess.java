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

import com.zaxxer.nuprocess.NuProcess;
import com.zaxxer.nuprocess.NuProcessBuilder;

import java.io.File;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class VertxProcess {

  private final String nodeName;
  private final NuProcess process;
  private final CompletableFuture<Void> ready;
  private final VertxProcessHandler processHandler;

  public VertxProcess(String nodeName, CompletableFuture<Void> ready, NuProcess process, VertxProcessHandler processHandler) {
    this.nodeName = nodeName;
    this.process = process;
    this.ready = ready;
    this.processHandler = processHandler;
  }

  public static VertxProcess startNode(String nodeName, String jarFilename, String conf) {
    NuProcessBuilder pb = new NuProcessBuilder(
      System.getProperty("java.home") + File.separator + "bin" + File.separator + "java",
      "-Djava.net.preferIPv4Stack=true",
      "-jar",
      "target/test-apps/" + jarFilename,
      "-cluster",
      "-conf",
      "'" + conf + "'"
    );
    CompletableFuture<Void> ready = new CompletableFuture<>();
    VertxProcessHandler handler = new VertxProcessHandler(ready);
    pb.setProcessListener(handler);
    NuProcess process = pb.start();
    return new VertxProcess(nodeName, ready, process, handler);
  }

  public NuProcess nuProcess() {
    return process;
  }

  public CompletableFuture<Void> ready() {
    return ready;
  }

  public String nodeName() {
    return nodeName;
  }

  public List<String> output() {
    return processHandler.output();
  }
}
