package org.example;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
public class ResultDTO {
    int userId;
    int problemId;
    int solvedTestNum;
    int totalTestNum;
}