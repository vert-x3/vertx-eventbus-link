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
import java.util.concurrent.CompletableFuture;

public class VertxProcess {

  private final NuProcess process;
  private final CompletableFuture<Void> ready;

  public VertxProcess(NuProcess process, CompletableFuture<Void> ready) {
    this.process = process;
    this.ready = ready;
  }

  public static VertxProcess startNode(String appName, int linkServerPort, int linkClientPort, int httpServerPort) {
    String conf = "{" +
      "\"eventBusLinkOptions\": {\"serverPort\": " + linkServerPort + ",\"clientPort\": " + linkClientPort + "}" + "," +
      "\"httpServerOptions\": {\"port\": " + httpServerPort + "}" +
      "}";
    NuProcessBuilder pb = new NuProcessBuilder(
      System.getProperty("java.home") + File.separator + "bin" + File.separator + "java",
      "-Djava.net.preferIPv4Stack=true",
      "-jar",
      "target/test-apps/" + appName,
      "-cluster",
      "-conf",
      "'" + conf + "'"
    );
    CompletableFuture<Void> ready = new CompletableFuture<>();
    VertxProcessHandler handler = new VertxProcessHandler(ready);
    pb.setProcessListener(handler);
    NuProcess process = pb.start();
    return new VertxProcess(process, ready);
  }

  public NuProcess nuProcess() {
    return process;
  }

  public CompletableFuture<Void> ready() {
    return ready;
  }
}
