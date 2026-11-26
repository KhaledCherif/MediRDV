package com.medirdv.praticien.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.medirdv.praticien.domain.Creneau;
import com.medirdv.praticien.domain.StatutCreneau;
import com.medirdv.praticien.dto.CreneauDisponible;

import java.time.LocalDateTime;
import java.util.List;

public interface CreneauRepository extends JpaRepository<Creneau, Long> {

    @Query("""
            select c from Creneau c
            where c.praticien.id = :praticienId
              and (:statut is null or c.statut = :statut)
              and c.debut >= :du and c.debut < :au
            order by c.debut
            """)
    List<Creneau> rechercher(@Param("praticienId") Long praticienId,
                             @Param("statut") StatutCreneau statut,
                             @Param("du") LocalDateTime du,
                             @Param("au") LocalDateTime au);

    @Query("""
            select new com.medirdv.praticien.dto.CreneauDisponible(
                c.id, p.id, p.nom, p.specialite, c.debut, c.fin)
            from Creneau c
            join c.praticien p
            where c.statut = com.medirdv.praticien.domain.StatutCreneau.LIBRE
              and (:specialite is null or lower(p.specialite) = lower(:specialite))
              and (:du is null or c.debut >= :du)
              and (:au is null or c.debut < :au)
              and (:dernierDebut is null or c.debut > :dernierDebut
                   or (c.debut = :dernierDebut and c.id > :dernierId))
            order by c.debut, c.id
            """)
    List<CreneauDisponible> rechercherDisponibles(@Param("specialite") String specialite,
                                                   @Param("du") LocalDateTime du,
                                                   @Param("au") LocalDateTime au,
                                                   @Param("dernierDebut") LocalDateTime dernierDebut,
                                                   @Param("dernierId") Long dernierId,
                                                   Pageable pageable);

    boolean existsByPraticienIdAndDebutLessThanAndFinGreaterThan(Long praticienId,
                                                                 LocalDateTime fin,
                                                                 LocalDateTime debut);

    boolean existsByPraticienIdAndIdNotAndDebutLessThanAndFinGreaterThan(Long praticienId,
                                                                          Long id,
                                                                          LocalDateTime fin,
                                                                          LocalDateTime debut);

    /**
     * Transition atomique LIBRE -> EN_ATTENTE. Retourne 1 si ce thread a gagné la course, 0 sinon.
     * La colonne version est incrémentée à la main : les UPDATE JPQL contournent @Version.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update Creneau c
               set c.statut = com.medirdv.praticien.domain.StatutCreneau.EN_ATTENTE,
                   c.version = c.version + 1
             where c.id = :id
               and c.statut = com.medirdv.praticien.domain.StatutCreneau.LIBRE
            """)
    int reserverSiLibre(@Param("id") Long id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update Creneau c
               set c.statut = com.medirdv.praticien.domain.StatutCreneau.RESERVE,
                   c.version = c.version + 1
             where c.id = :id
               and c.statut = com.medirdv.praticien.domain.StatutCreneau.EN_ATTENTE
            """)
    int accepterSiEnAttente(@Param("id") Long id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update Creneau c
               set c.statut = com.medirdv.praticien.domain.StatutCreneau.LIBRE,
                   c.version = c.version + 1
             where c.id = :id
               and c.statut = com.medirdv.praticien.domain.StatutCreneau.EN_ATTENTE
            """)
    int refuserSiEnAttente(@Param("id") Long id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update Creneau c
               set c.statut = com.medirdv.praticien.domain.StatutCreneau.LIBRE,
                   c.version = c.version + 1
             where c.id = :id
               and c.statut in (com.medirdv.praticien.domain.StatutCreneau.EN_ATTENTE,
                                com.medirdv.praticien.domain.StatutCreneau.RESERVE)
            """)
    int libererSiEnAttenteOuReserve(@Param("id") Long id);
}
