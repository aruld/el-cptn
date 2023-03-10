package com.elcptn.mgmtsvc.scheduler.processors;

import com.elcptn.mgmtsvc.entities.Event;
import com.elcptn.mgmtsvc.entities.OutboundWriteEvent;
import com.elcptn.mgmtsvc.entities.State;
import com.elcptn.mgmtsvc.repositories.EventRepository;
import com.elcptn.mgmtsvc.repositories.OutboundWriteEventRepository;
import com.elcptn.mgmtsvc.repositories.PipelineRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

/* @author: kc, created on 3/9/23 */
@Component
@Slf4j
@AllArgsConstructor
public class InboundEventProcessor {

    private final EventRepository eventRepository;

    private final PipelineRepository pipelineRepository;

    private final OutboundWriteEventRepository outboundWriteEventRepository;

    public void processEvent(Event event) {
        try {
            dispatchEventToPipelines(event);
            updateEvent(event.getId(), State.COMPLETED);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            updateEvent(event.getId(), State.FAILED);
        }
    }

    private void dispatchEventToPipelines(Event event) {
        pipelineRepository.findActivePipelinesBySource(event.getSource().getId()).forEach(pipeline -> {
            OutboundWriteEvent outboundWriteEvent = new OutboundWriteEvent(event, pipeline);
            outboundWriteEventRepository.save(outboundWriteEvent);
        });
    }

    private void updateEvent(UUID eventId, State state) {
        if (eventId == null) {
            return;
        }
        eventRepository.updateEventState(eventId, state);
    }


}
