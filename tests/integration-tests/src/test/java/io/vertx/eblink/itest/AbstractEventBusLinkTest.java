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

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public abstract class AbstractEventBusLinkTest {

  private static final List<VertxProcess> PROCESSES = new ArrayList<>();

  protected static final int[] HTTP_PORTS = {8081, 8082, 8083, 8181, 8182, 8183};

  @BeforeAll
  static void beforeAll() throws Exception {
    PROCESSES.add(VertxProcess.startNode("vertx3-app.jar", 26185, 27186, 8081));
    PROCESSES.add(VertxProcess.startNode("vertx3-app.jar", 26186, 27187, 8082));
    PROCESSES.add(VertxProcess.startNode("vertx3-app.jar", 26187, 27185, 8083));
    PROCESSES.add(VertxProcess.startNode("vertx4-app.jar", 27185, 26187, 8181));
    PROCESSES.add(VertxProcess.startNode("vertx4-app.jar", 27186, 26185, 8182));
    PROCESSES.add(VertxProcess.startNode("vertx4-app.jar", 27187, 26186, 8183));
    CompletableFuture<?>[] futures = PROCESSES.stream().map(VertxProcess::ready).toArray(CompletableFuture[]::new);
    CompletableFuture.allOf(futures).get(1, TimeUnit.MINUTES);
  }

  @AfterAll
  static void afterAll() {
    for (VertxProcess process : PROCESSES) {
      process.nuProcess().destroy(true);
    }
  }
}
