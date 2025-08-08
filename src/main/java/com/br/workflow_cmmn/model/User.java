package com.br.workflow_cmmn.model;

import lombok.Data;

@Data
public class User {
    private String id;
    private String username;
    private String email;
    private String role;
    private String name;
}