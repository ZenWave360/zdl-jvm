package io.zenwave360.zdl.antlr;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static io.zenwave360.zdl.antlr.ZdlListenerUtils.camelCase;
import static io.zenwave360.zdl.antlr.ZdlListenerUtils.createCRUDMethods;
import static io.zenwave360.zdl.antlr.ZdlListenerUtils.first;
import static io.zenwave360.zdl.antlr.ZdlListenerUtils.getArray;
import static io.zenwave360.zdl.antlr.ZdlListenerUtils.getComplexValue;
import static io.zenwave360.zdl.antlr.ZdlListenerUtils.getLocations;
import static io.zenwave360.zdl.antlr.ZdlListenerUtils.getOptionValue;
import static io.zenwave360.zdl.antlr.ZdlListenerUtils.getText;
import static io.zenwave360.zdl.antlr.ZdlListenerUtils.getValueText;
import static io.zenwave360.zdl.antlr.ZdlListenerUtils.javadoc;
import static io.zenwave360.zdl.antlr.ZdlListenerUtils.kebabCase;
import static io.zenwave360.zdl.antlr.ZdlListenerUtils.lowerCamelCase;
import static io.zenwave360.zdl.antlr.ZdlListenerUtils.pluralize;
import static io.zenwave360.zdl.antlr.ZdlListenerUtils.snakeCase;

public class ZdlListenerImpl extends ZdlBaseListener {

    ZdlModel model = new ZdlModel();
    Stack<FluentMap> currentStack = new Stack<>();
    String currentCollection = null;

    public ZdlModel getModel() {
        return model;
    }

    @Override
    public void enterZdl(ZdlParser.ZdlContext ctx) {

    }

    @Override
    public void enterSuffix_javadoc(ZdlParser.Suffix_javadocContext ctx) {
        super.enterSuffix_javadoc(ctx);
    }

    @Override
    public void enterGlobal_javadoc(ZdlParser.Global_javadocContext ctx) {
        model.put("javadoc", javadoc(ctx));
    }

    @Override
    public void enterLegacy_constants(ZdlParser.Legacy_constantsContext ctx) {
        ctx.LEGACY_CONSTANT().stream().map(TerminalNode::getText).map(c -> c.split(" *= *")).forEach(c -> model.appendTo("constants", c[0], c[1]));
    }

    @Override
    public void enterImport_(ZdlParser.Import_Context ctx) {
        model.appendToList("imports", new FluentMap()
                .with("key", getText(ctx.import_key()))
                .with("value", getValueText(ctx.import_value().string())));
    }

    @Override
    public void enterConfig_option(ZdlParser.Config_optionContext ctx) {
        var name = ctx.field_name().getText();
        var value = getComplexValue(ctx.complex_value());
        model.appendTo("config", name, value);
    }

    @Override
    public void enterApi(ZdlParser.ApiContext ctx) {
        var name = getText(ctx.api_name());
        var type = getText(ctx.api_type());
        var role = getText(ctx.api_role(), "provider");
        var javadoc = javadoc(ctx.javadoc());
        currentStack.push(new FluentMap()
                .with("name", name)
                .with("type", type)
                .with("role", role)
                .with("javadoc", javadoc)
                .with("options", new FluentMap())
                .with("config", new FluentMap())
        );
        model.appendTo("apis", name, currentStack.peek());

        var apiLocation = "apis." + name;
        model.setLocation(apiLocation, getLocations(ctx));
        model.setLocation(apiLocation + ".name", getLocations(ctx.api_name()));
        model.setLocation(apiLocation + ".type", getLocations(ctx.api_type()));
        if(ctx.api_role() != null) {
            model.setLocation(apiLocation + ".role", getLocations(ctx.api_role()));
        }
    }

    @Override
    public void enterApi_config(ZdlParser.Api_configContext ctx) {
        var name = ctx.field_name().getText();
        var value = getComplexValue(ctx.complex_value());
        currentStack.peek().appendTo("config", name, value);
    }

    @Override
    public void exitApi(ZdlParser.ApiContext ctx) {
        currentStack.pop();
    }

