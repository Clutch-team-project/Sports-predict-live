package com.example.edu.sports_predict_live.prediction.dto.response;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public class RankingResponseDTO {

    private final int    rank;
    private final Long   userId;
    private final String nickname;
    private final long   totalPoints;
    private final long   correctCount;
    private final long   totalCount;

    // Redis 역직렬화용
    @JsonCreator
    public RankingResponseDTO(
            @JsonProperty("rank")         int rank,
            @JsonProperty("userId")       Long userId,
            @JsonProperty("nickname")     String nickname,
            @JsonProperty("totalPoints")  long totalPoints,
            @JsonProperty("correctCount") long correctCount,
            @JsonProperty("totalCount")   long totalCount
    ) {
        this.rank         = rank;
        this.userId       = userId;
        this.nickname     = nickname;
        this.totalPoints  = totalPoints;
        this.correctCount = correctCount;
        this.totalCount   = totalCount;
    }

    public RankingResponseDTO(int rank, Object[] row) {
        this.rank         = rank;
        this.userId       = (Long) row[0];
        this.nickname     = row[1] != null ? (String) row[1] : "익명";
        this.totalPoints  = row[2] != null ? ((Number) row[2]).longValue() : 0L;
        this.correctCount = row[3] != null ? ((Number) row[3]).longValue() : 0L;
        this.totalCount   = row[4] != null ? ((Number) row[4]).longValue() : 0L;
    }
}
