package com.fpolizzi.smartlead.repository;

import com.fpolizzi.smartlead.entity.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TicketRepository extends JpaRepository<Ticket, Long> {
}