    @Override
    public void enterPlugin(ZdlParser.PluginContext ctx) {
        var name = getText(ctx.plugin_name());
        var javadoc = javadoc(ctx.javadoc());
        var disabled = ctx.plugin_disabled().DISABLED() != null;
        var inherit = ctx.plugin_options() != null? getText(ctx.plugin_options().plugin_options_inherit()) : true;
        currentStack.push(new FluentMap()
                .with("name", name)
                .with("javadoc", javadoc)
                .with("disabled", disabled)
                .with("options", new FluentMap().with("inherit", inherit))
        );
        model.appendTo("plugins", name, currentStack.peek());

        var location = "plugins." + name;
        model.setLocation(location, getLocations(ctx));
        model.setLocation(location + ".name", getLocations(ctx.plugin_name()));
        model.setLocation(location + ".javadoc", getLocations(ctx.javadoc()));
        if(ctx.plugin_disabled().DISABLED() != null) {
            model.setLocation(location + ".disabled", getLocations(ctx.plugin_disabled()));
        }
        if(ctx.plugin_options() != null) {
            model.setLocation(location + ".options", getLocations(ctx.plugin_options()));
            if(ctx.plugin_options().plugin_options_inherit() != null) {
                model.setLocation(location + ".options.inherit", getLocations(ctx.plugin_options().plugin_options_inherit()));
            }
        }
    }

    @Override
    public void enterPlugin_config_option(ZdlParser.Plugin_config_optionContext ctx) {
        var name = getText(ctx.field_name());
        var value = getComplexValue(ctx.complex_value());
        currentStack.peek().appendTo("config", name, value);
    }

    @Override
    public void enterPlugin_config_cli_option(ZdlParser.Plugin_config_cli_optionContext ctx) {
        var keyword = getText(ctx.keyword());
        var value = getText(ctx.simple());
        currentStack.peek().appendTo("cliOptions", keyword, value);
    }

    @Override
    public void exitPlugin(ZdlParser.PluginContext ctx) {
        currentStack.pop();
    }

    @Override
    public void enterPolicie_body(ZdlParser.Policie_bodyContext ctx) {
        var name = getText(ctx.policie_name());
        var value = ctx.policie_value() != null? getValueText(ctx.policie_value().simple()) : null;
        var aggregate = ((ZdlParser.PoliciesContext) ctx.getParent().getParent()).policy_aggregate();
        model.appendTo("policies", new FluentMap().with(name, new FluentMap().with("name", name).with("value", value).with("aggregate", aggregate)));
        super.enterPolicie_body(ctx);
    }

    @Override
    public void enterEntity(ZdlParser.EntityContext ctx) {
        var entity = ctx.entity_definition();
        var name = entity.entity_name().getText();
        var javadoc = javadoc(ctx.javadoc());
        var tableName = getText(entity.entity_table_name());
        currentStack.push(processEntity(name, javadoc, tableName).with("type", "entities"));
        model.appendTo("entities", name, currentStack.peek());
        currentCollection = "entities";

        var entityLocation = currentCollection + "." + name;
        model.setLocation(entityLocation, getLocations(ctx));
        model.setLocation(entityLocation + ".name", getLocations(entity.entity_name()));
        model.setLocation(entityLocation + ".tableName", getLocations(entity.entity_table_name()));
        model.setLocation(entityLocation + ".body", getLocations(ctx.entity_body()));
    }

    @Override
    public void exitEntity(ZdlParser.EntityContext ctx) {
        currentStack.pop();
    }

    private FluentMap processEntity(String name, String javadoc, String tableName) {
        var className = camelCase(name);
        var instanceName = lowerCamelCase(className);
        var kebabCase = kebabCase(name);
        return new FluentMap()
                .with("name", name)
                .with("className", className)
                .with("tableName", tableName != null? tableName : snakeCase(name))
                .with("instanceName", instanceName)
                .with("classNamePlural", pluralize(name))
                .with("instanceNamePlural", pluralize(instanceName))
                .with("kebabCase", kebabCase)
                .with("kebabCasePlural", pluralize(kebabCase))
                .with("javadoc", javadoc)
                .with("options", new FluentMap())
                .with("fields", new FluentMap())
        ;
    }

