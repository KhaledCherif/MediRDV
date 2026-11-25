package com.medirdv.rdvcore.clients;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.medirdv.praticien.grpc.Creneau;
import com.medirdv.praticien.grpc.LibererCreneauRequest;
import com.medirdv.praticien.grpc.ListerCreneauxDisponiblesRequest;
import com.medirdv.praticien.grpc.PraticienServiceGrpc.PraticienServiceBlockingStub;
import com.medirdv.praticien.grpc.ReserverCreneauRequest;
import com.medirdv.praticien.grpc.ReserverCreneauResponse;
import com.medirdv.rdvcore.dto.CreneauDto;
import com.medirdv.rdvcore.dto.CreneauxDisponibles;
import com.medirdv.rdvcore.exceptions.CreneauDejaReserveException;
import com.medirdv.rdvcore.exceptions.CreneauNotFoundException;
import com.medirdv.rdvcore.exceptions.PraticienServiceUnavailableException;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;

/**
 * gRPC client toward praticien-service (Suivi 2, Membre 5).
 *
 * Every call gets an explicit deadline:
 *  - 2s for the unary RPCs ReserverCreneau / LibererCreneau,
 *  - 5s for the server-streaming RPC ListerCreneauxDisponibles.
 *
 * gRPC status codes are translated to business exceptions:
 *  NOT_FOUND -> CreneauNotFoundException (404),
 *  FAILED_PRECONDITION -> CreneauDejaReserveException (409),
 *  INVALID_ARGUMENT -> IllegalArgumentException (400),
 *  DEADLINE_EXCEEDED / UNAVAILABLE / others -> PraticienServiceUnavailableException (503).
 */
@Component
public class PraticienGrpcClient {

    private final PraticienServiceBlockingStub stub;
    private final Duration unaryDeadline;
    private final Duration streamingDeadline;

    public PraticienGrpcClient(PraticienServiceBlockingStub stub,
                               @Value("${praticien-service.grpc.unary-deadline:2s}") Duration unaryDeadline,
                               @Value("${praticien-service.grpc.streaming-deadline:5s}") Duration streamingDeadline) {
        this.stub = stub;
        this.unaryDeadline = unaryDeadline;
        this.streamingDeadline = streamingDeadline;
    }

    /**
     * Reserves a slot. Idempotent on requestId: replaying the same requestId is safe
     * (the response then has dejaTraite = true).
     */
    public ReserverCreneauResponse reserverCreneau(String requestId, long creneauId) {
        ReserverCreneauRequest request = ReserverCreneauRequest.newBuilder()
                .setRequestId(requestId)
                .setCreneauId(creneauId)
                .build();
        try {
            return stub.withDeadlineAfter(unaryDeadline.toMillis(), TimeUnit.MILLISECONDS)
                    .reserverCreneau(request);
        } catch (StatusRuntimeException e) {
            throw translate(e, creneauId, "ReserverCreneau");
        }
    }

    /**
     * Compensation: frees a slot. Freeing an already free slot is a successful no-op on the server.
     */
    public void libererCreneau(String requestId, long creneauId) {
        LibererCreneauRequest request = LibererCreneauRequest.newBuilder()
                .setRequestId(requestId)
                .setCreneauId(creneauId)
                .build();
        try {
            stub.withDeadlineAfter(unaryDeadline.toMillis(), TimeUnit.MILLISECONDS)
                    .libererCreneau(request);
        } catch (StatusRuntimeException e) {
            throw translate(e, creneauId, "LibererCreneau");
        }
    }

    /**
     * Streams the free slots of a specialty between du (inclusive) and au (exclusive), ISO-8601 date-times.
     * If the 5s deadline expires mid-stream, the slots already received are returned with partiel = true,
     * so the front can show a "partial result" banner instead of failing.
     */
    public CreneauxDisponibles listerCreneauxDisponibles(String specialite, String du, String au) {
        ListerCreneauxDisponiblesRequest request = ListerCreneauxDisponiblesRequest.newBuilder()
                .setSpecialite(specialite == null ? "" : specialite)
                .setDu(du == null ? "" : du)
                .setAu(au == null ? "" : au)
                .build();

        List<CreneauDto> creneaux = new ArrayList<>();
        try {
            Iterator<Creneau> stream = stub
                    .withDeadlineAfter(streamingDeadline.toMillis(), TimeUnit.MILLISECONDS)
                    .listerCreneauxDisponibles(request);
            while (stream.hasNext()) {
                creneaux.add(CreneauDto.from(stream.next()));
            }
            return new CreneauxDisponibles(creneaux, false);
        } catch (StatusRuntimeException e) {
            if (e.getStatus().getCode() == Status.Code.DEADLINE_EXCEEDED && !creneaux.isEmpty()) {
                return new CreneauxDisponibles(creneaux, true);
            }
            throw translate(e, null, "ListerCreneauxDisponibles");
        }
    }

    private RuntimeException translate(StatusRuntimeException e, Long creneauId, String rpc) {
        String description = e.getStatus().getDescription();
        return switch (e.getStatus().getCode()) {
            case NOT_FOUND -> new CreneauNotFoundException(creneauId, description);
            case FAILED_PRECONDITION -> new CreneauDejaReserveException(creneauId, description);
            case INVALID_ARGUMENT -> new IllegalArgumentException(
                    description != null ? description : "Invalid argument for " + rpc, e);
            case DEADLINE_EXCEEDED -> new PraticienServiceUnavailableException(
                    "praticien-service did not answer " + rpc + " before the deadline", e);
            default -> new PraticienServiceUnavailableException(
                    "praticien-service is unavailable (" + e.getStatus().getCode() + " on " + rpc + ")", e);
        };
    }
}
