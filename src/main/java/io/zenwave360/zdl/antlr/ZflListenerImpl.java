package io.zenwave360.zdl.antlr;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import static io.zenwave360.zdl.antlr.ZflListenerUtils.*;

public class ZflListenerImpl extends ZflBaseListener {

    ZflModel model = new ZflModel();
    Stack<FluentMap> currentStack = new Stack<>();

    public ZflModel getModel() {
        return model;
    }

    @Override
    public void enterZfl(ZflParser.ZflContext ctx) {
        // Entry point
    }

    @Override
    public void enterGlobal_javadoc(ZflParser.Global_javadocContext ctx) {
        model.put("javadoc", javadoc(ctx));
    }

    @Override
    public void enterImport_(ZflParser.Import_Context ctx) {
        model.appendToList("imports", new FluentMap()
                .with("key", getText(ctx.import_key()))
                .with("value", getValueText(ctx.import_value().string())));
    }

    @Override
    public void enterConfig_option(ZflParser.Config_optionContext ctx) {
        var name = ctx.field_name().getText();
        var value = getComplexValue(ctx.complex_value());
        model.appendTo("config", name, value);
    }

    @Override
    public void enterFlow(ZflParser.FlowContext ctx) {
        var name = getText(ctx.flow_name());
        var javadoc = javadoc(ctx.javadoc());
        
        currentStack.push(new FluentMap()
                .with("name", name)
                .with("className", camelCase(name))
                .with("javadoc", javadoc)
                .with("options", new FluentMap())
                .with("systems", new FluentMap())
                .with("starts", new FluentMap())
                .with("whens", new ArrayList<>())
                .with("end", new FluentMap())
        );
        model.appendTo("flows", name, currentStack.peek());

        var flowLocation = "flows." + name;
        model.setLocation(flowLocation, getLocations(ctx));
        model.setLocation(flowLocation + ".name", getLocations(ctx.flow_name()));
    }

    @Override
    public void exitFlow(ZflParser.FlowContext ctx) {
        currentStack.pop();
    }

    @Override
    public void enterOption(ZflParser.OptionContext ctx) {
        var name = ctx.option_name().getText().replace("@", "");
        var value = getOptionValue(ctx.option_value());
        if(!currentStack.isEmpty()) {
            currentStack.peek().appendTo("options", name, value);
            currentStack.peek().appendToList("optionsList", new FluentMap().with("name", name).with("value", value));
        }
    }

    // Systems block
    @Override
    public void enterFlow_systems(ZflParser.Flow_systemsContext ctx) {
        // Systems container - no action needed
    }

    @Override
    public void enterFlow_system(ZflParser.Flow_systemContext ctx) {
        var name = getText(ctx.flow_system_name());
        var javadoc = javadoc(ctx.javadoc());
        
        currentStack.push(new FluentMap()
                .with("name", name)
                .with("javadoc", javadoc)
                .with("options", new FluentMap())
                .with("zdl", null)
                .with("services", new FluentMap())
                .with("events", new ArrayList<>())
        );
        
        var flow = currentStack.get(currentStack.size() - 2);
        ((FluentMap) flow.get("systems")).put(name, currentStack.peek());
    }

    @Override
    public void exitFlow_system(ZflParser.Flow_systemContext ctx) {
        currentStack.pop();
    }

    @Override
    public void enterFlow_system_zdl(ZflParser.Flow_system_zdlContext ctx) {
        var zdlPath = getValueText(ctx.string());
        currentStack.peek().put("zdl", zdlPath);
    }

    @Override
    public void enterFlow_system_service(ZflParser.Flow_system_serviceContext ctx) {
        var serviceName = ctx.flow_system_service_name() != null ? getText(ctx.flow_system_service_name()) : "DefaultService";

        var service = new FluentMap()
                .with("name", serviceName)
                .with("options", new FluentMap())
                .with("commands", new ArrayList<>());
        
        currentStack.push(service);
        var system = currentStack.get(currentStack.size() - 2);
        ((FluentMap) system.get("services")).put(serviceName, service);
    }

    @Override
    public void exitFlow_system_service(ZflParser.Flow_system_serviceContext ctx) {
        currentStack.pop();
    }

    @Override
    public void enterFlow_system_service_body(ZflParser.Flow_system_service_bodyContext ctx) {
        var commands = getArray(ctx.flow_system_service_command_list(), ",");
        currentStack.peek().put("commands", commands);
    }

    @Override
    public void enterFlow_system_events(ZflParser.Flow_system_eventsContext ctx) {
        var events = getArray(ctx.flow_system_event_list(), ",");
        currentStack.peek().put("events", events);
    }

    // Start events
    @Override
    public void enterFlow_start(ZflParser.Flow_startContext ctx) {
        var name = getText(ctx.flow_start_name());
        var javadoc = javadoc(ctx.javadoc());

        var start = new FluentMap()
                .with("name", name)
                .with("className", camelCase(name))
                .with("javadoc", javadoc)
                .with("options", new FluentMap())
                .with("fields", new FluentMap());

        currentStack.push(start);
        var flow = currentStack.get(currentStack.size() - 2);
        ((FluentMap) flow.get("starts")).put(name, start);
    }

    @Override
    public void exitFlow_start(ZflParser.Flow_startContext ctx) {
        currentStack.pop();
    }

