package com.fpolizzi.smartlead.repository;

import com.fpolizzi.smartlead.entity.Ticket;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

    /** Loads each ticket's comment in the same query, so listing leads does not issue one query per row. */
    @Override
    @EntityGraph(attributePaths = "comment")
    Page<Ticket> findAll(Pageable pageable);
}
