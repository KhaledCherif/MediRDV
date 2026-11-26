package com.medirdv.praticien.grpc;

import io.grpc.stub.ServerCallStreamObserver;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.data.domain.PageRequest;
import com.medirdv.praticien.dto.CreneauDisponible;
import com.medirdv.praticien.repository.CreneauRepository;
import com.medirdv.praticien.service.GrpcRequestService;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

@GrpcService
public class PraticienGrpcService extends PraticienServiceGrpc.PraticienServiceImplBase {

    private static final int PAGE_SIZE = 100;
    private static final LocalDateTime MIN_DATE = LocalDate.of(1, 1, 1).atStartOfDay();
    private static final LocalDateTime MAX_DATE = LocalDate.of(9999, 12, 31).atTime(23, 59, 59, 999_999_999);

    private final GrpcRequestService requests;
    private final CreneauRepository creneaux;

    public PraticienGrpcService(GrpcRequestService requests, CreneauRepository creneaux) {
        this.requests = requests;
        this.creneaux = creneaux;
    }

    @Override
    public void reserverCreneau(ReserverCreneauRequest request,
                                StreamObserver<ReserverCreneauResponse> responseObserver) {
        GrpcRequestService.ReservationResult result =
                requests.reserver(request.getRequestId(), request.getCreneauId());
        responseObserver.onNext(ReserverCreneauResponse.newBuilder()
                .setCreneauId(request.getCreneauId())
                .setPraticienId(result.praticienId())
                .setDejaTraite(result.dejaTraite())
                .build());
        responseObserver.onCompleted();
    }

    @Override
    public void libererCreneau(LibererCreneauRequest request,
                               StreamObserver<LibererCreneauResponse> responseObserver) {
        requests.liberer(request.getRequestId(), request.getCreneauId());
        responseObserver.onNext(LibererCreneauResponse.newBuilder()
                .setCreneauId(request.getCreneauId())
                .build());
        responseObserver.onCompleted();
    }

    @Override
    public void listerCreneauxDisponibles(ListerCreneauxDisponiblesRequest request,
                                          StreamObserver<Creneau> responseObserver) {
        ServerCallStreamObserver<Creneau> serverObserver = asServerObserver(responseObserver);
        String specialite = request.getSpecialite().isBlank() ? "" : request.getSpecialite().trim();
        LocalDateTime parsedDu = parserDate(request.getDu(), "du");
        LocalDateTime parsedAu = parserDate(request.getAu(), "au");
        LocalDateTime du = parsedDu == null ? MIN_DATE : parsedDu;
        LocalDateTime au = parsedAu == null ? MAX_DATE : parsedAu;
        if (du != null && au != null && !du.isBefore(au)) {
            throw new IllegalArgumentException("du doit être antérieur à au");
        }

        LocalDateTime dernierDebut = MIN_DATE;
        Long dernierId = 0L;
        while (!serverObserver.isCancelled()) {
            List<CreneauDisponible> resultPage =
                    creneaux.rechercherDisponibles(
                            specialite, du, au, dernierDebut, dernierId, PageRequest.of(0, PAGE_SIZE));
            if (resultPage.isEmpty()) {
                break;
            }
            for (CreneauDisponible available : resultPage) {
                if (serverObserver.isCancelled()) {
                    return;
                }
                serverObserver.onNext(toProto(available));
                dernierDebut = available.debut();
                dernierId = available.id();
            }
            if (resultPage.size() < PAGE_SIZE) {
                break;
            }
        }
        if (!serverObserver.isCancelled()) {
            serverObserver.onCompleted();
        }
    }

    private LocalDateTime parserDate(String value, String field) {
        if (value.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(value);
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException(field + " doit être une date ISO-8601 sans fuseau", ex);
        }
    }

    private Creneau toProto(CreneauDisponible available) {
        return Creneau.newBuilder()
                .setId(available.id())
                .setPraticienId(available.praticienId())
                .setPraticienNom(available.praticienNom())
                .setSpecialite(available.specialite())
                .setDebut(available.debut().toString())
                .setFin(available.fin().toString())
                .build();
    }

    private ServerCallStreamObserver<Creneau> asServerObserver(StreamObserver<Creneau> observer) {
        if (!(observer instanceof ServerCallStreamObserver<?>)) {
            throw new IllegalStateException("Le streaming gRPC requiert un ServerCallStreamObserver");
        }
        return (ServerCallStreamObserver<Creneau>) observer;
    }
}
