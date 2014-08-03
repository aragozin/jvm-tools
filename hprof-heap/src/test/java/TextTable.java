

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
class TextTable {

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

	public String formatTextTableUnbordered(int maxCellWidth) {
		return formatTable(rows, maxCellWidth, false);
	}

	private String formatTable(List<String[]> content, int maxCell, boolean table) {
		int[] width = new int[content.get(0).length];
		for(String[] row: content) {
			for(int i = 0; i != row.length; ++i) {
				width[i] = Math.min(Math.max(width[i], (row[i] == null ? 0 : row[i].length())), maxCell);
			}
		}

		StringBuilder sb = new StringBuilder();
		boolean header = table;
		for(String[] row: content) {
			for(int i = 0; i != width.length; ++i) {
				String cell = row[i] == null ? "" : row[i];
				if (cell.length() > width[i]) {
					cell = cell.substring(0, width[i] - 3) + "...";
				}
				sb.append(cell);
				for(int s = 0; s != width[i] - cell.length(); ++s) {
					sb.append(' ');
				}
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
}
