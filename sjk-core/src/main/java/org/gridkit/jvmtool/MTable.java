/**
 * Copyright 2014-2018 Alexey Ragozin
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
package org.gridkit.jvmtool;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.gridkit.util.formating.TextTable;

public class MTable {

	private List<String> header = new ArrayList<String>();
	private List<String[]> rows = new ArrayList<String[]>();
	
	public boolean isEmpty() {
		return rows.isEmpty();
	}
	
	public void append(String[] hdr, String[] values) {
		for(String h: hdr) {
			if (!header.contains(h)) {
				header.add(h);
			}
		}
		String[] row = new String[header.size()];
		for(int i = 0; i != hdr.length; ++i) {
			int n = header.indexOf(hdr[i]);
			row[n] = values[i];
		}
		rows.add(row);
	}
	
	public void export(TextTable table) {
		String[] hdr = header.toArray(new String[0]);
		table.addRow(hdr);
		for(String[] r: rows) {
			String[] rr = Arrays.copyOf(r, hdr.length);
			table.addRow(rr);
		}
	}	
}
