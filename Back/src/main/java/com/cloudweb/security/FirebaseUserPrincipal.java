package com.cloudweb.security;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class FirebaseUserPrincipal {
    private final String uid;
    private final String email;
}