    @Override
    public void enterOption(ZdlParser.OptionContext ctx) {
//        var name = ctx.reserved_option() != null? ctx.reserved_option().getText().replace("@", "") : getText(ctx.option_name());
        var name = ctx.option_name().getText().replace("@", "");
        var value = getOptionValue(ctx.option_value());
        if(!currentStack.isEmpty()) {
            currentStack.peek().appendTo("options", name, value);
            currentStack.peek().appendToList("optionsList", new FluentMap().with("name", name).with("value", value));
        }
        super.enterOption(ctx);
    }

    @Override
    public void enterField(ZdlParser.FieldContext ctx) {
        var name = getText(ctx.field_name());
        var type = ctx.field_type() != null && ctx.field_type().ID() != null? ctx.field_type().ID().getText() : null;
        var initialValue = ctx.field_initialization() != null && ctx.field_initialization().field_initial_value() != null? getValueText(ctx.field_initialization().field_initial_value().simple()) : null;
        var javadoc = javadoc(first(ctx.javadoc(), ctx.suffix_javadoc()));
        var isEnum = false; // see ZdlModelPostProcessor
        var isEntity = false; // see ZdlModelPostProcessor
        var isArray = ctx.field_type().ARRAY() != null;
        var validations = processFieldValidations(ctx.field_validations());
        if("byte".equals(type) && isArray) {
            type = "byte[]";
            isArray = false;
        }
        var field = new FluentMap()
                .with("name", name)
                .with("type", type)
                .with("initialValue", initialValue)
                .with("javadoc", javadoc)
                .with("comment", javadoc)
                .with("isEnum", isEnum)
                .with("isEntity", isEntity)
                .with("isArray", isArray)
                .with("options", new FluentMap())
                .with("validations", validations);
        currentStack.peek().appendTo("fields", name, field);

        var entityName =  currentStack.peek().get("name");
        var entityLocation = currentCollection + "." + entityName + ".fields." + name;
        model.setLocation(entityLocation, getLocations(ctx));
        model.setLocation(entityLocation + ".name", getLocations(ctx.field_name()));
        model.setLocation(entityLocation + ".type", getLocations(ctx.field_type()));
        for (var fieldValidation : ctx.field_validations()) {
            model.setLocation(entityLocation + ".validations." + getText(fieldValidation.field_validation_name()), getLocations(fieldValidation));
        }
        model.setLocation(entityLocation + ".javadoc", getLocations(first(ctx.javadoc(), ctx.suffix_javadoc())));

        currentStack.push(field);
    }

    private Map<String, Object> processFieldValidations(List<ZdlParser.Field_validationsContext> field_validations) {
        var validations = new FluentMap();
        if(field_validations != null) {
            field_validations.forEach(v -> {
                var name = getText(v.field_validation_name());
                var value = first(getText(v.field_validation_value()), "");
                if("pattern".equals(name) && value != null) {
                    value = value.substring(1, value.length() - 2);
                }
                validations.with(name, Map.of("name", name, "value", value));
            });
        }
        return validations;
    }

    @Override
    public void exitField(ZdlParser.FieldContext ctx) {
        currentStack.pop();
        super.exitField(ctx);
    }

