package com.medirdv.praticien;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import com.medirdv.praticien.domain.Creneau;
import com.medirdv.praticien.domain.Praticien;
import com.medirdv.praticien.domain.StatutCreneau;
import com.medirdv.praticien.exception.ConflictException;
import com.medirdv.praticien.exception.NotFoundException;
import com.medirdv.praticien.repository.CreneauRepository;
import com.medirdv.praticien.repository.PraticienRepository;
import com.medirdv.praticien.service.CreneauService;

import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:test;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class CreneauReservationTest {

    @Autowired CreneauService service;
    @Autowired CreneauRepository creneaux;
    @Autowired PraticienRepository praticiens;

    private Creneau nouveauCreneau() {
        Praticien p = praticiens.save(new Praticien("Dr Test", "Cardiologie"));
        LocalDateTime debut = LocalDateTime.of(2026, 12, 1, 9, 0);
        return creneaux.save(new Creneau(p, debut, debut.plusMinutes(30)));
    }

    @Test
    void reserverPuisReReserverEchoueEn409() {
        Creneau c = nouveauCreneau();
        service.reserver(c.getId());
        assertThat(creneaux.findById(c.getId()).orElseThrow().getStatut()).isEqualTo(StatutCreneau.RESERVE);
        assertThatThrownBy(() -> service.reserver(c.getId())).isInstanceOf(ConflictException.class);
    }

    @Test
    void reserverCreneauInexistantEchoueEn404() {
        assertThatThrownBy(() -> service.reserver(999_999L)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void libererEstIdempotent() {
        Creneau c = nouveauCreneau();
        service.reserver(c.getId());
        service.liberer(c.getId());
        service.liberer(c.getId());
        assertThat(creneaux.findById(c.getId()).orElseThrow().getStatut()).isEqualTo(StatutCreneau.LIBRE);
    }

    @Test
    void vingtReservationsConcurrentesUnSeulGagnant() throws Exception {
        Creneau c = nouveauCreneau();
        int n = 20;
        ExecutorService pool = Executors.newFixedThreadPool(n);
        CountDownLatch depart = new CountDownLatch(1);
        AtomicInteger succes = new AtomicInteger();
        AtomicInteger conflits = new AtomicInteger();
        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            futures.add(pool.submit(() -> {
                depart.await();
                try {
                    service.reserver(c.getId());
                    succes.incrementAndGet();
                } catch (ConflictException e) {
                    conflits.incrementAndGet();
                }
                return null;
            }));
        }
        depart.countDown();
        for (Future<?> f : futures) f.get();
        pool.shutdown();

        assertThat(succes.get()).isEqualTo(1);
        assertThat(conflits.get()).isEqualTo(n - 1);
    }
}
