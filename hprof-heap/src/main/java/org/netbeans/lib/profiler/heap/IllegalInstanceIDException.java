/**
 * Copyright 2016 Alexey Ragozin
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
package org.netbeans.lib.profiler.heap;

/**
 * Throws by {@link Heap#getInstanceByID(long)} and similar method
 * if referenced instance cannot be found in dump.
 * <br>
 * Only for {@link FastHprofHeap} implementation.
 * 
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 *
 */
public class IllegalInstanceIDException extends IllegalArgumentException {

    private static final long serialVersionUID = 20160114L;

    public IllegalInstanceIDException() {
        super();
    }

    public IllegalInstanceIDException(String message, Throwable cause) {
        super(message, cause);
    }

    public IllegalInstanceIDException(String s) {
        super(s);
    }

    public IllegalInstanceIDException(Throwable cause) {
        super(cause);
    }
}