    @Override
    public void enterNested_field(ZdlParser.Nested_fieldContext ctx) {
        ZdlParser.FieldContext parent = (ZdlParser.FieldContext) ctx.getParent();
        var parentEntity = currentStack.get(currentStack.size() - 2); // currentStack.peek();
        var parentEntityFields = ((FluentMap) parentEntity.get("fields"));
        var parentField = new ArrayList<>(parentEntityFields.values()).get(parentEntityFields.size() - 1);
        String entityName = parent.field_type().ID().getText();
        String entityJavadoc = javadoc(parent.javadoc());
        String tableName = getText(parent.entity_table_name());
        var validations = processNestedFieldValidations(ctx.nested_field_validations());
        ((FluentMap) parentField).appendTo("validations", validations);
        currentStack.push(processEntity(entityName, entityJavadoc, tableName).with("type", currentCollection.split("\\.")[0]));
        currentStack.peek().appendTo("options", "embedded", true);
        var parenFieldOptions = JSONPath.get(parentField, "options", Map.of());
        for (var entry : parenFieldOptions.entrySet()) {
            currentStack.peek().appendTo("options", (String) entry.getKey(), entry.getValue());
        }
        model.appendTo(currentCollection, entityName, currentStack.peek());

        var entityLocation = currentCollection + "." + entityName;
        var startLocation = getLocations(parent.field_type());
        var endLocation = getLocations(ctx);
        model.setLocation(entityLocation, mergeLocations(startLocation, endLocation));
        model.setLocation(entityLocation + ".name", getLocations(parent.field_type()));
        model.setLocation(entityLocation + ".tableName", getLocations(parent.entity_table_name()));
        model.setLocation(entityLocation + ".body", getLocations(ctx));
    }

    private int[] mergeLocations(int[] startLocation, int[] endLocation) {
        return new int[]{ startLocation[0], endLocation[1], startLocation[2], startLocation[3], endLocation[4], endLocation[5] };
    }

    private Map<String, Object> processNestedFieldValidations(List<ZdlParser.Nested_field_validationsContext> field_validations) {
        var validations = new FluentMap();
        if(field_validations != null) {
            field_validations.forEach(v -> {
                var name = getText(v.nested_field_validation_name());
                var value = first(getText(v.nested_field_validation_value()), "");
                validations.with(name, Map.of("name", name, "value", value));
            });
        }
        return validations;
    }

    @Override
    public void exitNested_field(ZdlParser.Nested_fieldContext ctx) {
        currentStack.pop();
    }

    @Override
    public void enterEnum(ZdlParser.EnumContext ctx) {
        var name = getText(ctx.enum_name());
        var javadoc = javadoc(ctx.javadoc());
        currentStack.push(new FluentMap()
                .with("name", name)
                .with("type", "enums")
                .with("className", camelCase(name))
                .with("javadoc", javadoc)
                .with("comment", javadoc));
        model.appendTo("enums", name, currentStack.peek());

        var entityLocation = "enums." + name;
        model.setLocation(entityLocation, getLocations(ctx));
        model.setLocation(entityLocation + ".name", getLocations(ctx.enum_name()));
        model.setLocation(entityLocation + ".body", getLocations(ctx.enum_body()));
    }

    @Override
    public void exitEnum(ZdlParser.EnumContext ctx) {
        currentStack.pop();
    }

    @Override
    public void enterEnum_value(ZdlParser.Enum_valueContext ctx) {
        var name = getText(ctx.enum_value_name());
        var javadoc = javadoc(first(ctx.javadoc(), ctx.suffix_javadoc()));
        var value = getText(ctx.enum_value_value());
        if(value != null) {
            currentStack.peek().with("hasValue", true);
        }
        currentStack.peek().appendTo("values", name, new FluentMap()
                .with("name", name)
                .with("javadoc", javadoc)
                .with("comment", javadoc)
                .with("value", value)
        );
    }

