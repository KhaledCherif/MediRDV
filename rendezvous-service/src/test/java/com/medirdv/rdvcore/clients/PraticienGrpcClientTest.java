package com.medirdv.rdvcore.clients;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.medirdv.praticien.grpc.Creneau;
import com.medirdv.praticien.grpc.LibererCreneauRequest;
import com.medirdv.praticien.grpc.LibererCreneauResponse;
import com.medirdv.praticien.grpc.ListerCreneauxDisponiblesRequest;
import com.medirdv.praticien.grpc.PraticienServiceGrpc;
import com.medirdv.praticien.grpc.ReserverCreneauRequest;
import com.medirdv.praticien.grpc.ReserverCreneauResponse;
import com.medirdv.rdvcore.dto.CreneauxDisponibles;
import com.medirdv.rdvcore.exceptions.CreneauDejaReserveException;
import com.medirdv.rdvcore.exceptions.CreneauNotFoundException;
import com.medirdv.rdvcore.exceptions.PraticienServiceUnavailableException;

import io.grpc.Context;
import io.grpc.Deadline;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.Status;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;

/**
 * Tests the gRPC client against an in-process fake praticien-service.
 */
class PraticienGrpcClientTest {

    private static final long LIBRE = 1, RESERVE = 2, INCONNU = 3, LENT = 4;

    private Server server;
    private ManagedChannel channel;
    private PraticienServiceGrpc.PraticienServiceBlockingStub stub;
    /** Remaining time of the deadline seen by the server, in ms (-1 if no deadline). */
    private final AtomicLong deadlineSeenByServerMs = new AtomicLong();

    @BeforeEach
    void setUp() throws Exception {
        String name = InProcessServerBuilder.generateName();
        server = InProcessServerBuilder.forName(name)
                .addService(new FakePraticienService()).build().start();
        channel = InProcessChannelBuilder.forName(name).build();
        stub = PraticienServiceGrpc.newBlockingStub(channel);
    }

    @AfterEach
    void tearDown() {
        channel.shutdownNow();
        server.shutdownNow();
    }

    private PraticienGrpcClient client() {
        return new PraticienGrpcClient(stub, Duration.ofSeconds(2), Duration.ofSeconds(5));
    }

    @Test
    void reserverCreneauSendsA2sDeadline() {
        ReserverCreneauResponse response = client().reserverCreneau("req-1", LIBRE);

        assertThat(response.getCreneauId()).isEqualTo(LIBRE);
        assertThat(response.getPraticienId()).isEqualTo(10L);
        assertThat(deadlineSeenByServerMs.get()).isBetween(1_000L, 2_000L);
    }

    @Test
    void reserverCreneauDejaReserveMapsFailedPreconditionTo409Exception() {
        assertThatThrownBy(() -> client().reserverCreneau("req-2", RESERVE))
                .isInstanceOf(CreneauDejaReserveException.class)
                .hasMessageContaining("deja reserve");
    }

    @Test
    void reserverCreneauInconnuMapsNotFound() {
        assertThatThrownBy(() -> client().reserverCreneau("req-3", INCONNU))
                .isInstanceOf(CreneauNotFoundException.class);
    }

    @Test
    void reserverCreneauTooSlowFailsWithDeadlineExceeded() {
        PraticienGrpcClient shortDeadline =
                new PraticienGrpcClient(stub, Duration.ofMillis(200), Duration.ofSeconds(5));

        assertThatThrownBy(() -> shortDeadline.reserverCreneau("req-4", LENT))
                .isInstanceOf(PraticienServiceUnavailableException.class)
                .hasMessageContaining("deadline");
    }

    @Test
    void libererCreneauSendsA2sDeadline() {
        client().libererCreneau("req-5", LIBRE);

        assertThat(deadlineSeenByServerMs.get()).isBetween(1_000L, 2_000L);
    }

    @Test
    void serverDownMapsUnavailable() {
        server.shutdownNow();

        assertThatThrownBy(() -> client().reserverCreneau("req-6", LIBRE))
                .isInstanceOf(PraticienServiceUnavailableException.class)
                .hasMessageContaining("UNAVAILABLE");
    }

