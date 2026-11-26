package com.medirdv.praticien;

import io.grpc.ManagedChannel;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.stub.ServerCallStreamObserver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import com.medirdv.praticien.domain.Creneau;
import com.medirdv.praticien.domain.Praticien;
import com.medirdv.praticien.grpc.LibererCreneauRequest;
import com.medirdv.praticien.grpc.ListerCreneauxDisponiblesRequest;
import com.medirdv.praticien.grpc.PraticienGrpcService;
import com.medirdv.praticien.grpc.PraticienServiceGrpc;
import com.medirdv.praticien.grpc.ReserverCreneauRequest;
import com.medirdv.praticien.repository.CreneauRepository;
import com.medirdv.praticien.repository.GrpcRequestRecordRepository;
import com.medirdv.praticien.repository.PraticienRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:grpctest;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "grpc.server.port=-1",
        "grpc.server.in-process-name=praticien-grpc-test"
})
class PraticienGrpcTest {

    @Autowired CreneauRepository creneaux;
    @Autowired PraticienRepository praticiens;
    @Autowired GrpcRequestRecordRepository requests;
    @Autowired PraticienGrpcService grpcService;

    private ManagedChannel channel;
    private PraticienServiceGrpc.PraticienServiceBlockingStub stub;

    @BeforeEach
    void connecterAuServeur() {
        channel = InProcessChannelBuilder.forName("praticien-grpc-test").directExecutor().build();
        stub = PraticienServiceGrpc.newBlockingStub(channel);
        requests.deleteAllInBatch();
        creneaux.deleteAllInBatch();
        praticiens.deleteAllInBatch();
    }

    @AfterEach
    void fermerCanal() {
        channel.shutdownNow();
    }

    @Test
    void reservationEstIdempotentePourLeMemeRequestId() {
        Creneau creneau = nouveauCreneau("Cardiologie", LocalDateTime.of(2027, 3, 1, 9, 0));
        Creneau autreCreneau = nouveauCreneau("Cardiologie", LocalDateTime.of(2027, 3, 1, 10, 0));
        String requestId = UUID.randomUUID().toString();
        ReserverCreneauRequest request = ReserverCreneauRequest.newBuilder()
                .setRequestId(requestId)
                .setCreneauId(creneau.getId())
                .build();

        assertThat(stub.reserverCreneau(request).getDejaTraite()).isFalse();
        assertThat(stub.reserverCreneau(request).getDejaTraite()).isTrue();
        assertStatus(Status.Code.INVALID_ARGUMENT, () -> stub.reserverCreneau(reservation(requestId, autreCreneau)));
    }

    @Test
    void reservationDuMemeCreneauAvecUnAutreRequestIdEchoueEnFailedPrecondition() {
        Creneau creneau = nouveauCreneau("Cardiologie", LocalDateTime.of(2027, 3, 1, 9, 0));
        stub.reserverCreneau(reservation(UUID.randomUUID().toString(), creneau));

        assertThatThrownBy(() -> stub.reserverCreneau(reservation(UUID.randomUUID().toString(), creneau)))
                .isInstanceOf(StatusRuntimeException.class)
                .satisfies(exception -> assertThat(((StatusRuntimeException) exception).getStatus().getCode())
                        .isEqualTo(Status.Code.FAILED_PRECONDITION));
    }