    @Override
    public void enterRelationship(ZdlParser.RelationshipContext ctx) {
        var parent = (ZdlParser.RelationshipsContext) ctx.parent;
        var relationshipType = parent.relationship_type().getText();
        var relationshipName = removeJavadoc(relationshipType + "_" + relationshipDescription(ctx.relationship_from().relationship_definition()) + "_" + relationshipDescription(ctx.relationship_to().relationship_definition()));

        var relationship = new FluentMap().with("type", relationshipType).with("name", relationshipName);
        var location = "relationships." + relationshipName;
        model.setLocation(location, getLocations(ctx));

        if(ctx.relationship_from() != null && ctx.relationship_from().relationship_definition() != null) {
            var from = getText(ctx.relationship_from().relationship_definition().relationship_entity_name());
            var fromField = getText(ctx.relationship_from().relationship_definition().relationship_field_name());
            var commentInFrom = javadoc(ctx.relationship_from().javadoc());
            var fromOptions = relationshipOptions(ctx.relationship_from().annotations().option());
            var isInjectedFieldInFromRequired = isRequired(ctx.relationship_from().relationship_definition());
            var injectedFieldInFromDescription = getText(ctx.relationship_from().relationship_definition().relationship_description_field());
            var relationshipValidations = relationshipValidations(ctx.relationship_from().relationship_definition());
            model.setLocation(location + ".from.entity", getLocations(ctx.relationship_from().relationship_definition().relationship_entity_name()));
            model.setLocation(location + ".from.field", getLocations(ctx.relationship_from().relationship_definition().relationship_field_name()));
            if (ctx.relationship_from().relationship_definition().relationship_field_validations() != null) {
                model.setLocation(location + ".from.validations", getLocations(ctx.relationship_from().relationship_definition().relationship_field_validations()));
                model.setLocation(location + ".from.validations.min", getLocations(ctx.relationship_from().relationship_definition().relationship_field_validations().relationship_field_min()));
                model.setLocation(location + ".from.validations.max", getLocations(ctx.relationship_from().relationship_definition().relationship_field_validations().relationship_field_max()));
            }
            relationship.with("from", from)
                    .with("commentInFrom", commentInFrom)
                    .with("injectedFieldInFrom", fromField)
                    .with("fromOptions", fromOptions)
                    .with("fromValidations", relationshipValidations)
                    .with("injectedFieldInFromDescription", injectedFieldInFromDescription)
                    .with("isInjectedFieldInFromRequired", isInjectedFieldInFromRequired);
        }

        if(ctx.relationship_to() != null && ctx.relationship_to().relationship_definition() != null) {
            var to = getText(ctx.relationship_to().relationship_definition().relationship_entity_name());
            var toField = getText(ctx.relationship_to().relationship_definition().relationship_field_name());
            var commentInTo = javadoc(ctx.relationship_to().javadoc());
            var toOptions = relationshipOptions(ctx.relationship_to().annotations().option());
            var isInjectedFieldInToRequired = isRequired(ctx.relationship_to().relationship_definition());
            var injectedFieldInToDescription = getText(ctx.relationship_to().relationship_definition().relationship_description_field());
            var relationshipValidations = relationshipValidations(ctx.relationship_to().relationship_definition());
            model.setLocation(location + ".to.entity", getLocations(ctx.relationship_to().relationship_definition().relationship_entity_name()));
            model.setLocation(location + ".to.field", getLocations(ctx.relationship_to().relationship_definition().relationship_field_name()));
            if (ctx.relationship_to().relationship_definition().relationship_field_validations() != null) {
                model.setLocation(location + ".to.validations", getLocations(ctx.relationship_to().relationship_definition().relationship_field_validations()));
                model.setLocation(location + ".to.validations.min", getLocations(ctx.relationship_to().relationship_definition().relationship_field_validations().relationship_field_min()));
                model.setLocation(location + ".to.validations.max", getLocations(ctx.relationship_to().relationship_definition().relationship_field_validations().relationship_field_max()));
            }
            relationship.with("to", to)
                    .with("commentInTo", commentInTo)
                    .with("injectedFieldInTo", toField)
                    .with("toOptions", toOptions)
                    .with("toValidations", relationshipValidations)
                    .with("injectedFieldInToDescription", injectedFieldInToDescription)
                    .with("isInjectedFieldInToRequired", isInjectedFieldInToRequired);
        }

        model.getRelationships().appendTo(relationshipType, relationshipName, relationship);
    }

    private boolean isRequired(ZdlParser.Relationship_definitionContext relationshipDefinitionContext) {
        return relationshipDefinitionContext.relationship_field_validations() != null
                && relationshipDefinitionContext.relationship_field_validations().relationship_field_required() != null;
    }

