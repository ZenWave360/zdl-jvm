package io.github.zenwave360.zdl.antlr;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.zenwave360.zdl.ZdlModel;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.junit.jupiter.api.Test;

import java.io.IOException;

public class ZdlListenerTest {

    ObjectMapper mapper = new ObjectMapper();

    @Test
    public void parseZdl_Simple() throws Exception {

        ZdlModel model = parseZdl("src/test/resources/simple.zdl");

        System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(model));
    }

    @Test
    public void parseZdl_NestedFields() throws Exception {

        ZdlModel model = parseZdl("src/test/resources/nested-fields.zdl");

        System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(model));
    }

    @Test
    public void parseZdl_UnrecognizedTokens() throws Exception {

        ZdlModel model = parseZdl("src/test/resources/unrecognized-tokens.zdl");

        System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(model));
    }


    private static ZdlModel parseZdl(String fileName) throws IOException {
        CharStream zdl = CharStreams.fromFileName(fileName);
        ZdlLexer lexer = new ZdlLexer(zdl);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        ZdlParser parser = new ZdlParser(tokens);
        ParseTree tree = parser.zdl();
        ParseTreeWalker walker = new ParseTreeWalker();
        ZdlListenerImpl listener = new ZdlListenerImpl();
        walker.walk(listener, tree);
        return listener.model;
    }

}
