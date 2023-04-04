
package io.helidon.example.lra.booking;

import org.eclipse.microprofile.lra.annotation.Compensate;
import org.eclipse.microprofile.lra.annotation.Complete;
import org.eclipse.microprofile.lra.annotation.ParticipantStatus;
import org.eclipse.microprofile.lra.annotation.ws.rs.LRA;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.json.Json;
import jakarta.json.JsonBuilderFactory;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.net.URI;
import java.util.Collections;
import java.util.logging.Logger;

@Path("/seat")
@ApplicationScoped
public class F1SeatBookingResource {

    private static final Logger LOG = Logger.getLogger(F1SeatBookingResource.class.getSimpleName());
    private static final JsonBuilderFactory JSON = Json.createBuilderFactory(Collections.emptyMap());


    @GET
    @Path("/book/{seat}")
    @LRA(value = LRA.Type.MANDATORY, end = false)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createBooking(@HeaderParam(LRA.LRA_HTTP_CONTEXT_HEADER) URI lraId,
                                  @PathParam("seat") String seat) {
        if (!seat.equals("06f")) {
            LOG.info("✅ Created booking for " + seat + " LRA ID: " + lraId);
            return Response.ok().build();
        } else {
            LOG.info("🚫 Seat " + seat + " already booked!");
            return Response
                    .status(Response.Status.CONFLICT)
                    .entity(JSON.createObjectBuilder()
                            .add("error", "Seat " + seat + " is already reserved!")
                            .add("seat", seat)
                            .build())
                    .build();
        }
    }

    @Compensate
    public Response seatBookingFailed(URI lraId) {
        LOG.info("🔥 Seat booking compensation initiated! LRA ID: " + lraId);
        // If the participant status is not confirmed as completed, coordinator retries the call eventually
        return Response.ok(ParticipantStatus.Compensated.name()).build();
    }

    @Complete
    public Response seatBookingSuccessful(URI lraId) {
        LOG.info("😀 Seat booking successful! " + lraId);
        return Response.ok(ParticipantStatus.Completed.name()).build();
    }

}
