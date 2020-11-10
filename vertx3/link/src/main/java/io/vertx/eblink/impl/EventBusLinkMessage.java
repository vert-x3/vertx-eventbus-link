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

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.ReplyException;
import io.vertx.core.eventbus.ReplyFailure;

public class EventBusLinkMessage<T> implements Message<T> {

  private final EventBusLinkImpl eventBusLink;
  private final String replyId;
  private final String address;
  private final T body;

  public EventBusLinkMessage(EventBusLinkImpl eventBusLink, String replyId, String address, Object body) {
    this.eventBusLink = eventBusLink;
    this.replyId = replyId;
    this.address = address;
    this.body = (T) body;
  }

  @Override
  public String address() {
    return address;
  }

  @Override
  public MultiMap headers() {
    return null;
  }

  @Override
  public T body() {
    return body;
  }

  @Override
  public String replyAddress() {
    return null;
  }

  @Override
  public boolean isSend() {
    return true;
  }

  @Override
  public void reply(Object message) {
    reply(message, new DeliveryOptions());
  }

  @Override
  public <R> void reply(Object message, Handler<AsyncResult<Message<R>>> replyHandler) {
    reply(message, new DeliveryOptions(), replyHandler);
  }

  @Override
  public void reply(Object message, DeliveryOptions options) {
    if (replyId != null) {
      eventBusLink.reply(address, replyId, message, options);
    }
  }

  @Override
  public <R> void reply(Object message, DeliveryOptions options, Handler<AsyncResult<Message<R>>> replyHandler) {
    if (replyId == null) {
      throw new IllegalStateException();
    }
    eventBusLink.requestAndReply(address, replyId, message, options, replyHandler);
  }

  @Override
  public void fail(int failureCode, String message) {
    if (replyId != null) {
      eventBusLink.reply(address, replyId, new ReplyException(ReplyFailure.RECIPIENT_FAILURE, failureCode, message), null);
    }
  }
}
