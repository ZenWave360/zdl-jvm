package io.github.zenwave360.zdl;

import io.github.zenwave360.zdl.antlr.ZdlLexer;
import io.github.zenwave360.zdl.antlr.ZdlListenerImpl;
import io.github.zenwave360.zdl.antlr.ZdlModel;
import io.github.zenwave360.zdl.antlr.ZdlModelPostProcessor;
import io.github.zenwave360.zdl.antlr.ZdlModelValidator;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ZdlParser {

    public static final List<String> STANDARD_FIELD_TYPES = List.of("String", "Integer", "Long", "int", "long", "BigDecimal", "Float", "float", "Double", "double", "Enum", "Boolean", "boolean", "Map", "LocalDate", "LocalDateTime", "ZonedDate", "ZonedDateTime", "Instant", "Duration", "UUID", "byte", "byte[]", "Blob", "AnyBlob", "ImageBlob", "TextBlob");

    public static final List<String> STANDARD_VALIDATIONS = List.of("required", "unique", "min", "max", "minlength", "maxlength", "pattern");

    private List<String> standardFieldTypes = STANDARD_FIELD_TYPES;
    private List<String> extraFieldTypes = List.of();
    public ZdlParser withStandardFieldTypes(List<String> standardFieldTypes) {
        this.standardFieldTypes = standardFieldTypes;
        return this;
    }
    public ZdlParser withExtraFieldTypes(List<String> extraFieldTypes) {
        this.extraFieldTypes = extraFieldTypes;
        return this;
    }
    public ZdlModel parseModel(String model) throws IOException {
        CharStream zdl = CharStreams.fromString(model);
        ZdlLexer lexer = new ZdlLexer(zdl);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        io.github.zenwave360.zdl.antlr.ZdlParser parser = new io.github.zenwave360.zdl.antlr.ZdlParser(tokens);
        ParseTree tree = parser.zdl();
        ParseTreeWalker walker = new ParseTreeWalker();
        ZdlListenerImpl listener = new ZdlListenerImpl();
        walker.walk(listener, tree);
        var zdlModel = listener.getModel();
        zdlModel = ZdlModelPostProcessor.postProcess(zdlModel);
        try {
            zdlModel = new ZdlModelValidator()
                    .withStandardFieldTypes(standardFieldTypes)
                    .withExtraFieldTypes(extraFieldTypes)
                    .validate(zdlModel);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return zdlModel;
    }
}
