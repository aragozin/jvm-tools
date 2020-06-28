package org.gridkit.jvmtool.stacktrace.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public abstract class IndentParser {

    public static String POPUP = "POPUP";

    List<Object> resultStack = new ArrayList<Object>();
    List<Integer> indentStack = new ArrayList<Integer>();

    List<ParseState> parseStateStack = new ArrayList<ParseState>();

    int lineNumber;
    int lineIndent;

    protected IndentParser() {
        reset();
    }

    public void push(String line) throws ParseException {
        lineNumber++;
        if (isEmpty(line)) {
            return;
        }
        int n = lineIndent = indentOf(line);
        String token = line.substring(n, line.length());
        if (isIndentPending()) {
            if (n > lastIndent()) {
                updateIndent(n);
                pushToken(token);
            }
            else if (n == lastIndent()) {
                indentPopup();
                pushToken(token);
            }
            else {
                popupTo(n);
                pushToken(token);
            }
        }
        else {
            if (n == lastIndent()) {
                pushToken(token);
            }
            else {
                popupTo(n);
                pushToken(token);
            }
        }
        if (parseState().expectedPatterns.isEmpty()) {
            throw new ParseException("Internal error: Parsing dead end", lineNumber, line.length() - 1);
        }
    }

    public void finish() throws ParseException {
        try {
            for(int i = 0; i != indentStack.size(); ++i) {
                sendPopup();
            }
            indentStack.clear();
        }
        catch(ParseException e) {
            throw e;
        }
        catch(Exception e) {
            throw new ParseException("Parse error: " + e.toString(), lineNumber + 1, 0);
        }
    }

    private boolean isIndentPending() {
        return indentStack.get(indentStack.size() - 1) == -1;
    }

    private void updateIndent(int n) {
        if (isIndentPending()) {
            indentStack.set(indentStack.size() - 1, n);
        }
        else {
            throw new IllegalArgumentException();
        }
    }

    private int lastIndent() {
        if (isIndentPending()) {
            return indentStack.size() == 1 ? -1 : indentStack.get(indentStack.size() - 2);
        }
        else {
            return indentStack.get(indentStack.size() - 1);
        }
    }

    private void popupTo(int n) throws ParseException {
        while(true) {
            int m = indentStack.remove(indentStack.size() - 1);
            if (m == -1) {
                sendPopup();
                m = indentStack.remove(indentStack.size() - 1);
            }
            if (m == n) {
                indentStack.add(m); // return back
                return;
            }
            if (m < n) {
                throw new ParseException("Invalid indenation alignment, expected is " + m, lineNumber, n);
            }
            else if (indentStack.size() == 0) {
                throw new ParseException("Invalid indenation alignment, expected is " + m, lineNumber, n);
            }
            sendPopup();
        }
    }

    private void pushToken(String token) throws ParseException {
        for(String tk: parseState().expectedPatterns.keySet()) {
            String ptr = parseState().expectedPatterns.get(tk);
            if (ptr.length() > 0 && token.matches(ptr)) {
                onToken(tk, token);
                return;
            }
        }
        throw new ParseException("Line is not expected. Expected " + parseState().getExpectedTokensHint() + ", line: " + token, lineNumber, lineIndent);
    }

    private void indentPopup() throws ParseException {
        indentStack.remove(indentStack.size() - 1);
        sendPopup();
    }

    private void sendPopup() throws ParseException {
        if (parseState().expectedPatterns.containsKey(POPUP)) {
            onPopup();
        }
        else {
            throw new ParseException("Unexpected unindent, expected " + parseState().getExpectedTokensHint(), lineNumber, lineIndent);
        }
    }

    protected int indentOf(String line) throws ParseException {
        for(int i = 0; i != line.length(); ++i) {
            char ch = line.charAt(i);
            if (ch == ' ') {
                continue;
            }
            if (!Character.isWhitespace(ch)) {
                return i;
            }
            throw new ParseException("Unallowed char 0x" + Integer.toHexString(ch), lineNumber, i);
        }
        throw new IllegalArgumentException("Line is empty");
    }

    protected boolean isEmpty(String line) {
        line = line.trim();
        if (line.startsWith("#") || line.length() == 0) {
            return true;
        }
        else {
            return false;
        }
    }

    public void reset() {
        resultStack.clear();
        indentStack.clear();
        indentStack.add(0);
        parseStateStack.clear();
        parseStateStack.add(new ParseState());

        initialState();
    }

    protected abstract void initialState();

    protected abstract void onToken(String tokenType, String token) throws ParseException;

    protected abstract void onPopup() throws ParseException;

    public int getLineNumber() {
        return lineNumber;
    }

    public int getIndent() {
        return lastIndent();
    }

    public Map<String, String> getExpectedTokens() {
        return parseState().expectedTokens();
    }

    private ParseState parseState() {
        return parseStateStack.get(parseStateStack.size() - 1);
    }

    protected void error(String message) throws ParseException {
        throw new ParseException(message, lineNumber, lineIndent);
    }

    protected void pushParseState() {
        indentStack.add(-1);
        parseStateStack.add(new ParseState());
    }

    protected void popParseState() {
        parseStateStack.remove(parseStateStack.size() - 1);
    }

    protected Object value() {
        return resultStack.get(resultStack.size() - 1);
    }

    protected void pushValue(Object value) {
        resultStack.add(value);
    }

    protected Object popValue() {
        return resultStack.remove(resultStack.size() - 1);
    }

    protected void expectToken(String id, String pattern) {
        parseState().expectToken(id, pattern);
    }

    protected void unexpectToken(String id) {
        parseState().unexpectToken(id);
    }

    protected void expectPopup() {
        parseState().expectToken(POPUP, "");
    }

    protected void unexpectPopup() {
        parseState().unexpectToken(POPUP);
    }

    protected void unexpectAll() {
        parseState().expectedPatterns.clear();
    }

    protected static class ParseState {

        Map<String, String> expectedPatterns = new LinkedHashMap<String, String>();

        public Map<String, String> expectedTokens() {
            return new LinkedHashMap<String, String>(expectedPatterns);
        }

        public String getExpectedTokensHint() {
            List<String> tokens = new ArrayList<String>(expectedPatterns.keySet());
            Collections.sort(tokens);
            return tokens.toString();
        }

        public void expectToken(String name, String pattern) {
            expectedPatterns.put(name, pattern);
        }

        public void unexpectToken(String name) {
            expectedPatterns.remove(name);
        }
    }

    @SuppressWarnings("serial")
    public static class ParseException extends Exception {

        private int line;
        private int position;

        public ParseException(String message, int line, int position) {
            super(message);
            this.line = line;
            this.position = position;
        }

        public ParseException(String message, int line, int position, Throwable cause) {
            super(message, cause);
            this.line = line;
            this.position = position;
        }

        public int getLine() {
            return line;
        }

        public int getPosition() {
            return position;
        }
    }
}
