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

package io.vertx.eblink.test;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.MessageCodec;

public class CustomTypeCodec implements MessageCodec<CustomType, CustomType> {

  @Override
  public void encodeToWire(Buffer buffer, CustomType customType) {
    String value = customType.toString();
    buffer.appendInt(value.length());
    buffer.appendString(value);
  }

  @Override
  public CustomType decodeFromWire(int pos, Buffer buffer) {
    int len = buffer.getInt(pos);
    pos += 4;
    return new CustomType(buffer.getString(pos, pos + len));
  }

  @Override
  public CustomType transform(CustomType customType) {
    return customType;
  }

  @Override
  public String name() {
    return "customType";
  }

  @Override
  public byte systemCodecID() {
    return -1;
  }
}
