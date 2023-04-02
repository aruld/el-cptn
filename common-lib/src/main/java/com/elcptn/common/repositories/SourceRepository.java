package com.elcptn.common.repositories;

import com.elcptn.common.entities.Source;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/* @author: kc, created on 2/7/23 */

@Repository
public interface SourceRepository extends JpaRepository<Source, UUID> {

}