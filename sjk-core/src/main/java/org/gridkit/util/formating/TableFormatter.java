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

/**
 * Common API for exporting data in text oriented tables.
 * Two common cases are CSV and ASCII table output.
 * 
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 *
 */
public interface TableFormatter extends TabularDataSink {

	int colByName(String name);

	/**
	 * Add column to table using default formating.
	 */
	void addCol(String name);

	/**
	 * Add column to table using default formating.
	 */
	void addCol(String name, String displayName);

	/**
	 * Add column to table using specific formating.
	 * <p>
	 * See {@link SimpleNumberFormatter} for details.
	 */
	void addCol(String name, String displayName, String format);

	void sortNumeric(String colId, boolean desc);

	void sort(String colId, boolean desc);
	
	void format(Appendable out) throws IOException;
}
