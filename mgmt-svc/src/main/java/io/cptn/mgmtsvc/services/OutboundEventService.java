package io.cptn.mgmtsvc.services;

import com.querydsl.core.types.Predicate;
import io.cptn.common.entities.OutboundEvent;
import io.cptn.common.repositories.OutboundEventRepository;
import io.cptn.common.services.CommonService;
import io.cptn.common.web.ListEntitiesParam;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/* @author: kc, created on 4/3/23 */
@Service
@RequiredArgsConstructor
public class OutboundEventService extends CommonService {

    private final OutboundEventRepository outboundEventRepository;

    public List<OutboundEvent> getAll(ListEntitiesParam param) {
        Pageable pageable = getPageable(param);
        return outboundEventRepository.findAll(pageable).stream().collect(Collectors.toList());
    }

    public List<OutboundEvent> getAll(ListEntitiesParam param, Predicate predicate) {
        Pageable pageable = getPageable(param);
        return outboundEventRepository.findAll(predicate, pageable).stream().collect(Collectors.toList());
    }

    public Optional<OutboundEvent> getById(UUID id) {
        return outboundEventRepository.findById(id);
    }

    public long count() {
        return count(null);
    }

    public long count(Predicate predicate) {
        if (predicate != null) {
            return outboundEventRepository.count(predicate);
        }

        return outboundEventRepository.count();
    }

    public void save(OutboundEvent outboundEvent) {
        outboundEventRepository.save(outboundEvent);
    }

    public void requeueFailedEventsInPipeline(UUID pipelineId) {
        outboundEventRepository.requeueFailedEvents(pipelineId);
    }
}
