
package io.helidon.example.lra.payments;

import java.net.URI;
import java.util.Collections;
import java.util.logging.Logger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.json.Json;
import jakarta.json.JsonBuilderFactory;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.lra.annotation.*;
import org.eclipse.microprofile.lra.annotation.ws.rs.LRA;

@Path("/payment")
@ApplicationScoped
public class F1PaymentResource {

    private static final Logger LOG = Logger.getLogger(F1PaymentResource.class.getName());
    private static final JsonBuilderFactory JSON = Json.createBuilderFactory(Collections.emptyMap());

    @GET
    @Path("/confirm/{cardNumber}")
    @LRA(value = LRA.Type.MANDATORY, end = false)
    @Produces(MediaType.APPLICATION_JSON)
    public Response makePayment(@HeaderParam(LRA.LRA_HTTP_CONTEXT_HEADER) URI lraId,
                                @PathParam("cardNumber") String cardNumber) {
        if (cardNumber.equals("0000-0000-0000")) {
            LOG.warning("⛔️ Payment " + cardNumber);
            return Response
                    .status(Response.Status.NOT_ACCEPTABLE)
                    .entity(JSON.createObjectBuilder()
                                    .add("error", "Card " + cardNumber + " is fake!")
                                    .build())
                    .build();
            //throw new IllegalStateException("Card " + cardNumber + " is not valid! " + lraId);
        }
        LOG.info("Payment " + cardNumber + " " + lraId);
        return Response.ok(JSON.createObjectBuilder().add("result", "success").build()).build();
    }

    @Compensate
    public Response paymentFailed(URI lraId) {
        LOG.info("🔥 Payment compensation initiated! LRA ID: " + lraId);
        // If the participant status is not confirmed as completed, coordinator retries the call eventually
        return Response.ok(ParticipantStatus.Compensated.name()).build();
    }

    @Complete
    public Response paymentSuccessful(URI lraId) {
        LOG.info("😀 Payment successful! " + lraId);
        return Response.ok(ParticipantStatus.Completed.name()).build();
    }
}
