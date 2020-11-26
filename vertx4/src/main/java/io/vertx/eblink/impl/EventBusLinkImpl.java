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
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.*;
import io.vertx.core.eventbus.impl.CodecManager;
import io.vertx.core.eventbus.impl.MessageImpl;
import io.vertx.core.http.*;
import io.vertx.core.impl.ContextInternal;
import io.vertx.core.impl.VertxInternal;
import io.vertx.core.json.JsonObject;
import io.vertx.eblink.EventBusLink;
import io.vertx.eblink.EventBusLinkOptions;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;

public class EventBusLinkImpl implements EventBusLink, Handler<ServerWebSocket> {

  private static final AtomicReference<EventBusLinkImpl> INSTANCE = new AtomicReference<>();

  private final VertxInternal vertx;
  private final EventBusLinkOptions options;
  private final EventBus delegate;
  private final Set<String> addresses;
  private final Promise<EventBusLink> setupPromise;
  private final CodecManager codecManager;
  private final HttpServer server;
  private final HttpClient client;
  private final ConcurrentMap<String, Object> replyContexts;

  private WebSocket webSocket;

  public EventBusLinkImpl(VertxInternal vertx, EventBusLinkOptions options) {
    this.vertx = vertx;
    this.options = options;
    delegate = vertx.eventBus();
    addresses = options.getAddresses() != null ? options.getAddresses():Collections.emptySet();
    setupPromise = Promise.promise();
    codecManager = new CodecManager();
    server = vertx.createHttpServer(options.getServerOptions()).webSocketHandler(this);
    HttpClientOptions clientOptions = options.getClientOptions()
      .setDefaultHost(options.getClientHost())
      .setDefaultPort(options.getClientPort());
    client = vertx.createHttpClient(clientOptions);
    replyContexts = new ConcurrentHashMap<>();
  }

  public static Future<EventBusLink> create(VertxInternal vertx, EventBusLinkOptions options) {
    EventBusLinkImpl link = new EventBusLinkImpl(vertx, options);
    if (INSTANCE.compareAndSet(null, link)) {
      link.init();
    } else {
      link = INSTANCE.get();
    }
    Promise<EventBusLink> promise = vertx.getOrCreateContext().promise();
    link.setupPromise.future().onComplete(promise);
    return promise.future();
  }

  private void init() {
    server.listen(options.getServerPort(), options.getServerHost())
      .<EventBusLink>map(this)
      .onComplete(setupPromise);
  }

  @Override
  public Future<Void> close() {
    // FIXME close the link properly
    return Future.failedFuture("Not implemented yet");
  }

  @Override
  public void handle(ServerWebSocket serverWebSocket) {
    serverWebSocket.binaryMessageHandler(buffer -> {
      JsonObject json = buffer.toJsonObject();
      String address = json.getString("address");
      Boolean send = json.getBoolean("send");
      String replyId = json.getString("replyId");
      String replyTo = json.getString("replyTo");
      DeliveryOptions options = new DeliveryOptions(json.getJsonObject("options"));
      String codec = json.getString("codec");
      Buffer body = Buffer.buffer(json.getBinary("body"));
      Object msg = forDecoding(codecManager, codec).decodeFromWire(0, body);
      if (replyId != null) {
        Message<Object> message = (Message<Object>) replyContexts.remove(replyId);
        if (msg instanceof ReplyException) {
          ReplyException e = (ReplyException) msg;
          message.fail(e.failureCode(), e.getMessage());
        } else if (replyTo == null) {
          message.reply(msg, options);
        } else {
          message.replyAndRequest(msg, options, handleReplyFromThisCluster(serverWebSocket, address, replyTo));
        }
      } else if (send == Boolean.TRUE) {
        if (replyTo == null) {
          vertx.eventBus().send(address, msg, options);
        } else {
          vertx.eventBus().request(address, msg, options, handleReplyFromThisCluster(serverWebSocket, address, replyTo));
        }
      } else {
        vertx.eventBus().publish(address, msg, options);
      }
    });
  }

  private MessageCodec forDecoding(CodecManager codecManager, String codec) {
    // FIXME handle all codecs correctly
    return Arrays.stream(codecManager.systemCodecs()).filter(mc -> mc.name().equals(codec)).findFirst().get();
  }

  private Handler<AsyncResult<Message<Object>>> handleReplyFromThisCluster(ServerWebSocket serverWebSocket, String address, String replyId) {
    return ar -> {
      JsonObject jsonObject = new JsonObject();
      jsonObject.put("address", address).put("replyId", replyId);
      MessageCodec messageCodec;
      Object body;
      if (ar.succeeded()) {
        MessageImpl<Object, Object> message = (MessageImpl<Object, Object>) ar.result();
        if (message.replyAddress() != null) {
          String replyTo = storeReplyContext(message, 30 * 1000, null);
          jsonObject.put("replyTo", replyTo);
        }
        body = message.body();
        messageCodec = forEncoding(null, body);
      } else {
        body = ar.cause();
        messageCodec = CodecManager.REPLY_EXCEPTION_MESSAGE_CODEC;
      }
      Buffer buffer = Buffer.buffer();
      jsonObject.put("codec", messageCodec.name());
      messageCodec.encodeToWire(buffer, body);
      jsonObject.put("body", buffer);
      writeBinaryMessage(serverWebSocket, jsonObject);
    };
  }

