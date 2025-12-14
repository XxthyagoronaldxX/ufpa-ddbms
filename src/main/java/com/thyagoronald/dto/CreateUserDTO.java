package com.thyagoronald.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
@AllArgsConstructor
public class CreateUserDTO {
    private String name;
    private String email;
}
