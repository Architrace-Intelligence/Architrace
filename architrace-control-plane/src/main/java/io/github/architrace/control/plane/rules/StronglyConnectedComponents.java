/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.architrace.control.plane.rules;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

final class StronglyConnectedComponents {

    private final Map<String, SortedSet<String>> successors;
    private final Map<String, Integer> index = new HashMap<>();
    private final Map<String, Integer> lowLink = new HashMap<>();
    private final Deque<String> stack = new ArrayDeque<>();
    private final Set<String> onStack = new HashSet<>();
    private final List<List<String>> components = new ArrayList<>();
    private int counter;

    private StronglyConnectedComponents(Map<String, SortedSet<String>> successors) {
        this.successors = successors;
    }

    static List<List<String>> of(Map<String, SortedSet<String>> successors) {
        StronglyConnectedComponents search = new StronglyConnectedComponents(successors);
        for (String node : new TreeSet<>(successors.keySet())) {
            if (!search.index.containsKey(node)) {
                search.visit(node);
            }
        }
        return search.components.stream()
                .map(component -> component.stream().sorted().toList())
                .toList();
    }

    private void visit(String node) {
        index.put(node, counter);
        lowLink.put(node, counter);
        counter++;
        stack.push(node);
        onStack.add(node);
        for (String next : successors.getOrDefault(node, Collections.emptySortedSet())) {
            if (!index.containsKey(next)) {
                visit(next);
                lowLink.merge(node, lowLink.get(next), Math::min);
            } else if (onStack.contains(next)) {
                lowLink.merge(node, index.get(next), Math::min);
            }
        }
        if (lowLink.get(node).equals(index.get(node))) {
            components.add(popComponent(node));
        }
    }

    private List<String> popComponent(String root) {
        List<String> component = new ArrayList<>();
        String member;
        do {
            member = stack.pop();
            onStack.remove(member);
            component.add(member);
        } while (!member.equals(root));
        return component;
    }
}