  private MessageCodec forEncoding(String codec, Object body) {
    // FIXME handle all codecs correctly
    return codecManager.lookupCodec(body, codec);
  }

  private <T> String storeReplyContext(Object ctx, long timeout, Promise<Message<T>> promise) {
    String id = UUID.randomUUID().toString();
    replyContexts.put(id, ctx);
    vertx.setTimer(timeout, l -> {
      Object o = replyContexts.remove(id);
      if (o != null && promise != null) {
        promise.tryFail(new ReplyException(ReplyFailure.TIMEOUT));
      }
    });
    return id;
  }

  @Override
  public EventBus send(String address, Object message) {
    return send(address, message, new DeliveryOptions());
  }

  @Override
  public EventBus send(String address, Object message, DeliveryOptions options) {
    if (addresses.contains(address)) {
      JsonObject json = new JsonObject()
        .put("address", address)
        .put("send", Boolean.TRUE)
        .put("options", options.toJson());
      MessageCodec messageCodec = forEncoding(options.getCodecName(), message);
      json.put("codec", messageCodec.name());
      Buffer buffer = Buffer.buffer();
      messageCodec.encodeToWire(buffer, message);
      json.put("body", buffer);
      connect(ar -> {
        if (ar.succeeded()) {
          writeBinaryMessage(ar.result(), json);
        }
      });
    } else {
      delegate.send(address, message, options);
    }
    return this;
  }

  private void connect(Handler<AsyncResult<WebSocket>> handler) {
    if (webSocket == null) {
      client.webSocket("/", ar -> {
        if (ar.succeeded()) {
          if (webSocket == null) {
            webSocket = ar.result();
            webSocket.closeHandler(v -> webSocket = null).binaryMessageHandler(this::handleReplyFromOtherCluster);
          } else {
            ar.result().close();
          }
          handler.handle(Future.succeededFuture(webSocket));
        } else {
          handler.handle(Future.failedFuture(ar.cause()));
        }
      });
    } else {
      handler.handle(Future.succeededFuture(webSocket));
    }
  }

  private void handleReplyFromOtherCluster(Buffer buffer) {
    JsonObject json = buffer.toJsonObject();
    String replyId = json.getString("replyId");
    Handler<JsonObject> handler = (Handler<JsonObject>) replyContexts.remove(replyId);
    if (handler != null) {
      handler.handle(json);
    }
  }

  @Override
  public <T> Future<Message<T>> request(String address, Object message) {
    return request(address, message, new DeliveryOptions());
  }

  @Override
  public <T> Future<Message<T>> request(String address, Object message, DeliveryOptions options) {
    if (addresses.contains(address)) {
      ContextInternal context = vertx.getOrCreateContext();
      Promise<Message<T>> promise = context.promise();
      Handler<JsonObject> handler = json -> {
        String replyTo = json.getString("replyTo");
        MessageCodec messageCodec = forDecoding(codecManager, json.getString("codec"));
        Object body = messageCodec.decodeFromWire(0, json.getBuffer("body"));
        if (body instanceof ReplyException) {
          ReplyException e = (ReplyException) body;
          promise.tryFail(e);
        } else {
          promise.tryComplete(new EventBusLinkMessage<>(this, replyTo, address, body));
        }
      };
      String replyTo = storeReplyContext(handler, options.getSendTimeout(), promise);
      JsonObject json = new JsonObject()
        .put("address", address)
        .put("send", Boolean.TRUE)
        .put("replyTo", replyTo)
        .put("options", options.toJson());
      MessageCodec messageCodec = forEncoding(options.getCodecName(), message);
      json.put("codec", messageCodec.name());
      Buffer buffer = Buffer.buffer();
      messageCodec.encodeToWire(buffer, message);
      json.put("body", buffer);
      connect(ar -> {
        if (ar.succeeded()) {
          writeBinaryMessage(ar.result(), json);
        }
      });
      return promise.future();
    }
    return delegate.request(address, message, options);
  }

  private void writeBinaryMessage(WebSocketBase ws, JsonObject json) {
    ws.writeBinaryMessage(json.toBuffer(), war -> {
      if (war.failed()) {
        war.cause().printStackTrace();
      }
    });
  }

  @Override
  public EventBus publish(String address, Object message) {
    return publish(address, message, new DeliveryOptions());
  }

  @Override
  public EventBus publish(String address, Object message, DeliveryOptions options) {
    JsonObject json = new JsonObject()
      .put("address", address)
      .put("send", Boolean.FALSE)
      .put("options", options.toJson());
    MessageCodec messageCodec = forEncoding(options.getCodecName(), message);
    json.put("codec", messageCodec.name());
    Buffer buffer = Buffer.buffer();
    messageCodec.encodeToWire(buffer, message);
    json.put("body", buffer);
    connect(ar -> {
      if (ar.succeeded()) {
        writeBinaryMessage(ar.result(), json);
      }
    });
    delegate.publish(address, message, options);
    return this;
  }

