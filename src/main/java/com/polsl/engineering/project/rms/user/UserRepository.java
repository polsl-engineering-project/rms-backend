package com.polsl.engineering.project.rms.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
interface UserRepository extends JpaRepository<User, UUID> {
    @Query("""
            UPDATE User u
            SET u.username = :username,
                u.firstName = :firstName,
                u.lastName = :lastName,
                u.phoneNumber = :phoneNumber
            WHERE u.id = :id
            """)
    @Modifying
    int updateById(
            @Param("id") UUID id,
            @Param("username") String username,
            @Param("firstName") String firstName,
            @Param("lastName") String lastName,
            @Param("phoneNumber") String phoneNumber
    );

    @Query("""
            DELETE FROM User u
            WHERE u.id = :id
            """
    )
    @Modifying
    int deleteUserById(@Param("id") UUID id);

    @Query("""
            SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END
            FROM User u
            WHERE u.username = :username
            """)
    boolean existsByUsername(@Param("username") String username);

    @Query("""
            SELECT u
            FROM User u
            WHERE u.username = :username
            """)
    Optional<User> findByUsername(
            @Param("username") String username
    );


}
