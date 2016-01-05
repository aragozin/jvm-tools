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
package org.gridkit.util.formating;


import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
public class TextTable {

	private List<String[]> rows = new ArrayList<String[]>();
	private int colCount;
	
	public void transpone() {
		int rc = rows.size();
		int cc = colCount;
		
		List<String[]> nrows = new ArrayList<String[]>();
		for(int i = 0; i != cc; ++i) {
			String[] nrow = new String[cc];
			for(int j = 0; j != rc; ++i) {
				nrow[j] = rows.get(j)[i];
			}
			nrows.add(nrow);
		}
		
		rows = nrows;
		colCount = rc;
	}
	
	public void addRow(String... row) {
		addRow(row, false);
	}

	public void addRow(List<String> row) {
		addRow(row.toArray(new String[row.size()]), false);
	}

	public void addRow(List<String> row, boolean autoGrow) {
		addRow(row.toArray(new String[row.size()]), autoGrow);
	}

	public void addRow(String[] row, boolean autoGrow) {
		if (rows.size() == 0) {
			colCount = row.length;
		}
		if (row.length > colCount) {
			if (autoGrow) {
				extendRows(row.length);
			}
			else {
				throw new IllegalArgumentException("Row is longer than table");
			}
		}
		rows.add(Arrays.copyOf(row, colCount));	
	}

	private void extendRows(int length) {
		for(int i = 0; i != rows.size(); ++i) {
			rows.set(i, Arrays.copyOf(rows.get(i), length));
		}		
		colCount = length;
	}

	public void addColumnRight(List<String> col) {
		addColumnRight(col.toArray(new String[col.size()]));
	}
	
	public void addColumnRight(String... col) {
		if (col.length > rows.size()) {
			throw new IllegalArgumentException("Column is taller than table");
		}
		colCount += 1;
		for(int i = 0; i != rows.size(); ++i) {
			String[] row = rows.get(i);
			row = Arrays.copyOf(row, colCount);
			if (col.length > i) {
				row[colCount - 1] = col[i];
			}
			rows.set(i, row);
		}
	}

	public void addColumnLeft(List<String> col) {
		addColumnLeft(col.toArray(new String[col.size()]));
	}
	
	public void addColumnLeft(String[] col) {
		if (col.length > rows.size()) {
			throw new IllegalArgumentException("Column is taller than table");
		}
		colCount += 1;
		for(int i = 0; i != rows.size(); ++i) {
			String[] row = rows.get(i);
			String[] nrow = new String[colCount];
			System.arraycopy(row, 0, nrow, 1, row.length);
			if (col.length > i) {
				nrow[0] = col[i];
			}
			rows.set(i, nrow);
		}
	}
	
	public String formatTextTable(int maxCellWidth) {
		return formatTable(rows, maxCellWidth, true);
	}

