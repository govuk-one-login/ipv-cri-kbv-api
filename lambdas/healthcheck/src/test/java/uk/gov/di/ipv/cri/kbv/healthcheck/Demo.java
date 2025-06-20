package uk.gov.di.ipv.cri.kbv.healthcheck;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.di.ipv.cri.kbv.healthcheck.handler.HealthCheckHandler;

import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class Demo {

    @Test
    void integration() {
        HealthCheckHandler handler = new HealthCheckHandler();
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        request.setPath("/info");


        APIGatewayProxyResponseEvent responseEvent = handler.handleRequest(request, mock(Context.class));

        System.out.println(responseEvent.getBody());

    }

}
