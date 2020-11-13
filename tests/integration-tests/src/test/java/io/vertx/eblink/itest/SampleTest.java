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
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.fail;

public class SampleTest {

  private static List<VertxProcess> processes = new ArrayList<>();

  @BeforeAll
  static void beforeAll() throws Exception {
    for (int i = 1; i <= 3; i++) {
      processes.add(startNode(3, i));
      processes.add(startNode(4, i));
    }
    CompletableFuture<?>[] futures = processes.stream().map(VertxProcess::ready).toArray(CompletableFuture[]::new);
    CompletableFuture.allOf(futures).get(1, TimeUnit.MINUTES);
  }

  private static VertxProcess startNode(int clusterVersion, int node) {
    NuProcessBuilder pb = new NuProcessBuilder(
      System.getProperty("java.home") + File.separator + "bin" + File.separator + "java",
      "-Djava.net.preferIPv4Stack=true",
      "-jar",
      "target/test-apps/vertx" + clusterVersion + "-app.jar",
      "-cluster"
    );
    CompletableFuture<Void> ready = new CompletableFuture<>();
    VertxProcessHandler handler = new VertxProcessHandler(clusterVersion, node, ready);
    pb.setProcessListener(handler);
    NuProcess process = pb.start();
    return new VertxProcess(process, ready);
  }

  @Test
  void clustersShouldStart() throws Exception {
    fail("Not implemented yet");
  }

  @AfterAll
  static void afterAll() {
    for (VertxProcess process : processes) {
      process.nuProcess().destroy(true);
    }
  }
}