    public String formatToCSV() {
        try {
            StringWriter sw = new StringWriter();
            CSVWriter writer = new CSVWriter(sw, ',', '"', '"', "\n");
            for(String[] row: rows) {
                writer.writeNext(row);
            }
            writer.close();
            return sw.toString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

	public String formatTextTableUnbordered(int maxCellWidth) {
		return formatTable(rows, maxCellWidth, false);
	}
	
	private String formatTable(List<String[]> content, int maxCell, boolean table) {
		int[] width = new int[content.get(0).length];
		for(String[] row: content) {
			for(int i = 0; i != row.length; ++i) {
				width[i] = Math.min(Math.max(width[i], measureCell(row[i])), maxCell);
			}
		}

		StringBuilder sb = new StringBuilder();
		boolean header = table;
		for(String[] row: content) {
			for(int i = 0; i != width.length; ++i) {
				renderCell(sb, row[i], width[i]);
				if (table) {
					sb.append('|');
				}
			}
			if (table) {
				sb.setLength(sb.length() - 1);
			}
			sb.append('\n');
			if (header) {
				header = false;
				for(int n: width) {
					for(int i = 0; i != n; ++i) {
						sb.append('-');
					}
					sb.append('+');
				}
				sb.setLength(sb.length() - 1);
				sb.append('\n');
			}
		}
		
		return sb.toString();
	}

    protected int measureCell(String cell) {
        if (cell == null) {
            return 0;
        }
        else {
            int len = 0;
            for(int i = 0; i != cell.length(); ++i) {
                if (cell.charAt(i) != '\t') {
                    len++;
                }
            }
            return len;
        }
    }

    protected void renderCell(StringBuilder sb, String rawCell, int width) {
        String cell = rawCell == null ? "" : rawCell;
        int tabs = 0;
        for(int i = 0; i != cell.length(); ++i) {
            if (cell.charAt(i) == '\t') {
                tabs++;
            }
        }
        if (cell.length() - tabs > width) {
            int n = width - 3;
            for(int i = 0; i != cell.length(); ++i) {
                if (cell.charAt(i) != '\t') {
                    sb.append(cell.charAt(i));
                    if (0 == --n) {
                        break;
                    }
                }
            }
            sb.append("...");
                    
        }
        else {
            if (tabs == 0) {
                sb.append(cell);
                for(int s = 0; s != width - cell.length(); ++s) {
                    sb.append(' ');
                }
            }
            else {
                int gap = width - cell.length() + tabs;
                for(int i = 0; i != cell.length(); ++i) {
                    if (cell.charAt(i) == '\t') {
                        int fill = (gap + tabs - 1) / tabs;
                        for(int j = 0; j != fill; ++j) {
                            sb.append(' ');
                        }
                        gap -= fill;
                        tabs--;
                    }
                    else {
                        sb.append(cell.charAt(i));
                    }
                }
            }
        }
    }
    
    class CSVWriter {
        
        private Writer rawWriter;
        private PrintWriter pw;
        private char separator;
        private char quoteChar;
        private char quoteEscapeChar;
        private String lineEnd;

        public CSVWriter(Writer writer, char separator, char quotechar, char escapechar, String lineEnd) {
            this.rawWriter = writer;
            this.pw = new PrintWriter(writer);
            this.separator = separator;
            this.quoteChar = quotechar;
            this.quoteEscapeChar = escapechar;
            this.lineEnd = lineEnd;
        }
        
        public void writeNext(String[] nextLine) {
            
            if (nextLine == null)
                return;
            
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < nextLine.length; i++) {

                if (i != 0) {
                    sb.append(separator);
                }

                String nextElement = nextLine[i];
                if (nextElement == null)
                    continue;
                if (quoteChar != 0)
                    sb.append(quoteChar);
                
                sb.append(stringContainsSpecialCharacters(nextElement) ? processLine(nextElement) : nextElement);

                if (quoteChar != 0)
                    sb.append(quoteChar);
            }
            
            sb.append(lineEnd);
            pw.write(sb.toString());
        }

        private boolean stringContainsSpecialCharacters(String line) {
            return line.indexOf(quoteChar) != -1 || line.indexOf(quoteEscapeChar) != -1;
        }

        protected StringBuilder processLine(String nextElement)
        {
            StringBuilder sb = new StringBuilder();
            for (int j = 0; j < nextElement.length(); j++) {
                char nextChar = nextElement.charAt(j);
                if (quoteEscapeChar != 0 && nextChar == quoteChar) {
                    sb.append(quoteEscapeChar).append(nextChar);
                } else if (quoteEscapeChar != 0 && nextChar == quoteEscapeChar) {
                    sb.append(quoteEscapeChar).append(nextChar);
                } else {
                    sb.append(nextChar);
                }
            }
            
            return sb;
        }

        public void flush() throws IOException {
            pw.flush();
        } 

        public void close() throws IOException {
            flush();
            pw.close();
            rawWriter.close();
        }

        public boolean checkError() {
            return pw.checkError();
        }
    }    
}
