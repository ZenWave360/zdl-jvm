package io.zenwave360.zdl.antlr;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.zenwave360.zdl.ZdlParser;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static io.zenwave360.zdl.antlr.JSONPath.get;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ZdlListenerTest {

    ObjectMapper mapper = new ObjectMapper();

    @Test
    public void parseZdl_SuffixJavadoc() throws Exception {
        ZdlModel model = parseZdl("src/test/resources/suffix_javadoc.zdl");
//        System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(model));
    }

    @Test
    public void parseZdl_Composed() throws Exception {
        ZdlModel model = parseZdl("src/test/resources/composed.zdl");
//        System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(model));
    }

    @Test
    public void getFindLocation() throws Exception {
        ZdlModel model = parseZdl("src/test/resources/complete.zdl");
        String location = null;
        location = model.getLocation(86, 12);
        assertEquals("entities.Customer.fields.customerId.name", location);
        location = model.getLocation(86, 20);
        assertEquals("entities.Customer.fields.customerId.type", location);
        location = model.getLocation(86, 30);
        assertEquals("entities.Customer.fields.customerId.validations.required", location);

        location = model.getLocation(86, 25);
        assertEquals("entities.Customer.fields.customerId.validations.required", location);

        location = model.getLocation(86, 33);
        assertEquals("entities.Customer.fields.customerId.validations.required", location);

        location = model.getLocation(86, 34);
        assertEquals("entities.Customer.body", location);

//        location = model.getLocation(91, 20);
//        assertEquals("entities.Customer.fields.avatar", location);
    }


    @Test
    public void parseZdl_CompleteZdl() throws Exception {

        ZdlModel model = parseZdl("src/test/resources/complete.zdl");
        assertEquals("ZenWave Online Food Delivery - Orders Module.", get(model, "$.javadoc"));

        assertEquals("com.example:artifact:RELEASE", get(model, "$.imports[0].value"));

        // CONFIG
        assertEquals("io.zenwave360.example.orders", get(model, "$.config.basePackage"));
        assertEquals("mongodb", get(model, "$.config.persistence"));

        // APIS
        assertEquals(3, get(model, "$.apis", Map.of()).size());
        assertEquals("asyncapi", get(model, "$.apis.default.type"));
        assertEquals("provider", get(model, "$.apis.default.role"));
        assertEquals("orders/src/main/resources/apis/asyncapi.yml", get(model, "$.apis.default.config.uri"));
        assertEquals("asyncapi", get(model, "$.apis.RestaurantsAsyncAPI.type"));
        assertEquals("client", get(model, "$.apis.RestaurantsAsyncAPI.role"));
        assertEquals("restaurants/src/main/resources/apis/asyncapi.yml", get(model, "$.apis.RestaurantsAsyncAPI.config.uri"));

        // PLUGINS
        assertEquals(5, get(model, "$.plugins", Map.of()).size());
        assertEquals(3, get(model, "$.plugins.ZDLToAsyncAPIPlugin.config", Map.of()).size());

        // ENUMS
        assertFalse(get(model, "$.enums.OrderStatus.hasValue", false));
        assertTrue(get(model, "$.enums.EnumWithValue.hasValue", false));

        // ENTITIES
        assertEquals(6, get(model, "$.entities", Map.of()).size());
        assertEquals("CustomerOrder", get(model, "$.entities.CustomerOrder.name"));
        assertEquals("customer_order", get(model, "$.entities.CustomerOrder.tableName"));
        assertEquals("customer-orders", get(model, "$.entities.CustomerOrder.kebabCasePlural"));
        assertEquals(true, get(model, "$.entities.CustomerOrder.options.aggregate"));
        assertNull(get(model, "$.entities.CustomerOrder.javadoc"));

        assertEquals(5, get(model, "$.entities.CustomerOrder.fields", Map.of()).size());
        assertEquals("Instant", get(model, "$.entities.CustomerOrder.fields.orderTime.type"));
        assertEquals("Instant.now()", get(model, "$.entities.CustomerOrder.fields.orderTime.initialValue"));
        assertNotNull(get(model, "$.entities.CustomerOrder.fields.orderTime.validations.required"));
        assertEquals("orderTime javadoc", get(model, "$.entities.CustomerOrder.fields.orderTime.javadoc"));
        assertEquals("orderTime javadoc", get(model, "$.entities.CustomerOrder.fields.orderTime.comment"));
        assertEquals(false, get(model, "$.entities.CustomerOrder.fields.orderTime.isEnum"));
        assertEquals(false, get(model, "$.entities.CustomerOrder.fields.orderTime.isEntity"));
        assertEquals(false, get(model, "$.entities.CustomerOrder.fields.orderTime.isArray"));

        assertEquals("OrderStatus", get(model, "$.entities.CustomerOrder.fields.status.type"));
        assertEquals("OrderStatus.RECEIVED", get(model, "$.entities.CustomerOrder.fields.status.initialValue"));
        assertNotNull(get(model, "$.entities.CustomerOrder.fields.status.validations.required"));
        assertEquals(true, get(model, "$.entities.CustomerOrder.fields.status.isEnum"));
        assertEquals(false, get(model, "$.entities.CustomerOrder.fields.status.isEntity"));
        assertEquals(false, get(model, "$.entities.CustomerOrder.fields.status.isArray"));
        assertEquals(true, get(model, "$.entities.CustomerOrder.fields.status.isComplexType"));

        assertEquals("Customer", get(model, "$.entities.CustomerOrder.fields.customerDetails.type"));
        assertNull(get(model, "$.entities.CustomerOrder.fields.customerDetails.initialValue"));
        assertNull(get(model, "$.entities.CustomerOrder.fields.customerDetails.validations.required"));
        assertEquals(false, get(model, "$.entities.CustomerOrder.fields.customerDetails.isEnum"));
        assertEquals(true, get(model, "$.entities.CustomerOrder.fields.customerDetails.isEntity"));
        assertEquals(false, get(model, "$.entities.CustomerOrder.fields.customerDetails.isArray"));
        assertEquals(true, get(model, "$.entities.CustomerOrder.fields.customerDetails.isComplexType"));
        assertNotNull(get(model, "$.entities.CustomerOrder.fields.customerDetails.options.auditing"));
        assertNotNull(get(model, "$.entities.CustomerOrder.fields.customerDetails.options.ref"));

        assertEquals("OrderItem", get(model, "$.entities.CustomerOrder.fields.orderItems.type"));
        assertNull(get(model, "$.entities.CustomerOrder.fields.orderItems.initialValue"));
        assertNull(get(model, "$.entities.CustomerOrder.fields.orderItems.validations.required"));
        assertEquals("1", get(model, "$.entities.CustomerOrder.fields.orderItems.validations.minlength.value"));
        assertEquals("200", get(model, "$.entities.CustomerOrder.fields.orderItems.validations.maxlength.value"));
        assertEquals(false, get(model, "$.entities.CustomerOrder.fields.orderItems.isEnum"));
        assertEquals(true, get(model, "$.entities.CustomerOrder.fields.orderItems.isEntity"));
        assertEquals(true, get(model, "$.entities.CustomerOrder.fields.orderItems.isArray"));
        assertEquals(true, get(model, "$.entities.CustomerOrder.fields.orderItems.isComplexType"));
        assertEquals("orderItems javadoc", get(model, "$.entities.CustomerOrder.fields.orderItems.javadoc"));
        assertEquals("orderItems javadoc", get(model, "$.entities.CustomerOrder.fields.orderItems.comment"));

        // RELATIONSHIPS
        assertEquals("Address", get(model, "$.relationships.ManyToOne.ManyToOne_Address{customer}_Customer.from"));
        assertEquals("Customer", get(model, "$.relationships.ManyToOne.ManyToOne_Address{customer}_Customer.to"));
        assertEquals("Address.customer javadoc", get(model, "$.relationships.ManyToOne.ManyToOne_Address{customer}_Customer.commentInFrom"));
        assertEquals("customer", get(model, "$.relationships.ManyToOne.ManyToOne_Address{customer}_Customer.injectedFieldInFrom"));
        assertNull(get(model, "$.relationships.ManyToOne.ManyToOne_Address{customer}_Customer.injectedFieldInTo"));

        assertEquals(true, get(model, "$.relationships.ManyToOne.ManyToOne_Address{customer2}_Customer.isInjectedFieldInFromRequired"));
        assertEquals(false, get(model, "$.relationships.ManyToOne.ManyToOne_Address{customer2}_Customer.isInjectedFieldInToRequired"));

        assertEquals("lastname", get(model, "$.relationships.OneToMany.OneToMany_Customer{addresses}_Address{customer}.injectedFieldInFromDescription"));
        assertEquals("Address.customer javadoc", get(model, "$.relationships.OneToMany.OneToMany_Customer{addresses}_Address{customer}.commentInTo"));

        assertEquals("Customer", get(model, "$.relationships.OneToOne.OneToOne_Customer{address}_Address{customer}.from"));
        assertEquals(true, get(model, "$.relationships.OneToOne.OneToOne_Customer{address}_Address{customer}.toOptions.Id"));


        // SERVICES
        assertEquals(2, get(model, "$.services", Map.of()).size());
        assertEquals(List.of("CustomerOrder"), get(model, "$.services.OrdersService.aggregates"));
        assertEquals(List.of("CustomerOrder", "Aggregate2"), get(model, "$.services.OrdersService2.aggregates"));
        assertEquals(8, get(model, "$.services.OrdersService.methods", Map.of()).size());

        assertEquals(List.of("OrderEvent", "OrderStatusUpdated"), get(model, "$.services.OrdersService.methods.updateKitchenStatus.withEvents"));
        assertEquals("RestaurantsAsyncAPI", get(model, "$.services.OrdersService.methods.updateKitchenStatus.options.asyncapi.api"));
        assertEquals("KitchenOrdersStatusChannel", get(model, "$.services.OrdersService.methods.updateKitchenStatus.options.asyncapi.channel"));
        assertEquals(1, get(model, "$.services.OrdersService.methods.updateKitchenStatus.optionsList", List.of()).size());

        assertEquals(2, get(model, "$.services.OrdersService.methods.cancelOrder.options", Map.of()).size());
        assertEquals(2, get(model, "$.services.OrdersService.methods.cancelOrder.optionsList", List.of()).size());

        assertEquals("/search", get(model, "$.services.OrdersService.methods.searchOrders.options.post.path"));
        assertEquals("String", get(model, "$.services.OrdersService.methods.searchOrders.options.post.params.param1"));

        // Test parameterIsOptional for different service methods
        assertEquals(false, get(model, "$.services.OrdersService.methods.createOrder.parameterIsOptional", false));
        assertEquals(false, get(model, "$.services.OrdersService.methods.updateOrder.parameterIsOptional", false));
        assertEquals(true, get(model, "$.services.OrdersService.methods.searchOrders.parameterIsOptional", false));
        assertEquals(false, get(model, "$.services.OrdersService.methods.getCustomerOrder.parameterIsOptional", false));

        assertEquals(true, get(model, "$.services.OrdersService.methods.createOrUpdateOrder.paramIdIsOptional", false));

        // ANNOTATIONS
        assertEquals("item1", get(model, "$.inputs.CustomerOrderInput.options.array_annotation[0]"));
        assertEquals("item1", get(model, "$.inputs.CustomerOrderInput.options.array2_annotation[0]"));
        assertEquals("value1", get(model, "$.inputs.CustomerOrderInput.options.object_annotation.item1"));
        assertEquals("value1", get(model, "$.inputs.CustomerOrderInput.options.object_annotation_pairs.item1"));
        assertEquals("value2", get(model, "$.inputs.CustomerOrderInput.options.object_annotation_nested_array.item3[1]"));


//        System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(model));
    }

    @Test
    public void parseZdl_Legacy() throws Exception {

        ZdlModel model = parseZdl("src/test/resources/legacy.jdl");

        assertEquals(1, get(model, "$.services", Map.of()).size());
        assertEquals(List.of("Customer", "Address"), get(model, "$.services.CustomerService.aggregates"));
        assertEquals(10, get(model, "$.services.CustomerService.methods", Map.of()).size());
    }



    @Test
    public void parseZdl_Problems() throws Exception {

        ZdlModel model = parseZdl("src/test/resources/problems.zdl");
        var problems = get(model, "$.problems", List.of());
        assertEquals(14, problems.size());
//        System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(model));
    }

    @Test
    public void parseZdl_Problems_ExtraTypes() throws Exception {

        ZdlModel model = parseZdl("src/test/resources/problems.zdl", List.of("OrderStatusX"));
        var problems = get(model, "$.problems", List.of());
        assertEquals(12, problems.size());
//        System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(model));
    }

    @Test
    public void parseZdl_Policies() throws Exception {

        ZdlModel model = parseZdl("src/test/resources/policies.zdl");

//        System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(model));
    }

    @Test
    public void parseZdl_NestedFields() throws Exception {

        ZdlModel model = parseZdl("src/test/resources/nested-fields.zdl");

//        System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(model));
    }

    @Test
    public void parseZdl_NestedId_Inputs_Outputs() throws Exception {

        ZdlModel model = parseZdl("src/test/resources/nested-input-output-model.zdl");

//        System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(model));
    }

    @Test
    public void parseZdl_UnrecognizedTokens() throws Exception {

        ZdlModel model = parseZdl("src/test/resources/unrecognized-tokens.zdl");

//        System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(model));
    }


    private static ZdlModel parseZdl(String fileName) throws IOException {
        CharStream zdl = CharStreams.fromFileName(fileName);
        return new ZdlParser().parseModel(zdl.toString());
    }

    private static ZdlModel parseZdl(String fileName, List<String> extraTypes) throws IOException {
        CharStream zdl = CharStreams.fromFileName(fileName);
        return new ZdlParser().withExtraFieldTypes(extraTypes).parseModel(zdl.toString());
    }

}