    private Object relationshipValidations(ZdlParser.Relationship_definitionContext relationshipDefinitionContext) {
        var validations = new FluentMap();
        if (relationshipDefinitionContext.relationship_field_validations() != null) {
            if (relationshipDefinitionContext.relationship_field_validations().relationship_field_required() != null) {
                var name = "required";
                validations.with(name, Map.of("name", name, "value", true));
            }
            if (relationshipDefinitionContext.relationship_field_validations().relationship_field_min() != null) {
                var name = "minlength";
                var value = getText(relationshipDefinitionContext.relationship_field_validations().relationship_field_min().relationship_field_value());
                validations.with(name, Map.of("name", name, "value", value));
            }
            if (relationshipDefinitionContext.relationship_field_validations().relationship_field_max() != null) {
                var name = "maxlength";
                var value = getText(relationshipDefinitionContext.relationship_field_validations().relationship_field_max().relationship_field_value());
                validations.with(name, Map.of("name", name, "value", value));
            }
        }
        return validations;
    }

    private String removeJavadoc(String text) {
        final String regex = "(/\\*\\*.+?\\*/)";
        text = text.replace("\r\n", "");
        text = text.replace("\n", "");
        return text.replaceAll(regex, "");
    }

    private String relationshipDescription(ZdlParser.Relationship_definitionContext ctx) {
        var description = "";
        if(ctx != null) {
            description = description + getText(ctx.relationship_entity_name());
            if (ctx.relationship_field_name() != null) {
                description = description + "{" + getText(ctx.relationship_field_name()) + "}";
            }
        }
        return description;
    }

    private Map<String, Object> relationshipOptions(List<ZdlParser.OptionContext> options) {
        return options.stream().collect(Collectors.toMap(o ->
                getText(o.option_name()).replace("@", ""),
                o -> getOptionValue(o.option_value())));
    }

    @Override
    public void enterService_legacy(ZdlParser.Service_legacyContext ctx) {
        var serviceName = ctx.ID().getText();
        String serviceJavadoc = "Legacy service";
        var serviceAggregates = getArray(ctx.service_aggregates(), ",");
        currentStack.push(new FluentMap()
                .with("name", serviceName)
                .with("isLegacy", true)
                .with("className", camelCase(serviceName))
                .with("javadoc", serviceJavadoc)
                .with("aggregates", serviceAggregates)
                .with("options", Map.of("rest", true))
                .with("methods", createCRUDMethods(serviceName, serviceAggregates))
        );
        model.appendTo("services", serviceName, currentStack.peek());
    }

    @Override
    public void exitService_legacy(ZdlParser.Service_legacyContext ctx) {
        currentStack.pop();
    }

    @Override
    public void enterAggregate(ZdlParser.AggregateContext ctx) {
        var aggregateName = getText(ctx.aggregate_name());
        var javadoc = javadoc(ctx.javadoc());
        var aggregateRoot = getText(ctx.aggregate_root());
        currentStack.push(new FluentMap()
                .with("name", aggregateName)
                .with("type", "aggregates")
                .with("className", camelCase(aggregateName))
                .with("javadoc", javadoc)
                .with("aggregateRoot", aggregateRoot)
                .with("commands", new FluentMap())
        );
        model.appendTo("aggregates", aggregateName, currentStack.peek());

        var name =  currentStack.peek().get("name");
        var location = "aggregates." + name;
        model.setLocation(location, getLocations(ctx));
        model.setLocation(location + ".name", getLocations(ctx.aggregate_name()));
        model.setLocation(location + ".aggregateRoot", getLocations(ctx.aggregate_root()));
    }

    @Override
    public void exitAggregate(ZdlParser.AggregateContext ctx) {
        currentStack.pop();
    }

