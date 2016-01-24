package org.gridkit.jvmtool.stacktrace;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;

import org.gridkit.jvmtool.stacktrace.analytics.ClassificatorAST.Root;
import org.gridkit.jvmtool.stacktrace.analytics.ClassificatorParser;
import org.gridkit.jvmtool.stacktrace.util.IndentParser.ParseException;
import org.junit.Test;

public class ClassificatorParserTest {

    @Test
    public void testJsf() throws ParseException, IOException {
        parse("jsf-histo.hsf");
    }
    
    @SuppressWarnings("resource")
    public void parse(String file) throws ParseException, IOException {
        Reader reader = new FileReader("src/test/resources/" + file);
        BufferedReader br = new BufferedReader(reader);
        ClassificatorParser parser = new ClassificatorParser();
        int n = 1;
        try {
            while(true) {
                String line = br.readLine();
                if (line != null) {
                    System.out.println("[" + n + "] " + line);                    
                    System.out.flush();
                    if (n == 27) {
                        new String();
                    }
                    parser.push(line);
                    System.err.flush();
                    ++n;
                }
                else {
                    parser.finish();
                    break;
                }
            }
        }
        catch(ParseException e) {
            for(int i = 0; i < e.getPosition() - 1; ++i) {
                System.out.print(" ");
            }
            System.out.println("^");
            throw e;
        }
        br.close();
        
        Root result = parser.getResult();
        result.toString();
        new String();
    }
    
}