    @Test
    void listerCreneauxStreamsEverythingWithA5sDeadline() {
        CreneauxDisponibles result = client().listerCreneauxDisponibles(
                "cardiologie", "2026-12-01T08:00:00", "2026-12-02T00:00:00");

        assertThat(result.partiel()).isFalse();
        assertThat(result.creneaux()).hasSize(3);
        assertThat(result.creneaux().get(0).specialite()).isEqualTo("cardiologie");
        assertThat(deadlineSeenByServerMs.get()).isBetween(4_000L, 5_000L);
    }

    @Test
    void listerCreneauxReturnsPartialResultWhenStreamingDeadlineExpires() {
        PraticienGrpcClient shortDeadline =
                new PraticienGrpcClient(stub, Duration.ofSeconds(2), Duration.ofMillis(300));

        CreneauxDisponibles result = shortDeadline.listerCreneauxDisponibles("lent", "", "");

        assertThat(result.partiel()).isTrue();
        assertThat(result.creneaux()).hasSize(2);
    }

    @Test
    void listerCreneauxWithNothingBeforeDeadlineFails() {
        PraticienGrpcClient shortDeadline =
                new PraticienGrpcClient(stub, Duration.ofSeconds(2), Duration.ofMillis(200));

        assertThatThrownBy(() -> shortDeadline.listerCreneauxDisponibles("muet", "", ""))
                .isInstanceOf(PraticienServiceUnavailableException.class);
    }

    /** Fake server mimicking the status codes of praticien-service. */
    private class FakePraticienService extends PraticienServiceGrpc.PraticienServiceImplBase {

        private void recordDeadline() {
            Deadline deadline = Context.current().getDeadline();
            deadlineSeenByServerMs.set(deadline == null ? -1 : deadline.timeRemaining(TimeUnit.MILLISECONDS));
        }

        @Override
        public void reserverCreneau(ReserverCreneauRequest request, StreamObserver<ReserverCreneauResponse> out) {
            recordDeadline();
            long id = request.getCreneauId();
            if (id == RESERVE) {
                out.onError(Status.FAILED_PRECONDITION.withDescription("Creneau 2 deja reserve").asRuntimeException());
            } else if (id == INCONNU) {
                out.onError(Status.NOT_FOUND.withDescription("Creneau 3 introuvable").asRuntimeException());
            } else {
                if (id == LENT) {
                    sleep(1_000);
                }
                out.onNext(ReserverCreneauResponse.newBuilder().setCreneauId(id).setPraticienId(10).build());
                out.onCompleted();
            }
        }

        @Override
        public void libererCreneau(LibererCreneauRequest request, StreamObserver<LibererCreneauResponse> out) {
            recordDeadline();
            out.onNext(LibererCreneauResponse.newBuilder().setCreneauId(request.getCreneauId()).build());
            out.onCompleted();
        }

        @Override
        public void listerCreneauxDisponibles(ListerCreneauxDisponiblesRequest request, StreamObserver<Creneau> out) {
            recordDeadline();
            switch (request.getSpecialite()) {
                case "lent" -> {
                    out.onNext(creneau(1, "lent"));
                    out.onNext(creneau(2, "lent"));
                    sleep(1_000); // the client deadline expires here
                    out.onNext(creneau(3, "lent"));
                    out.onCompleted();
                }
                case "muet" -> {
                    sleep(1_000);
                    out.onCompleted();
                }
                default -> {
                    for (int i = 1; i <= 3; i++) {
                        out.onNext(creneau(i, request.getSpecialite()));
                    }
                    out.onCompleted();
                }
            }
        }

        private Creneau creneau(long id, String specialite) {
            return Creneau.newBuilder().setId(id).setPraticienId(10).setPraticienNom("Dr Test")
                    .setSpecialite(specialite).setDebut("2026-12-01T09:00:00").setFin("2026-12-01T09:30:00").build();
        }

        private void sleep(long ms) {
            try {
                Thread.sleep(ms);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
