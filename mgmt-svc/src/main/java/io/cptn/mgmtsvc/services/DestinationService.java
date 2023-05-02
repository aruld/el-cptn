package io.cptn.mgmtsvc.services;

import io.cptn.common.entities.Destination;
import io.cptn.common.projections.DestinationView;
import io.cptn.common.repositories.DestinationRepository;
import io.cptn.common.services.CommonService;
import io.cptn.common.web.ListEntitiesParam;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.hibernate.Session;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/* @author: kc, created on 2/16/23 */
@Service
@RequiredArgsConstructor
public class DestinationService extends CommonService {
    private final DestinationRepository destinationRepository;
    @PersistenceContext
    private final EntityManager entityManager;

    public Destination create(Destination destination) {
        return save(destination);
    }

    @CacheEvict(value = "destination-proc", key = "#destination.id")
    public Destination update(Destination destination, boolean forceUpdate) {
        if (forceUpdate) {
            //force update if there are updates to config. JPA doesn't detect object changes within a list
            Session session = (Session) entityManager.getDelegate();
            session.evict(destination);
        }
        return save(destination);
    }

    @CacheEvict(value = "destination-proc", key = "#destination.id")
    public void delete(Destination destination) {
        destinationRepository.delete(destination);
    }

    private Destination save(Destination destination) {
        return destinationRepository.save(destination);
    }

    public List<DestinationView> getAll(ListEntitiesParam param) {
        Pageable pageable = getPageable(param);
        return destinationRepository.findAllProjectedBy(pageable).stream().collect(Collectors.toList());
    }

    public long count() {
        return destinationRepository.count();
    }


    public Optional<Destination> getById(UUID id) {
        return destinationRepository.findById(id);
    }
}