package io.zenwave360.zdl.antlr;

import io.zenwave360.zdl.antlr.ZflParser.Complex_valueContext;
import io.zenwave360.zdl.antlr.ZflParser.Option_valueContext;
import org.antlr.v4.runtime.ParserRuleContext;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

class ZflListenerUtils {

    static final Inflector inflector = Inflector.getInstance();

    static String getText(ParserRuleContext ctx) {
        return ctx != null? ctx.getText() : null;
    }

    static Object getText(ParserRuleContext ctx, Object defaultValue) {
        return ctx != null? ctx.getText() : defaultValue;
    }

    static Object getValueText(ZflParser.ValueContext ctx) {
        if(ctx == null) {
            return null;
        }
        if(ctx.simple() != null) {
            return getValueText(ctx.simple());
        }
        if(ctx.object() != null) {
            return getObject(ctx.object());
        }
        return getText(ctx);
    }

    static Object getValueText(ZflParser.StringContext ctx) {
        if(ctx == null) {
            return null;
        }
        if(ctx.keyword() != null) {
            return ctx.keyword().getText();
        }
        if(ctx.SINGLE_QUOTED_STRING() != null) {
            return unquote(ctx.SINGLE_QUOTED_STRING().getText(), "'");
        }
        if(ctx.DOUBLE_QUOTED_STRING() != null) {
            return unquote(ctx.DOUBLE_QUOTED_STRING().getText(), "\"");
        }
        return getText(ctx);
    }

    static Object getValueText(ZflParser.SimpleContext ctx) {
        if(ctx == null) {
            return null;
        }
        if(ctx.keyword() != null) {
            return ctx.keyword().getText();
        }
        if(ctx.SINGLE_QUOTED_STRING() != null) {
            return unquote(ctx.SINGLE_QUOTED_STRING().getText(), "'");
        }
        if(ctx.DOUBLE_QUOTED_STRING() != null) {
            return unquote(ctx.DOUBLE_QUOTED_STRING().getText(), "\"");
        }
        if(ctx.INT() != null) {
            return Long.valueOf(ctx.INT().getText());
        }
        if(ctx.NUMBER() != null) {
            return new BigDecimal(ctx.NUMBER().getText());
        }
        if(ctx.TRUE() != null) {
            return true;
        }
        if(ctx.FALSE() != null) {
            return false;
        }
        return getText(ctx);
    }

    static Object getOptionValue(Option_valueContext ctx) {
        if(ctx == null) {
            return true;
        }
        return getComplexValue(ctx.complex_value());
    }

    static Object getComplexValue(Complex_valueContext complex_value) {
        if(complex_value == null) {
            return true;
        }
        if(complex_value.value() != null) {
            return getValue(complex_value.value());
        } else {
            var array = getArrayPlain(complex_value.array_plain());
            var object = getObjectFromPairs(complex_value.pairs());
            return first(array, object, true);
        }
    }

    static Object getValue(ZflParser.ValueContext value) {
        if(value == null) {
            return true;
        }
        var object = getObject(value.object());
        var array = getArray(value.array());
        var simple = getValueText(value.simple());
        return first(object, array, simple, true);
    }

    static String unquote(String text, String quote) {
        var escape = "\\\\";
        return text
                .replaceAll("^" + quote, "")
                .replaceAll(escape + quote, quote)
                .replaceAll(quote + "$", "");
    }

    static Object getObject(ZflParser.ObjectContext ctx) {
        if(ctx == null) {
            return null;
        }
        var map = new FluentMap();
        ctx.pair().forEach(pair -> map.put(getValueText(pair.string()).toString(), getValue(pair.value())));
        return map;
    }

    static Object getObjectFromPairs(ZflParser.PairsContext ctx) {
        if(ctx == null) {
            return null;
        }
        var map = new FluentMap();
        ctx.pair().forEach(pair -> map.put(getValueText(pair.string()).toString(), getValue(pair.value())));
        return map;
    }

    static Object getArray(ZflParser.ArrayContext ctx) {
        if(ctx == null) {
            return null;
        }
        var list = new ArrayList<>();
        ctx.value().forEach(value -> list.add(getValue(value)));
        return list;
    }

    static Object getArrayPlain(ZflParser.Array_plainContext ctx) {
        if(ctx == null) {
            return null;
        }
        var list = new ArrayList<>();
        ctx.simple().forEach(value -> list.add(getValueText(value)));
        return list;
    }

    static List<String> getArray(ParserRuleContext ctx, String split) {
        if(ctx == null) {
            return null;
        }
        return Arrays.stream(ctx.getText().split(split)).map(String::trim).toList();
    }

    static String camelCase(String name) {
        return inflector.upperCamelCase(name);
    }

    static String lowerCamelCase(String name) {
        return inflector.lowerCamelCase(name);
    }

    static String kebabCase(String name) {
        return inflector.kebabCase(name);
    }

    @SafeVarargs
    static <T> T first(T... args) {
        for(T arg : args) {
            if(arg != null) {
                return arg;
            }
        }
        return null;
    }

    static String javadoc(Object javadoc) {
        if (javadoc == null) {
            return null;
        }
        return getText((ParserRuleContext) javadoc)
                .replaceAll("^/\\*\\*", "")
                .replaceAll("\\*/\\s*$", "")
                .replaceAll("^\\s*\\* ", "")
                .trim();
    }

    static int[] getLocations(ParserRuleContext ctx) {
        if(ctx == null) {
            return null;
        }
        int stopCharOffset = 0;
        if(ctx.start == ctx.stop) {
            stopCharOffset = ctx.getText().length();
        }
        // range in chars, range in lines and columns
        return new int[] { ctx.start.getStartIndex(), ctx.stop.getStopIndex() + 1, ctx.start.getLine(), ctx.start.getCharPositionInLine(), ctx.stop.getLine(), ctx.stop.getCharPositionInLine() + stopCharOffset };
    }
}

