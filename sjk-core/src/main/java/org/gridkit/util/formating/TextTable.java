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

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
public class TextTable {

	public static String formatCsv(TextTable table) {
		StringWriter writer = new StringWriter();
		try {
			table.formatCsv(writer);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return writer.toString();
	}

	public static String formatASCII(TextTable table) {
		return table.formatTextTable(Integer.MAX_VALUE);
	}

    public static final Comparator<String> NUM_CMP = new NumberCmp();
    
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

	public void addColumnLeft(String... col) {
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

    public int rowCount() {
        return rows.size();
    }
	
	public void sort(int col, boolean exludeHeader, boolean descOrder, Comparator<String> cmp) {
	    Comparator<String[]> rowcmp = new RowCmp(col, descOrder, cmp);
	    if (exludeHeader) {
	        Collections.sort(rows.subList(1, rows.size()), rowcmp);
	    }
	    else {
	        Collections.sort(rows, rowcmp);
	    }
	}
	
	public void formatCsv(Appendable out) throws IOException {
		for(String[] row: rows) {
			for(int i = 0; i != row.length; ++i) {
				if (i > 0) {
					out.append(",");
				}
				formatCell(out, row[i]);
			}
			out.append("\n");
		}
	}
	
	private void formatCell(Appendable out, String row) throws IOException {
		if (row == null || row.length() == 0) {
			return;
		}
		if (isCsvSafe(row)) {
			out.append(row);
		}
		else {
			out.append('"');
			for(int i = 0; i != row.length(); ++i) {
				char c = row.charAt(i);
				if (c == '"') {
					out.append("\"\"");
				}
				else if (c == '\n') {
					// replace line end with space
					out.append(' ');
				}
				else {
					out.append(c);
				}
			}
			out.append('"');
		}
	}

	private boolean isCsvSafe(String row) {
		for(int i = 0; i != row.length(); ++i) {
			char ch = row.charAt(i);
			if (!Character.isJavaIdentifierPart(ch) && "._-".indexOf(ch) < 0) {
				return false;
			}
		}
		return true;
	}

	public String formatTextTable(int maxCellWidth) {
		return formatTable(rows, maxCellWidth, true);
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
			trimTail(sb);
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
				trimTail(sb);
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
    
    private static void trimTail(StringBuilder sb) {
        while(sb.length() > 0 && sb.charAt(sb.length() - 1) == ' ') {
            sb.setLength(sb.length() - 1);
        }
    }    
	
	private static class RowCmp implements Comparator<String[]> {

	    private final int col;
	    private final Comparator<String> cmp;
	    private final boolean descOrder;
	    
        public RowCmp(int col, boolean descOrder, Comparator<String> cmp) {
            this.col = col;
            this.cmp = cmp;
            this.descOrder = descOrder;
        }

        @Override
        public int compare(String[] o1, String[] o2) {
            String s1 = o1[col];
            String s2 = o2[col];
            if (cmp == null) {
                return (descOrder ? -1 : 1) * s1.compareTo(s2);
            }
            else {
                return (descOrder ? -1 : 1) * cmp.compare(s1, s2);
            }
        }
	}
	
	private static class NumberCmp implements Comparator<String> {

        @Override
        public int compare(String o1, String o2) {
            if (o1.length() == 0 || o2.length() == 0) {
                return o1.compareTo(o2);
            }
            try {
                Double v1 = Double.parseDouble(o1);
                Double v2 = Double.parseDouble(o2);
                return v1.compareTo(v2);
            }
            catch(NumberFormatException e) {
                return o1.compareTo(o2);
            }
        }
	}
}