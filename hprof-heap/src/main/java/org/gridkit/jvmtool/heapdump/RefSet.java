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
package org.gridkit.jvmtool.heapdump;

import org.gridkit.jvmtool.util.PagedBitMap;



/**
 * This is thin wrapper around {@link PagedBitMap}
 * enforcing 8 byte index alignment (and compressing indexes in underlying bitmap).
 *
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
public class RefSet extends PagedBitMap {

    @Override
    public boolean get(long index) {
        if (index % 8 != 0) {
            throw new IllegalArgumentException("" + index);
        }
        return super.get(index / 8);
    }

    @Override
    public long seekNext(long index) {
        index = (index + 7) / 8; // alligning to 8 byte boundary
        return 8 * super.seekNext(index);
    }

    @Override
    public boolean getAndSet(long index, boolean value) {
        if (index % 8 != 0) {
            throw new IllegalArgumentException("" + index);
        }
        return super.getAndSet(index / 8, value);
    }

    @Override
    public void set(long index, boolean value) {
        if (index % 8 != 0) {
            throw new IllegalArgumentException("" + index);
        }
        super.set(index / 8, value);
    }

    @Override
    public void sub(PagedBitMap that) {
        super.sub(that);
    }
}