    @Override
    public void enterAggregate_command(ZdlParser.Aggregate_commandContext ctx) {
        var aggregateName = getText(((ZdlParser.AggregateContext) ctx.getParent()).aggregate_name());
        var commandName = getText(ctx.aggregate_command_name());
        var location = "aggregates." + aggregateName + ".commands." + commandName;
        var parameter = ctx.aggregate_command_parameter() != null? ctx.aggregate_command_parameter().getText() : null;
        var parameterIsOptional = ctx.aggregate_command_parameter() != null? ctx.aggregate_command_parameter().OPTIONAL() != null : null;
        var withEvents = getServiceMethodEvents(location, ctx.with_events());
        var javadoc = javadoc(first(ctx.javadoc(), ctx.suffix_javadoc()));

        var method = new FluentMap()
                .with("name", commandName)
                .with("aggregateName", aggregateName)
                .with("parameter", parameter)
                .with("parameterIsOptional", parameterIsOptional)
                .with("withEvents", withEvents)
                .with("javadoc", javadoc)
                ;
        currentStack.peek().appendTo("commands", commandName, method);
        currentStack.push(method);

        model.setLocation(location, getLocations(ctx));
        model.setLocation(location + ".name", getLocations(ctx.aggregate_command_name()));
        model.setLocation(location + ".parameter", getLocations(ctx.aggregate_command_parameter()));
    }

    @Override
    public void exitAggregate_command(ZdlParser.Aggregate_commandContext ctx) {
        currentStack.pop();
    }

    @Override
    public void enterService(ZdlParser.ServiceContext ctx) {
        var serviceName = getText(ctx.service_name());
        var serviceJavadoc = javadoc(ctx.javadoc());
        var serviceAggregates = getArray(ctx.service_aggregates(), ",");
        currentStack.push(new FluentMap()
                .with("name", serviceName)
                .with("className", camelCase(serviceName))
                .with("javadoc", serviceJavadoc)
                .with("aggregates", serviceAggregates)
                .with("methods", new FluentMap())
        );
        model.appendTo("services", serviceName, currentStack.peek());

        var name =  currentStack.peek().get("name");
        var location = "services." + name;
        model.setLocation(location, getLocations(ctx));
        model.setLocation(location + ".name", getLocations(ctx.service_name()));
        model.setLocation(location + ".aggregates", getLocations(ctx.service_aggregates()));
    }

    @Override
    public void exitService(ZdlParser.ServiceContext ctx) {
        currentStack.pop();
    }

    @Override
    public void enterService_method(ZdlParser.Service_methodContext ctx) {
        var serviceName = getText(((ZdlParser.ServiceContext) ctx.getParent()).service_name());
        var methodName = getText(ctx.service_method_name());
        var location = "services." + serviceName + ".methods." + methodName;
        var naturalId = ctx.service_method_parameter_natural() != null? true : null;
        var methodParamId = ctx.service_method_parameter_id() != null? "id" : null;
        var methodParamIdIsOptional = ctx.service_method_parameter_id() != null? ctx.service_method_parameter_id().OPTIONAL() != null : null;
        var methodParameter = ctx.service_method_parameter() != null? ctx.service_method_parameter().getText() : null;
        var methodParameterIsOptional = ctx.service_method_parameter() != null? ctx.service_method_parameter().OPTIONAL() != null : null;
        var returnType = ctx.service_method_return() != null? ctx.service_method_return().ID().getText() : null;
        var returnTypeIsArray = ctx.service_method_return() != null? ctx.service_method_return().ARRAY() != null : null;
        var returnTypeIsOptional = ctx.service_method_return() != null? ctx.service_method_return().OPTIONAL() != null : null;
        var withEvents = getServiceMethodEvents(location, ctx.with_events());
        var javadoc = javadoc(first(ctx.javadoc(), ctx.suffix_javadoc()));

        var method = new FluentMap()
                .with("name", methodName)
                .with("serviceName", serviceName)
                .with("naturalId", naturalId)
                .with("paramId", methodParamId)
                .with("paramIdIsOptional", methodParamIdIsOptional)
                .with("parameter", methodParameter)
                .with("parameterIsOptional", methodParameterIsOptional)
                .with("returnType", returnType)
                .with("returnTypeIsArray", returnTypeIsArray)
                .with("returnTypeIsOptional", returnTypeIsOptional)
                .with("withEvents", withEvents)
                .with("javadoc", javadoc)
                ;
        currentStack.peek().appendTo("methods", methodName, method);
        currentStack.push(method);

        model.setLocation(location, getLocations(ctx));
        model.setLocation(location + ".name", getLocations(ctx.service_method_name()));
        model.setLocation(location + ".parameter", getLocations(ctx.service_method_parameter()));
        model.setLocation(location + ".returnType", getLocations(ctx.service_method_return()));
    }

