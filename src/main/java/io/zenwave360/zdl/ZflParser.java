package io.zenwave360.zdl;

import io.zenwave360.zdl.antlr.ZflLexer;
import io.zenwave360.zdl.antlr.ZflListenerImpl;
import io.zenwave360.zdl.antlr.ZflModel;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

import java.io.IOException;

public class ZflParser {

    public ZflModel parseModel(String model) throws IOException {
        long start = System.currentTimeMillis();
        CharStream zfl = CharStreams.fromString(model);
        ZflLexer lexer = new ZflLexer(zfl);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        io.zenwave360.zdl.antlr.ZflParser parser = new io.zenwave360.zdl.antlr.ZflParser(tokens);
        ParseTree tree = parser.zfl();
        ParseTreeWalker walker = new ParseTreeWalker();
        ZflListenerImpl listener = new ZflListenerImpl();
        long end = System.currentTimeMillis();
        System.out.println("Time: " + (end - start));
        walker.walk(listener, tree);
        var zflModel = listener.getModel();
        return zflModel;
    }
}

