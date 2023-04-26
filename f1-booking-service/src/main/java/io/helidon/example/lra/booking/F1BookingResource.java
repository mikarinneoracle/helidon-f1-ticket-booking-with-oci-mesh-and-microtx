package io.helidon.example.lra.booking;

import java.net.URI;
import java.util.logging.Logger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.*;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.lra.annotation.*;
import org.eclipse.microprofile.lra.annotation.ws.rs.LRA;

@Path("/f1booking")
@ApplicationScoped
public class F1BookingResource {

    private static final Logger LOG = Logger.getLogger(F1BookingResource.class.getSimpleName());

    @GET
    @Path("/book/{seat}/{cardNumber}")
    @LRA(value = LRA.Type.REQUIRES_NEW, end = true, timeLimit = 10)
    @Produces(MediaType.APPLICATION_JSON)
    public Response makeBooking(@HeaderParam(LRA.LRA_HTTP_CONTEXT_HEADER) URI lraId,
                                @PathParam("seat") String seat,
                                @PathParam("cardNumber") String cardNumber) {

        LOG.info("🪑 Seat " + seat);
        Response seatResponse = ClientBuilder.newClient()
                .target("http://f1-seat-booking-service:7001")
                .path("/seat/book/" + seat)
                .request()
                .get();
        if (seatResponse != null) {
            LOG.info("Seat booking service response: "
                    + seatResponse.getStatus()
                    + " " + seatResponse.getStatusInfo().getReasonPhrase()
                    + " " + lraId);
            seatResponse.close();
        }
        if (seatResponse.getStatus() != Response.Status.OK.getStatusCode()){
            return Response.status(Response.Status.OK)
                    .entity("{\"error\":\"Seat booking failed\"}")
                    .build();
        }

        LOG.info("💳 Payment " + cardNumber);
        Response payResponse = ClientBuilder.newClient()
                .target("http://f1-payment-service:7002")
                .path("/payment/confirm/" + cardNumber)
                .request()
                .get();

        if (payResponse != null) {
            LOG.info("Payment service response: "
                    + payResponse.getStatus()
                    + " " + payResponse.getStatusInfo().getReasonPhrase()
                    + " " + lraId);
            payResponse.close();
        }

        if (payResponse.getStatus() != Response.Status.OK.getStatusCode()){
            return Response.status(Response.Status.OK)
                    .entity("{\"error\":\"Payment failed\"}")
                    .build();
        }

        LOG.info("All Done!");
        return Response.status(Response.Status.OK)
                    .entity("{\"success\":\"Booking done!\"}")
                    .build();
    }

    @Compensate
    public Response bookingCompensate(URI lraId) {
        LOG.info("Seat Booking and Payment COMPENSATED! LRA ID:" + lraId);
        return Response.ok(ParticipantStatus.Compensated.name()).build();
    }

    @Complete
    public Response bookingSuccessful(URI lraId) {
        LOG.info("Seat Booking and Payment completed! LRA ID:" + lraId);
        return Response.ok(ParticipantStatus.Completed.name()).build();
    }
    @AfterLRA
    public void onLRAEnd(URI lraId, LRAStatus status) {
        LOG.info("Completed LRA: '" + status + "' ID: " + lraId);
    }

}
