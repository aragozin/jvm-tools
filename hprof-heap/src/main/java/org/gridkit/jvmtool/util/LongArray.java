package org.gridkit.jvmtool.util;
/**
 * Copyright 2014 Alexey Ragozin
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


/**
 * Minimal API for growable array of 64 bit integers.
 * 
 * @author Alexey Ragozin (alexey.ragozin@db.com)
 */
interface LongArray {

    public long get(long n);
    public long seekNext(long start);
    public void set(long n, long value);
    
}
