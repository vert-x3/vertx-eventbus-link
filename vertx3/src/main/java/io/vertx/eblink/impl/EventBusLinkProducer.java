/*
 * Copyright 2021 Red Hat, Inc.
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

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.MessageProducer;
import io.vertx.eblink.EventBusLink;

public class EventBusLinkProducer<T> implements MessageProducer<T> {

  private final EventBusLink eventBusLink;
  private final String address;
  private final boolean send;

  private DeliveryOptions options;

  public EventBusLinkProducer(EventBusLink eventBusLink, String address, boolean send, DeliveryOptions options) {
    this.eventBusLink = eventBusLink;
    this.address = address;
    this.send = send;
    this.options = options;
  }

  @Override
  public MessageProducer<T> send(T message) {
    return write(message);
  }

  @Override
  public <R> MessageProducer<T> send(T message, Handler<AsyncResult<Message<R>>> replyHandler) {
    eventBusLink.request(address, message, replyHandler);
    return this;
  }

  @Override
  public MessageProducer<T> exceptionHandler(Handler<Throwable> handler) {
    return this;
  }

  @Override
  public MessageProducer<T> write(T data) {
    write(data, null);
    return this;
  }

  @Override
  public MessageProducer<T> write(T data, Handler<AsyncResult<Void>> handler) {
    if (send) {
      eventBusLink.send(address, data, options);
    } else {
      eventBusLink.publish(address, data, options);

    }
    if (handler != null) {
      handler.handle(Future.succeededFuture());
    }
    return this;
  }

  @Override
  public MessageProducer<T> setWriteQueueMaxSize(int maxSize) {
    return this;
  }

  @Override
  public boolean writeQueueFull() {
    return false;
  }

  @Override
  public MessageProducer<T> drainHandler(Handler<Void> handler) {
    return this;
  }

  @Override
  public MessageProducer<T> deliveryOptions(DeliveryOptions options) {
    this.options = options;
    return this;
  }

  @Override
  public String address() {
    return address;
  }

  @Override
  public void end() {
  }

  @Override
  public void end(Handler<AsyncResult<Void>> handler) {
    handler.handle(Future.succeededFuture());
  }

  @Override
  public void close() {
  }

  @Override
  public void close(Handler<AsyncResult<Void>> handler) {
    handler.handle(Future.succeededFuture());
  }
}
