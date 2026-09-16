package com.thoughtsandopinions.thoughtsservice.repository;

import com.thoughtsandopinions.thoughtsservice.entity.UsersEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;


public interface UsersRepository extends JpaRepository<UsersEntity, Long> {

    Optional<UsersEntity> findByUsername(String username) ;

}
