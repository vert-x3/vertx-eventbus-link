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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

public enum ClusterHelper {

  INSTANCE;

  private final List<ClusterNode> vertx3Nodes;
  private final List<ClusterNode> vertx4Nodes;
  private final List<VertxProcess> processes;

  ClusterHelper() {
    int clusterSize = Integer.getInteger("default.cluster.size", 3);
    if (clusterSize < 1) throw new IllegalArgumentException();
    vertx3Nodes = Collections.synchronizedList(new ArrayList<>(clusterSize));
    vertx4Nodes = Collections.synchronizedList(new ArrayList<>(clusterSize));
    processes = Collections.synchronizedList(new ArrayList<>(2 * clusterSize));
    for (int i = 0; i < clusterSize; i++) {
      vertx3Nodes.add(new ClusterNode("vertx3-app", 8081 + i, 26185 + i, 27185 + ((i + 1) % clusterSize)));
      vertx4Nodes.add(new ClusterNode("vertx4-app", 8181 + i, 27185 + i, 26185 + ((i + 2) % clusterSize)));
    }
  }

  public void startClusters() throws Exception {
    for (ClusterNode clusterNode : vertx3Nodes) {
      processes.add(clusterNode.start());
    }
    for (ClusterNode clusterNode : vertx4Nodes) {
      processes.add(clusterNode.start());
    }
    CompletableFuture<?>[] futures = processes.stream().map(VertxProcess::ready).toArray(CompletableFuture[]::new);
    CompletableFuture.allOf(futures).get(1, TimeUnit.MINUTES);
  }

  public void stopClusters() {
    for (VertxProcess process : processes) {
      process.nuProcess().destroy(true);
    }
  }

  public Stream<ClusterNode> clusterNodes() {
    return Stream.concat(INSTANCE.vertx3Nodes.stream(), INSTANCE.vertx4Nodes.stream());
  }

  public int[] httpServerPorts() {
    return clusterNodes().mapToInt(ClusterNode::getHttpServerPort).toArray();
  }
}