    @Test
    void reservationsConcurrentesAvecLeMemeRequestIdOntUnSeulTraitement() throws Exception {
        Creneau creneau = nouveauCreneau("Cardiologie", LocalDateTime.of(2027, 3, 1, 9, 0));
        ReserverCreneauRequest request = reservation(UUID.randomUUID().toString(), creneau);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch pretes = new CountDownLatch(2);
        CountDownLatch depart = new CountDownLatch(1);
        try {
            Future<Boolean> first = pool.submit(() -> reserverApresDepart(request, pretes, depart));
            Future<Boolean> second = pool.submit(() -> reserverApresDepart(request, pretes, depart));
            pretes.await();
            depart.countDown();

            assertThat(List.of(first.get(), second.get())).containsExactlyInAnyOrder(false, true);
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void requestIdReutiliseConcurremmentPourDeuxCreneauxEstRefuse() throws Exception {
        Creneau premier = nouveauCreneau("Cardiologie", LocalDateTime.of(2027, 3, 1, 9, 0));
        Creneau second = nouveauCreneau("Cardiologie", LocalDateTime.of(2027, 3, 1, 10, 0));
        String requestId = UUID.randomUUID().toString();
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch pretes = new CountDownLatch(2);
        CountDownLatch depart = new CountDownLatch(1);
        try {
            Future<Status.Code> first = pool.submit(
                    () -> reserverEtLireStatut(reservation(requestId, premier), pretes, depart));
            Future<Status.Code> secondFuture = pool.submit(
                    () -> reserverEtLireStatut(reservation(requestId, second), pretes, depart));
            pretes.await();
            depart.countDown();

            assertThat(List.of(first.get(), secondFuture.get()))
                    .containsExactlyInAnyOrder(Status.Code.OK, Status.Code.INVALID_ARGUMENT);
            long libres = List.of(premier, second).stream()
                    .filter(slot -> creneaux.findById(slot.getId()).orElseThrow().getStatut()
                            == com.medirdv.praticien.domain.StatutCreneau.LIBRE)
                    .count();
            assertThat(libres).isEqualTo(1);
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void argumentsInvalidesEtCreneauAbsentOntLesCodesAttendus() {
        assertStatus(Status.Code.INVALID_ARGUMENT, () -> stub.reserverCreneau(
                ReserverCreneauRequest.newBuilder().setRequestId("pas-un-uuid").setCreneauId(1).build()));
        assertStatus(Status.Code.INVALID_ARGUMENT, () -> stub.reserverCreneau(
                ReserverCreneauRequest.newBuilder().setRequestId(UUID.randomUUID().toString()).setCreneauId(0).build()));
        assertStatus(Status.Code.NOT_FOUND, () -> stub.reserverCreneau(
                ReserverCreneauRequest.newBuilder().setRequestId(UUID.randomUUID().toString())
                        .setCreneauId(999_999).build()));
    }

    @Test
    void liberationEstIdempotenteEtInconnueEstNotFound() {
        Creneau creneau = nouveauCreneau("Cardiologie", LocalDateTime.of(2027, 3, 1, 9, 0));
        String requestId = UUID.randomUUID().toString();
        LibererCreneauRequest request = LibererCreneauRequest.newBuilder()
                .setRequestId(requestId)
                .setCreneauId(creneau.getId())
                .build();

        assertThat(stub.libererCreneau(request).getCreneauId()).isEqualTo(creneau.getId());
        assertThat(stub.libererCreneau(request).getCreneauId()).isEqualTo(creneau.getId());
        assertStatus(Status.Code.NOT_FOUND, () -> stub.libererCreneau(LibererCreneauRequest.newBuilder()
                .setRequestId(UUID.randomUUID().toString()).setCreneauId(999_999).build()));
    }

    @Test
    void fluxDisponibleParcourtPlusieursPagesEtAppliqueLesFiltres() {
        LocalDateTime start = LocalDateTime.of(2027, 3, 1, 8, 0);
        List<Creneau> disponibles = new ArrayList<>();
        Praticien cardiologue = praticiens.save(new Praticien("Dr Test", "Cardiologie"));
        for (int i = 0; i < 101; i++) {
            LocalDateTime debut = start.plusHours(i);
            disponibles.add(new Creneau(cardiologue, debut, debut.plusMinutes(30)));
        }
        creneaux.saveAll(disponibles);
        Creneau enAttente = disponibles.get(0);
        stub.reserverCreneau(reservation(UUID.randomUUID().toString(), enAttente));
        nouveauCreneau("ORL", start.plusHours(102));

        List<com.medirdv.praticien.grpc.Creneau> actual = lireTous(
                ListerCreneauxDisponiblesRequest.getDefaultInstance());
        assertThat(actual).hasSize(101);
        assertThat(actual.stream().filter(slot -> slot.getSpecialite().equals("Cardiologie"))).hasSize(100);
        assertThat(actual.stream().filter(slot -> slot.getSpecialite().equals("ORL"))).hasSize(1);

        List<com.medirdv.praticien.grpc.Creneau> filtered = lireTous(
                ListerCreneauxDisponiblesRequest.newBuilder()
                        .setSpecialite("cardiologie")
                        .setDu(start.plusHours(1).toString())
                        .setAu(start.plusHours(10).toString())
                        .build());
        assertThat(filtered).hasSize(9);
    }

    @Test
    void fluxSarreteQuandLeClientAnnule() {
        nouveauCreneau("Cardiologie", LocalDateTime.of(2027, 3, 1, 9, 0));
        @SuppressWarnings("unchecked")
        ServerCallStreamObserver<com.medirdv.praticien.grpc.Creneau> observer =
                mock(ServerCallStreamObserver.class);
        when(observer.isCancelled()).thenReturn(false, true);

        grpcService.listerCreneauxDisponibles(ListerCreneauxDisponiblesRequest.getDefaultInstance(), observer);

        verify(observer, never()).onNext(any());
        verify(observer, never()).onCompleted();
    }

    private Creneau nouveauCreneau(String specialite, LocalDateTime debut) {
        Praticien praticien = praticiens.save(new Praticien("Dr Test", specialite));
        return creneaux.save(new Creneau(praticien, debut, debut.plusMinutes(30)));
    }

    private ReserverCreneauRequest reservation(String requestId, Creneau creneau) {
        return ReserverCreneauRequest.newBuilder()
                .setRequestId(requestId)
                .setCreneauId(creneau.getId())
                .build();
    }

    private boolean reserverApresDepart(ReserverCreneauRequest request, CountDownLatch pretes,
                                        CountDownLatch depart) throws InterruptedException {
        pretes.countDown();
        depart.await();
        return stub.reserverCreneau(request).getDejaTraite();
    }

    private Status.Code reserverEtLireStatut(ReserverCreneauRequest request, CountDownLatch pretes,
                                              CountDownLatch depart) throws InterruptedException {
        pretes.countDown();
        depart.await();
        try {
            stub.reserverCreneau(request);
            return Status.Code.OK;
        } catch (StatusRuntimeException exception) {
            return exception.getStatus().getCode();
        }
    }

    private List<com.medirdv.praticien.grpc.Creneau> lireTous(ListerCreneauxDisponiblesRequest request) {
        List<com.medirdv.praticien.grpc.Creneau> result = new ArrayList<>();
        Iterator<com.medirdv.praticien.grpc.Creneau> stream = stub.listerCreneauxDisponibles(request);
        stream.forEachRemaining(result::add);
        return result;
    }

    private void assertStatus(Status.Code expected, Runnable action) {
        assertThatThrownBy(action::run)
                .isInstanceOf(StatusRuntimeException.class)
                .satisfies(exception -> assertThat(((StatusRuntimeException) exception).getStatus().getCode())
                        .isEqualTo(expected));
    }
}
