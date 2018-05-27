/**
 * Copyright 2017 Alexey Ragozin
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gridkit.jvmtool.gcmon;

public interface GarbageCollectionSummary {

    public long timestamp();

    public long duration();

    public String collectorName();

    public long collectionCount();

    public long collectionTotalTime();

    public Iterable<String> memorySpaces();

    public long memoryBefore(String spaceId);

    public long memoryAfter(String spaceId);

    public long memoryMax(String spaceId);

    public String memorySpaceName(String spaceId);

}