    @Override
    public void exitService_method(ZdlParser.Service_methodContext ctx) {
        currentStack.pop();
    }

    private List<Object> getServiceMethodEvents(String location, ZdlParser.With_eventsContext ctx) {
        model.setLocation(location + ".withEvents", getLocations(ctx));
        var events = new ArrayList<>();
        if (ctx != null) {
            AtomicInteger i = new AtomicInteger(0);
            ctx.with_events_events().forEach(event -> {
                if (event.with_events_event() != null) {
                    var eventName = getText(event.with_events_event());
                    events.add(eventName);
                    model.setLocation(location + ".withEvents." + i.get(), getLocations(event.with_events_event()));
                    model.setLocation(location + ".withEvents." + eventName, getLocations(event.with_events_event()));
                }
                if (event.with_events_events_or() != null) {
                    var orEvents = event.with_events_events_or().with_events_event().stream().map(ParseTree::getText).collect(Collectors.toList());
                    events.add(orEvents);
                    int j = 0;
                    for (var eventContext: event.with_events_events_or().with_events_event()) {
                        model.setLocation(location + ".withEvents." + i.get() + "." + j, getLocations(eventContext));
                        model.setLocation(location + ".withEvents." + getText(eventContext), getLocations(eventContext));
                        j++;
                    }
                }
                i.incrementAndGet();
            });
        }
        return events;
    }

    @Override
    public void enterEvent(ZdlParser.EventContext ctx) {
        var name = ctx.event_name().getText();
        var javadoc = javadoc(ctx.javadoc());
        var kebabCase = kebabCase(name);
        currentStack.push(new FluentMap()
                .with("name", name)
                .with("className", camelCase(name))
                .with("type", "events")
                .with("kebabCase", kebabCase)
                .with("javadoc", javadoc)
                .with("fields", new FluentMap())
        );
        model.appendTo("events", name, currentStack.peek());
        currentCollection = "events";
    }

    @Override
    public void exitEvent(ZdlParser.EventContext ctx) {
        currentStack.pop();
    }

    //    @Override
//    public void exitEvent(ZdlParser.EventContext ctx) {
//        ctx.option().stream().filter(o -> o.option_name().getText().equals("entity")).findFirst().ifPresent(o -> {
//            var entityName = o.option_value().getText();
//            var entity = model.getEntities().get(entityName);
//            var fields = ((Map)entity).get("fields");
//            current.appendTo("fields", (Map) fields);
//        });
//    }

    @Override
    public void enterInput(ZdlParser.InputContext ctx) {
        var name = ctx.input_name().getText();
        var javadoc = javadoc(ctx.javadoc());
        currentStack.push(processEntity(name, javadoc, null).with("type", "inputs"));
        model.appendTo("inputs", name, currentStack.peek());
        currentCollection = "inputs";
    }

    @Override
    public void exitInput(ZdlParser.InputContext ctx) {
        currentStack.pop();
    }

    @Override
    public void enterOutput(ZdlParser.OutputContext ctx) {
        var name = ctx.output_name().getText();
        var javadoc = javadoc(ctx.javadoc());
        currentStack.push(processEntity(name, javadoc, null).with("type", "outputs"));
        model.appendTo("outputs", name, currentStack.peek());
        currentCollection = "outputs";
    }

    @Override
    public void exitOutput(ZdlParser.OutputContext ctx) {
        currentStack.pop();
    }

    @Override
    public void exitEveryRule(ParserRuleContext ctx) {
        super.exitEveryRule(ctx);
    }

    @Override
    public void visitTerminal(TerminalNode node) {
        super.visitTerminal(node);
    }

    @Override
    public void visitErrorNode(ErrorNode node) {
        super.visitErrorNode(node);
    }

}
