package com.elcptn.mgmtsvc.controllers;

import com.elcptn.common.entities.InboundEvent;
import com.elcptn.common.exceptions.NotFoundException;
import com.elcptn.common.web.ListEntitiesParam;
import com.elcptn.mgmtsvc.dto.InboundEventDto;
import com.elcptn.mgmtsvc.mappers.InboundEventMapper;
import com.elcptn.mgmtsvc.services.InboundEventService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/* @author: kc, created on 4/3/23 */
@RestController
@Slf4j
@Validated
@RequiredArgsConstructor
public class InboundEventController {

    private final InboundEventService inboundEventService;

    private final InboundEventMapper mapper;

    @GetMapping("/api/inbound_event")
    public ResponseEntity<List<InboundEventDto>> list(HttpServletRequest request) {
        ListEntitiesParam listParam = new ListEntitiesParam(request);
        List<InboundEventDto> inboundEventDtoList = inboundEventService.getAll(listParam).stream()
                .map(this::convert).collect(Collectors.toList());
        long count = inboundEventService.count();
        return ResponseEntity.ok().header("x-total-count", String.valueOf(count)).body(inboundEventDtoList);
    }

    @GetMapping("/api/inbound_event/{id}")
    public ResponseEntity<InboundEventDto> get(@PathVariable UUID id) {
        InboundEvent inboundEvent = getById(id);
        return ResponseEntity.ok().body(convert(inboundEvent));
    }

    private InboundEvent getById(UUID id) {
        Optional<InboundEvent> inboundEventOptional = inboundEventService.getById(id);
        if (inboundEventOptional.isEmpty()) {
            throw new NotFoundException("Event not found with the passed id");
        }

        return inboundEventOptional.get();
    }

    private InboundEventDto convert(com.elcptn.common.entities.InboundEvent inboundEvent) {
        return mapper.toDto(inboundEvent);
    }
}
