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
package org.gridkit.util.formating;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TimeZone;

public class AsciiTableFormatter implements TableFormatter, TabularDataSink {

    protected TextTable textTable = new TextTable();
    protected List<String> header = new ArrayList<String>();
    protected List<String> displayHeader = new ArrayList<String>();
    protected List<NumberFormat> numberFormat = new ArrayList<NumberFormat>();
    protected TimeZone timeZone;
    
    public AsciiTableFormatter() {
    	this(TimeZone.getDefault());
    }

    public AsciiTableFormatter(TimeZone timeZone) {
		this.timeZone = timeZone;
	}
    
    @Override
	public void addCol(String name) {
        header.add(name);
        displayHeader.add(name);
        numberFormat.add(SimpleNumberFormatter.DEFAULT);
    }
    
    @Override
	public void addCol(String name, String displayName) {
        header.add(name);
        displayHeader.add(displayName);
        numberFormat.add(SimpleNumberFormatter.DEFAULT);
    }

    @Override
	public void addCol(String name, String displayName, String format) {
        header.add(name);
        displayHeader.add(displayName);
        numberFormat.add(new SimpleNumberFormatter(format, timeZone));
    }
    
    protected String formatDouble(String name, double v) {
        return numberFormat.get(header.indexOf(name)).formatDouble(v);
    }

    protected String formatLong(String name, long v) {
        return numberFormat.get(header.indexOf(name)).formatLong(v);
    }

    @Override
    public int colByName(String name) {
        return header.indexOf(name);
    }

    public String format() {        
        return textTable.formatTextTable(512);
    }
    
    @Override
	public void format(Appendable out) throws IOException {
    	out.append(format());
	}

	@Override
	public void sort(String colId, boolean desc) {
        textTable.sort(colByName(colId), true, desc, null);
    }

    @Override
	public void sortNumeric(String colId, boolean desc) {
        textTable.sort(colByName(colId), true, desc, TextTable.NUM_CMP);
    }
    
    @Override
    public void close() {
        // do nothing
    }

    protected void addRow(String[] row) {
        if (textTable.rowCount() == 0) {
            textTable.addRow(displayHeader.toArray(new String[0]));
        }
        textTable.addRow(row);
    }
    
    @Override
    public Cursor newCursor() {
        return new Cursor() {
            
            final String[] row = new String[header.size()];
            {
                clear();
            }
            
            @Override
            public void submit() {
                addRow(row);
                clear();
            }
            
            private void clear() {
                Arrays.fill(row, "");                
            }

            @Override
            public void setCell(int colNo, double value) {
                if (colNo >= 0) {
                    row[colNo] = formatDouble(header.get(colNo), value);
                }                   
            }
            
            @Override
            public void setCell(int colNo, long value) {
                if (colNo >= 0) {
                    row[colNo] = formatLong(header.get(colNo), value);
                }                   
            }
            
            @Override
            public void setCell(int colNo, String value) {
                if (colNo >= 0) {
                    row[colNo] = value;
                }                   
            }
            
            @Override
            public void setCell(String name, double value) {
                setCell(colByName(name), value);
            }
            
            @Override
            public void setCell(String name, long value) {
                setCell(colByName(name), value);
            }
            
            @Override
            public void setCell(String name, String value) {
                setCell(colByName(name), value);
            }
        };
    }
}
