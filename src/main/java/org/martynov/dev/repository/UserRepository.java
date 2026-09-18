package org.martynov.dev.repository;

import org.martynov.dev.entity.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<AppUser, Long> {}
