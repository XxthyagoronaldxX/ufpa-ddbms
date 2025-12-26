package com.thyagoronald.debug.models;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class UserModel {
    private Integer id;
    private String name;
    private String email;
}
