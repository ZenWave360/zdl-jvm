package io.zenwave360.zdl.antlr;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.zenwave360.zdl.ZflParser;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static io.zenwave360.zdl.antlr.JSONPath.get;
import static org.junit.jupiter.api.Assertions.*;

public class ZflListenerTest {

    ObjectMapper mapper = new ObjectMapper();

    @Test
    public void parseZfl_Subscriptions() throws Exception {
        ZflModel model = parseZfl("src/test/resources/flow/subscriptions.zfl");
        
        // Print the model for debugging
        System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(model));
        
        // Test imports
        assertEquals(2, get(model, "$.imports", List.of()).size());
        assertEquals("subscriptions", get(model, "$.imports[0].key"));
        assertEquals("http://localhost:8080/subscription/model.zdl", get(model, "$.imports[0].value"));
        assertEquals("payments", get(model, "$.imports[1].key"));
        assertEquals("com.example.domain:payments:RELEASE", get(model, "$.imports[1].value"));
        
        // Test flow
        assertEquals(1, get(model, "$.flows", Map.of()).size());
        assertEquals("PaymentsFlow", get(model, "$.flows.PaymentsFlow.name"));
        assertEquals("PaymentsFlow", get(model, "$.flows.PaymentsFlow.className"));
        assertNotNull(get(model, "$.flows.PaymentsFlow.javadoc"));
        
        // Test systems
        assertEquals(3, get(model, "$.flows.PaymentsFlow.systems", Map.of()).size());
        
        // Test Subscription system
        assertEquals("Subscription", get(model, "$.flows.PaymentsFlow.systems.Subscription.name"));
        assertEquals("subscription/model.zdl", get(model, "$.flows.PaymentsFlow.systems.Subscription.zdl"));
        assertEquals(1, get(model, "$.flows.PaymentsFlow.systems.Subscription.services", Map.of()).size());
        assertEquals("SubscriptionService", get(model, "$.flows.PaymentsFlow.systems.Subscription.services.SubscriptionService.name"));
        assertEquals(List.of("renewSubscription", "suspendSubscription", "cancelRenewal"), 
                     get(model, "$.flows.PaymentsFlow.systems.Subscription.services.SubscriptionService.commands"));
        assertEquals(List.of("SubscriptionRenewed", "SubscriptionSuspended", "RenewalCancelled"), 
                     get(model, "$.flows.PaymentsFlow.systems.Subscription.events"));
        
        // Test Payments system
        assertEquals("Payments", get(model, "$.flows.PaymentsFlow.systems.Payments.name"));
        assertNull(get(model, "$.flows.PaymentsFlow.systems.Payments.zdl"));
        
        // Test Billing system
        assertEquals("Billing", get(model, "$.flows.PaymentsFlow.systems.Billing.name"));
        
        // Test start events
        assertEquals(3, get(model, "$.flows.PaymentsFlow.starts", Map.of()).size());
        
        // Test CustomerRequestsSubscriptionRenewal start
        assertEquals("CustomerRequestsSubscriptionRenewal", 
                     get(model, "$.flows.PaymentsFlow.starts.CustomerRequestsSubscriptionRenewal.name"));
        assertEquals("Customer", 
                     get(model, "$.flows.PaymentsFlow.starts.CustomerRequestsSubscriptionRenewal.options.actor"));
        assertEquals(3, get(model, "$.flows.PaymentsFlow.starts.CustomerRequestsSubscriptionRenewal.fields", Map.of()).size());
        assertEquals("String", get(model, "$.flows.PaymentsFlow.starts.CustomerRequestsSubscriptionRenewal.fields.subscriptionId.type"));
        assertEquals("String", get(model, "$.flows.PaymentsFlow.starts.CustomerRequestsSubscriptionRenewal.fields.customerId.type"));
        assertEquals("String", get(model, "$.flows.PaymentsFlow.starts.CustomerRequestsSubscriptionRenewal.fields.paymentMethodId.type"));
        
