/* Copyright 2018 Telstra Open Source
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package org.openkilda.wfm.topology.ping.model;

import org.openkilda.wfm.topology.ping.model.FlowObserver.FlowObserverBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

public class FlowObserversPool {
    private final FlowObserver.FlowObserverBuilder builder;

    private HashMap<String, Batch> pool = new HashMap<>();

    public FlowObserversPool(FlowObserverBuilder builder) {
        this.builder = builder;
    }

    public FlowObserver get(String flowId, long cookie) {
        Batch batch = getBatch(flowId);
        return batch.computeIfAbsent(cookie, k -> builder.build());
    }

    public List<FlowObserver> getAll() {
        ArrayList<FlowObserver> all = new ArrayList<>();
        ArrayList<String> empty = new ArrayList<>();

        for (String flowId : pool.keySet()) {
            boolean isEmpty = true;
            Iterator<Entry<Long, FlowObserver>> iter;
            Batch batch = pool.get(flowId);
            for (iter = batch.entrySet().iterator(); iter.hasNext(); ) {
                Entry<Long, FlowObserver> entry = iter.next();
                if (entry.getValue().isGarbage()) {
                    iter.remove();
                    continue;
                }

                isEmpty = false;
                all.add(entry.getValue());
            }

            if (isEmpty) {
                empty.add(flowId);
            }
        }

        for (String key : empty) {
            pool.remove(key);
        }

        return all;
    }

    public void remove(String flowId, long cookie) {
        Batch batch = pool.get(flowId);
        if (batch != null) {
            batch.remove(cookie);
        }
    }

    private Batch getBatch(String flowId) {
        return pool.computeIfAbsent(flowId, k -> new Batch());
    }

    private static class Batch extends HashMap<Long, FlowObserver> {}
}
