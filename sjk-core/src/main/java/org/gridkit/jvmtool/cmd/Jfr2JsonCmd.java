package org.gridkit.jvmtool.cmd;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.gridkit.jvmtool.cli.CommandLauncher;
import org.gridkit.jvmtool.cli.CommandLauncher.CmdRef;
import org.gridkit.jvmtool.jackson.DefaultPrettyPrinter;
import org.gridkit.jvmtool.jackson.JsonGenerationException;
import org.gridkit.jvmtool.spi.parsers.FileInputStreamSource;
import org.gridkit.jvmtool.spi.parsers.JsonEventDumpHelper;
import org.gridkit.jvmtool.spi.parsers.JsonEventDumpParserFactory;
import org.gridkit.jvmtool.spi.parsers.JsonEventSource;
import org.gridkit.jvmtool.util.json.SmartJsonWriter;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.beust.jcommander.ParametersDelegate;

public class Jfr2JsonCmd implements CmdRef {

    @Override
    public String getCommandName() {
        return "jfr2json";
    }

    @Override
    public Runnable newCommand(CommandLauncher host) {
        return new Jfr2Json(host);
    }

    @Parameters(commandDescription = "[JFR 2 JSON] Flight decoder, command translates JFR files into JSON")
    public static class Jfr2Json implements Runnable {

        @Parameter(names = {"-i", "--input"}, required = true)
        private String input;

        @Parameter(names = {"-o", "--output"}, required = false)
        private String output;

        @Parameter(names = {"-wl", "--whitelist"}, variableArity = true, required = false, description = "Include only specified event types")
        private List<String> whileList;

        @Parameter(names = {"-bl", "--blacklist"}, variableArity = true, required = false, description = "Exclude listed event types")
        private List<String> blackList;

        @Parameter(names = {"--max-depth"}, required = false)
        private int jsonMaxDepth = 32;

        @Parameter(names = {"--force-jdk-parser"}, required = false)
        private boolean forceJdkParser = false;

        @Parameter(names = {"--force-mc-parser"}, required = false)
        private boolean forceMcParser = false;

        @Parameter(names = {"--list-types"}, required = false, description = "Outputs list of distinct event types")
        private boolean listTypes = false;

        @ParametersDelegate
        private CommandLauncher host;

        public Jfr2Json(CommandLauncher host) {
            this.host = host;
        }

        @Override
        public void run() {
            try {
                if (!new File(input).isFile()) {
                    host.fail("Input file is not found", new File(input).getAbsolutePath());
                }
                FileInputStreamSource iss = new FileInputStreamSource(new File(input));

                Map<String, String> options = new HashMap<String, String>();
                if (forceJdkParser && forceMcParser) {
                    host.failAndPrintUsage("--force-jdk-parser and --force-mc-parser are mutually exclusive options");
                }
                if (forceJdkParser) {
                    options.put(JsonEventDumpParserFactory.OPT_USE_NATIVE_JFR_PARSER, "true");
                }
                if (forceMcParser) {
                    options.put(JsonEventDumpParserFactory.OPT_USE_NATIVE_JFR_PARSER, "false");
                }
                options.put(JsonEventDumpParserFactory.OPT_JSON_MAX_DEPTH, String.valueOf(jsonMaxDepth));

                if (whileList != null) {
                    StringBuilder sb = new StringBuilder();
                    for(String e: whileList) {
                        if (sb.length() > 0) {
                            sb.append(',');
                        }
                        sb.append(e);
                    }
                    options.put(JsonEventDumpParserFactory.OPT_JFR_EVENT_WHITELIST, sb.toString());
                }

                if (blackList != null) {
                    StringBuilder sb = new StringBuilder();
                    for(String e: blackList) {
                        if (sb.length() > 0) {
                            sb.append(',');
                        }
                        sb.append(e);
                    }
                    options.put(JsonEventDumpParserFactory.OPT_JFR_EVENT_BLACKLIST, sb.toString());
                }

                Writer writer;
                if (output != null) {
                    writer = new FileWriter(new File(output));
                }
                else {
                    writer = new OutputStreamWriter(System.out, Charset.forName("utf8"));
                }
                SmartJsonWriter jsonGen = new SmartJsonWriter(writer);

                JsonEventSource jsource = JsonEventDumpHelper.open(iss, options);

                jsonGen.writeStartArray();
                try {
                    if (listTypes) {
                        DefaultPrettyPrinter pp = new DefaultPrettyPrinter();
                        pp.indentArraysWith(new DefaultPrettyPrinter.Lf2SpacesIndenter());
                        jsonGen.setPrettyPrinter(pp);
                        final Set<String> types = new TreeSet<String>();
                        StringWriter sw = new StringWriter();
                        SmartJsonWriter sink = new SmartJsonWriter(sw) {
                            @Override
                            public void writeStringField(String fieldName, String value) throws IOException, JsonGenerationException {
                                if ("eventType".equals(fieldName)) {
                                    types.add(value);
                                }
                                super.writeStringField(fieldName, value);
                            }
                        };
                        while(jsource.readNext(sink)) {
                            sw.getBuffer().setLength(0); // clear
                        }
                        for(String type: types) {
                            jsonGen.writeString(type);
                        }
                    }
                    else {
                        // dump event
                        while(jsource.readNext(jsonGen));
                    }
                }
                finally {
                    jsonGen.writeEndArray();
                    jsonGen.flush();
                    if (writer instanceof FileWriter) {
                        writer.close();
                    }
                }

            } catch (IOException e) {
                host.fail("", e);
            }
        }
    }

}