        // Test BillingCycleEnded start
        assertEquals("BillingCycleEnded", get(model, "$.flows.PaymentsFlow.starts.BillingCycleEnded.name"));
        assertEquals("end of month", get(model, "$.flows.PaymentsFlow.starts.BillingCycleEnded.options.time"));
        assertEquals(1, get(model, "$.flows.PaymentsFlow.starts.BillingCycleEnded.fields", Map.of()).size());
        
        // Test PaymentTimeout start
        assertEquals("PaymentTimeout", get(model, "$.flows.PaymentsFlow.starts.PaymentTimeout.name"));
        assertEquals("5 minutes after SubscriptionRenewed and not PaymentSucceeded or PaymentFailed", 
                     get(model, "$.flows.PaymentsFlow.starts.PaymentTimeout.options.time"));
        
        // Test when blocks
        List<Map> whens = get(model, "$.flows.PaymentsFlow.whens", List.of());
        assertEquals(5, whens.size());
        
        // Test first when block
        assertEquals(List.of("CustomerRequestsSubscriptionRenewal"), get(whens.get(0), "$.triggers"));
        assertEquals(List.of("renewSubscription"), get(whens.get(0), "$.commands"));
        assertEquals(List.of("SubscriptionRenewed"), get(whens.get(0), "$.events"));
        
        // Test second when block
        assertEquals(List.of("SubscriptionRenewed"), get(whens.get(1), "$.triggers"));
        assertEquals(List.of("chargePayment"), get(whens.get(1), "$.commands"));
        assertEquals(List.of("PaymentSucceeded", "PaymentFailed"), get(whens.get(1), "$.events"));
        
        // Test third when block with if/else
        assertEquals(List.of("PaymentFailed"), get(whens.get(2), "$.triggers"));
        List<Map> ifs = get(whens.get(2), "$.ifs", List.of());
        assertEquals(1, ifs.size());
        assertEquals("less than 3 attempts", get(ifs.get(0), "$.condition"));
        assertEquals(List.of("retryPayment"), get(ifs.get(0), "$.commands"));
        assertEquals(List.of("PaymentRetryScheduled"), get(ifs.get(0), "$.events"));
        
        Map elseBlock = get(ifs.get(0), "$.else");
        assertNotNull(elseBlock);
        assertEquals(List.of("Suspend after 3 failed attempts"), get(elseBlock, "$.policies"));
        assertEquals(List.of("suspendSubscription"), get(elseBlock, "$.commands"));
        assertEquals(List.of("SubscriptionSuspended"), get(elseBlock, "$.events"));
        
        // Test fourth when block with AND trigger
        assertEquals(List.of("PaymentSucceeded", "BillingCycleEnded"), get(whens.get(3), "$.triggers"));
        assertEquals(List.of("recordPayment"), get(whens.get(3), "$.commands"));
        assertEquals(List.of("PaymentRecorded"), get(whens.get(3), "$.events"));
        
        // Test fifth when block
        assertEquals(List.of("PaymentTimeout"), get(whens.get(4), "$.triggers"));
        assertEquals(List.of("cancelRenewal"), get(whens.get(4), "$.commands"));
        assertEquals(List.of("RenewalCancelled"), get(whens.get(4), "$.events"));
        
        // Test end block
        Map<String, List<String>> outcomes = get(model, "$.flows.PaymentsFlow.end.outcomes", Map.of());
        assertEquals(3, outcomes.size());
        assertEquals("PaymentRecorded", outcomes.get("completed").get(0));
        assertEquals("SubscriptionSuspended", outcomes.get("suspended").get(0));
        assertEquals("RenewalCancelled", outcomes.get("cancelled").get(0));
    }

    private static ZflModel parseZfl(String fileName) throws IOException {
        CharStream zfl = CharStreams.fromFileName(fileName);
        return new ZflParser().parseModel(zfl.toString());
    }
}

