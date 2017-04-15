/**
 * Copyright 2014-2017 Alexey Ragozin
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
package org.gridkit.util.formating;

public interface TabularDataSink {

    public int colByName(String name);
    
    public Cursor newCursor();  
    
    public void close();
    
    public interface Cursor {
        
        public void setCell(String name, String value);
        
        public void setCell(String name, long value);

        public void setCell(String name, double value);

        public void setCell(int colNo, String value);

        public void setCell(int colNo, long value);

        public void setCell(int colNo, double value);
        
        public void submit();
    }
}
