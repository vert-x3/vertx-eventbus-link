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

package io.vertx.eblink.impl;

import io.vertx.codegen.annotations.Nullable;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.*;
import io.vertx.core.eventbus.impl.CodecManager;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.ServerWebSocket;
import io.vertx.core.impl.ContextInternal;
import io.vertx.core.impl.VertxInternal;
import io.vertx.eblink.EventBusLinkOptions;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

public class EventBusLinkImpl implements EventBus, Handler<ServerWebSocket> {

  private static final AtomicReference<EventBusLinkImpl> INSTANCE = new AtomicReference<>();

  private final VertxInternal vertx;
  private final EventBusLinkOptions options;
  private final Set<String> addresses;
  private final Promise<EventBus> setupPromise;
  private final CodecManager codecManager;
  private final HttpServer server;
  private final HttpClient client;

  public EventBusLinkImpl(VertxInternal vertx, EventBusLinkOptions options) {
    this.vertx = vertx;
    this.options = options;
    addresses = options.getAddresses() != null ? options.getAddresses():Collections.emptySet();
    setupPromise = Promise.promise();
    codecManager = new CodecManager();
    server = vertx.createHttpServer(options.getServerOptions()).webSocketHandler(this);
    client = vertx.createHttpClient(options.getClientOptions());
  }

  public static void create(VertxInternal vertx, EventBusLinkOptions options, Handler<AsyncResult<EventBus>> resultHandler) {
    EventBusLinkImpl link = new EventBusLinkImpl(vertx, options);
    if (INSTANCE.compareAndSet(null, link)) {
      link.init();
    } else {
      link = INSTANCE.get();
    }
    ContextInternal context = vertx.getOrCreateContext();
    link.setupPromise.future().onComplete(ar -> {
      context.runOnContext(v -> resultHandler.handle(ar));
    });
  }

  private void init() {
    server.listen(options.getServerPort(), options.getServerHost(), ar -> {
      if (ar.succeeded()) {
        setupPromise.complete(this);
      } else {
        setupPromise.fail(ar.cause());
      }
    });
  }

  @Override
  public void handle(ServerWebSocket event) {

  }

  @Override
  public EventBus send(String s, @Nullable Object o) {
    return null;
  }

  @Override
  public <T> EventBus send(String s, @Nullable Object o, Handler<AsyncResult<Message<T>>> handler) {
    return null;
  }

  @Override
  public EventBus send(String s, @Nullable Object o, DeliveryOptions deliveryOptions) {
    return null;
  }

  @Override
  public <T> EventBus send(String s, @Nullable Object o, DeliveryOptions deliveryOptions, Handler<AsyncResult<Message<T>>> handler) {
    return null;
  }

  @Override
  public EventBus publish(String s, @Nullable Object o) {
    return null;
  }

  @Override
  public EventBus publish(String s, @Nullable Object o, DeliveryOptions deliveryOptions) {
    return null;
  }

  @Override
  public <T> MessageConsumer<T> consumer(String s) {
    return null;
  }

  @Override
  public <T> MessageConsumer<T> consumer(String s, Handler<Message<T>> handler) {
    return null;
  }

  @Override
  public <T> MessageConsumer<T> localConsumer(String s) {
    return null;
  }

  @Override
  public <T> MessageConsumer<T> localConsumer(String s, Handler<Message<T>> handler) {
    return null;
  }

  @Override
  public <T> MessageProducer<T> sender(String s) {
    return null;
  }

  @Override
  public <T> MessageProducer<T> sender(String s, DeliveryOptions deliveryOptions) {
    return null;
  }

  @Override
  public <T> MessageProducer<T> publisher(String s) {
    return null;
  }

  @Override
  public <T> MessageProducer<T> publisher(String s, DeliveryOptions deliveryOptions) {
    return null;
  }

  @Override
  public EventBus registerCodec(MessageCodec messageCodec) {
    return null;
  }

  @Override
  public EventBus unregisterCodec(String s) {
    return null;
  }

  @Override
  public <T> EventBus registerDefaultCodec(Class<T> aClass, MessageCodec<T, ?> messageCodec) {
    return null;
  }

  @Override
  public EventBus unregisterDefaultCodec(Class aClass) {
    return null;
  }

  @Override
  public void start(Handler<AsyncResult<Void>> handler) {

  }

  @Override
  public void close(Handler<AsyncResult<Void>> handler) {

  }

  @Override
  public <T> EventBus addOutboundInterceptor(Handler<DeliveryContext<T>> handler) {
    return null;
  }

  @Override
  public <T> EventBus removeOutboundInterceptor(Handler<DeliveryContext<T>> handler) {
    return null;
  }

  @Override
  public <T> EventBus addInboundInterceptor(Handler<DeliveryContext<T>> handler) {
    return null;
  }

  @Override
  public <T> EventBus removeInboundInterceptor(Handler<DeliveryContext<T>> handler) {
    return null;
  }
}