  @Override
  public <T> MessageConsumer<T> consumer(String address) {
    return delegate.consumer(address);
  }

  @Override
  public <T> MessageConsumer<T> consumer(String address, Handler<Message<T>> handler) {
    return delegate.consumer(address, handler);
  }

  @Override
  public <T> MessageConsumer<T> localConsumer(String address) {
    return delegate.localConsumer(address);
  }

  @Override
  public <T> MessageConsumer<T> localConsumer(String address, Handler<Message<T>> handler) {
    return delegate.localConsumer(address, handler);
  }

  @Override
  public <T> MessageProducer<T> sender(String address) {
    return sender(address, new DeliveryOptions());
  }

  @Override
  public <T> MessageProducer<T> sender(String address, DeliveryOptions options) {
    // FIXME
    return delegate.sender(address, options);
  }

  @Override
  public <T> MessageProducer<T> publisher(String address) {
    return publisher(address, new DeliveryOptions());
  }

  @Override
  public <T> MessageProducer<T> publisher(String address, DeliveryOptions options) {
    // FIXME
    return delegate.publisher(address, options);
  }

  @Override
  public EventBus registerCodec(MessageCodec codec) {
    codecManager.registerCodec(codec);
    delegate.registerCodec(codec);
    return this;
  }

  @Override
  public EventBus unregisterCodec(String name) {
    codecManager.unregisterCodec(name);
    delegate.unregisterCodec(name);
    return this;
  }

  @Override
  public <T> EventBus registerDefaultCodec(Class<T> clazz, MessageCodec<T, ?> codec) {
    codecManager.registerDefaultCodec(clazz, codec);
    delegate.registerDefaultCodec(clazz, codec);
    return this;
  }

  @Override
  public EventBus unregisterDefaultCodec(Class clazz) {
    codecManager.unregisterDefaultCodec(clazz);
    delegate.unregisterDefaultCodec(clazz);
    return this;
  }

  @Override
  public <T> EventBus addOutboundInterceptor(Handler<DeliveryContext<T>> interceptor) {
    delegate.addOutboundInterceptor(interceptor);
    return this;
  }

  @Override
  public <T> EventBus removeOutboundInterceptor(Handler<DeliveryContext<T>> interceptor) {
    delegate.removeOutboundInterceptor(interceptor);
    return this;
  }

  @Override
  public <T> EventBus addInboundInterceptor(Handler<DeliveryContext<T>> interceptor) {
    delegate.addInboundInterceptor(interceptor);
    return this;
  }

  @Override
  public <T> EventBus removeInboundInterceptor(Handler<DeliveryContext<T>> interceptor) {
    delegate.removeInboundInterceptor(interceptor);
    return this;
  }

  @Override
  public boolean isMetricsEnabled() {
    return delegate.isMetricsEnabled();
  }

  void reply(String address, String replyId, Object message, DeliveryOptions options) {
    JsonObject json = new JsonObject().put("address", address);
    if (options != null) {
      json.put("options", options.toJson());
    }
    MessageCodec messageCodec = forEncoding(options.getCodecName(), message);
    json.put("codec", messageCodec.name());
    Buffer buffer = Buffer.buffer();
    messageCodec.encodeToWire(buffer, message);
    json.put("body", buffer);
    json.put("replyId", replyId);
    if (webSocket != null) {
      writeBinaryMessage(webSocket, json);
    }
  }

  <R> Future<Message<R>> requestAndReply(String address, String replyId, Object message, DeliveryOptions options) {
    ContextInternal context = vertx.getOrCreateContext();
    Promise<Message<R>> promise = context.promise();
    Handler<JsonObject> handler = json -> {
      String replyTo = json.getString("replyTo");
      MessageCodec messageCodec = forDecoding(codecManager, json.getString("codec"));
      Object body = messageCodec.decodeFromWire(0, json.getBuffer("body"));
      if (body instanceof ReplyException) {
        ReplyException e = (ReplyException) body;
        promise.tryFail(e);
      } else {
        promise.tryComplete(new EventBusLinkMessage<>(this, replyTo, address, body));
      }
    };
    String replyTo = storeReplyContext(handler, options.getSendTimeout(), promise);
    JsonObject json = new JsonObject()
      .put("address", address)
      .put("send", Boolean.TRUE)
      .put("replyId", replyId)
      .put("replyTo", replyTo)
      .put("options", options.toJson());
    MessageCodec messageCodec = forEncoding(options.getCodecName(), message);
    json.put("codec", messageCodec.name());
    Buffer buffer = Buffer.buffer();
    messageCodec.encodeToWire(buffer, message);
    json.put("body", buffer);
    if (webSocket != null) {
      writeBinaryMessage(webSocket, json);
    }
    return promise.future();
  }

}
