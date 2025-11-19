package com.polsl.engineering.project.rms.security.jwt;

public interface JwtSubjectExistenceByIdVerifier {
    boolean doesExist(String id);
}
