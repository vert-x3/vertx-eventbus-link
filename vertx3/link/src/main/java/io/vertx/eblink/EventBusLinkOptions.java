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

package io.vertx.eblink;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.codegen.annotations.GenIgnore;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.NetServerOptions;

import java.util.LinkedHashSet;
import java.util.Set;

@DataObject(generateConverter = true)
public class EventBusLinkOptions {

  private static final int DEFAULT_PORT = 26185;

  private Set<String> addresses;

  private String serverHost;
  private int serverPort;
  private HttpServerOptions serverOptions;

  private String clientHost;
  private int clientPort;
  private HttpClientOptions clientOptions;

  public EventBusLinkOptions() {
    addresses = new LinkedHashSet<>();
    serverHost = NetServerOptions.DEFAULT_HOST;
    serverPort = DEFAULT_PORT;
    serverOptions = new HttpServerOptions();
    clientHost = NetServerOptions.DEFAULT_HOST;
    clientPort = DEFAULT_PORT;
    clientOptions = new HttpClientOptions();
  }

  public EventBusLinkOptions(EventBusLinkOptions other) {
    addresses = other != null ? new LinkedHashSet<>(other.addresses):new LinkedHashSet<>();
    serverHost = other != null ? other.serverHost:NetServerOptions.DEFAULT_HOST;
    serverPort = other != null ? other.serverPort:DEFAULT_PORT;
    serverOptions = other != null ? new HttpServerOptions(other.serverOptions):new HttpServerOptions();
    clientHost = other != null ? other.clientHost:NetServerOptions.DEFAULT_HOST;
    clientPort = other != null ? other.clientPort:DEFAULT_PORT;
    clientOptions = other != null ? new HttpClientOptions(other.clientOptions):new HttpClientOptions();
  }

  public EventBusLinkOptions(JsonObject json) {
    this();
    EventBusLinkOptionsConverter.fromJson(json, this);
  }

  public Set<String> getAddresses() {
    return addresses;
  }

  public EventBusLinkOptions setAddresses(Set<String> addresses) {
    this.addresses = addresses;
    return this;
  }

  @GenIgnore
  public EventBusLinkOptions addAddress(String address) {
    if (addresses == null) {
      addresses = new LinkedHashSet<>();
    }
    addresses.add(address);
    return this;
  }

  public String getServerHost() {
    return serverHost;
  }

  public EventBusLinkOptions setServerHost(String serverHost) {
    this.serverHost = serverHost;
    return this;
  }

  public int getServerPort() {
    return serverPort;
  }

  public EventBusLinkOptions setServerPort(int serverPort) {
    this.serverPort = serverPort;
    return this;
  }

  public HttpServerOptions getServerOptions() {
    return serverOptions;
  }

  public EventBusLinkOptions setServerOptions(HttpServerOptions serverOptions) {
    this.serverOptions = serverOptions;
    return this;
  }

  public String getClientHost() {
    return clientHost;
  }

  public EventBusLinkOptions setClientHost(String clientHost) {
    this.clientHost = clientHost;
    return this;
  }

  public int getClientPort() {
    return clientPort;
  }

  public EventBusLinkOptions setClientPort(int clientPort) {
    this.clientPort = clientPort;
    return this;
  }

  public HttpClientOptions getClientOptions() {
    return clientOptions;
  }

  public EventBusLinkOptions setClientOptions(HttpClientOptions clientOptions) {
    this.clientOptions = clientOptions;
    return this;
  }
}