    @Override
    public void enterField(ZflParser.FieldContext ctx) {
        var name = getText(ctx.field_name());
        var type = ctx.field_type() != null && ctx.field_type().ID() != null ? ctx.field_type().ID().getText() : null;
        var isArray = ctx.field_type().ARRAY() != null;
        var javadoc = javadoc(first(ctx.javadoc(), ctx.suffix_javadoc()));

        var field = new FluentMap()
                .with("name", name)
                .with("type", type)
                .with("isArray", isArray)
                .with("javadoc", javadoc)
                .with("options", new FluentMap());

        currentStack.peek().appendTo("fields", name, field);
    }

    // When blocks
    @Override
    public void enterFlow_when(ZflParser.Flow_whenContext ctx) {
        var triggers = new ArrayList<String>();
        for(var trigger : ctx.flow_when_trigger().flow_when_event_trigger()) {
            triggers.add(trigger.getText());
        }

        var when = new FluentMap()
                .with("triggers", triggers)
                .with("commands", new ArrayList<>())
                .with("events", new ArrayList<>())
                .with("ifs", new ArrayList<>())
                .with("policies", new ArrayList<>());

        currentStack.push(when);
        var flow = currentStack.get(currentStack.size() - 2);
        ((List) flow.get("whens")).add(when);
    }

    @Override
    public void exitFlow_when(ZflParser.Flow_whenContext ctx) {
        currentStack.pop();
    }

    @Override
    public void enterFlow_when_command(ZflParser.Flow_when_commandContext ctx) {
        var commandName = getText(ctx.flow_command_name());
        ((List) currentStack.peek().get("commands")).add(commandName);
    }

    @Override
    public void enterFlow_when_event(ZflParser.Flow_when_eventContext ctx) {
        var eventName = getText(ctx.flow_event_name());
        ((List) currentStack.peek().get("events")).add(eventName);
    }

    @Override
    public void enterFlow_when_policy(ZflParser.Flow_when_policyContext ctx) {
        var policyName = getValueText(ctx.string());
        ((List) currentStack.peek().get("policies")).add(policyName);
    }

    @Override
    public void enterFlow_when_if(ZflParser.Flow_when_ifContext ctx) {
        var condition = getValueText(ctx.string());

        var ifBlock = new FluentMap()
                .with("condition", condition)
                .with("commands", new ArrayList<>())
                .with("events", new ArrayList<>())
                .with("policies", new ArrayList<>())
                .with("elseIfs", new ArrayList<>())
                .with("else", null);

        currentStack.push(ifBlock);
        var when = currentStack.get(currentStack.size() - 2);
        ((List) when.get("ifs")).add(ifBlock);
    }

    @Override
    public void exitFlow_when_if(ZflParser.Flow_when_ifContext ctx) {
        currentStack.pop();
    }

    @Override
    public void enterFlow_when_else_if(ZflParser.Flow_when_else_ifContext ctx) {
        var condition = getValueText(ctx.string());

        var elseIfBlock = new FluentMap()
                .with("condition", condition)
                .with("commands", new ArrayList<>())
                .with("events", new ArrayList<>())
                .with("policies", new ArrayList<>());

        var ifBlock = currentStack.peek();
        ((List) ifBlock.get("elseIfs")).add(elseIfBlock);
        currentStack.push(elseIfBlock);
    }

    @Override
    public void exitFlow_when_else_if(ZflParser.Flow_when_else_ifContext ctx) {
        currentStack.pop();
    }

    @Override
    public void enterFlow_when_else(ZflParser.Flow_when_elseContext ctx) {
        var elseBlock = new FluentMap()
                .with("commands", new ArrayList<>())
                .with("events", new ArrayList<>())
                .with("policies", new ArrayList<>());

        var ifBlock = currentStack.peek();
        ifBlock.put("else", elseBlock);
        currentStack.push(elseBlock);
    }

    @Override
    public void exitFlow_when_else(ZflParser.Flow_when_elseContext ctx) {
        currentStack.pop();
    }

    // End block
    @Override
    public void enterFlow_end(ZflParser.Flow_endContext ctx) {
        var end = new FluentMap()
                .with("outcomes", new FluentMap());

        currentStack.push(end);
        var flow = currentStack.get(currentStack.size() - 2);
        flow.put("end", end);
    }

    @Override
    public void exitFlow_end(ZflParser.Flow_endContext ctx) {
        currentStack.pop();
    }

    @Override
    public void enterFlow_end_outcomes(ZflParser.Flow_end_outcomesContext ctx) {
        var completedEvents = ctx.flow_end_completed() != null ? getOutcomeEvents(ctx.flow_end_completed().flow_end_outcome_list()) : null;
        var suspendedEvents = ctx.flow_end_suspended() != null ? getOutcomeEvents(ctx.flow_end_suspended().flow_end_outcome_list()) : null;
        var cancelledEvents = ctx.flow_end_cancelled() != null ? getOutcomeEvents(ctx.flow_end_cancelled().flow_end_outcome_list()) : null;

        var outcomes = new FluentMap()
                .with("completed", completedEvents)
                .with("suspended", suspendedEvents)
                .with("cancelled", cancelledEvents);

        currentStack.peek().appendTo("outcomes", outcomes);
    }

    private List<String> getOutcomeEvents(ZflParser.Flow_end_outcome_listContext ctx) {
        if(ctx == null) {
            return null;
        }
        return getArray(ctx, ",");
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

