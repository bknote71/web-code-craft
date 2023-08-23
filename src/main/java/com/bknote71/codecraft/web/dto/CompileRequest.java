package com.bknote71.codecraft.web.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CompileRequest {
    int robotId;
    int specIndex;
    String code;
    String lang;
}
