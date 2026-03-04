/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmitry Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.architrace.model;

public class TopicNode implements GraphNode {

  private final String topicName;

  public TopicNode(String topicName) {
    this.topicName = topicName;
  }

  @Override
  public String id() {
    return "topic:" + topicName;
  }

  public String topicName() {
    return topicName;
  }
}