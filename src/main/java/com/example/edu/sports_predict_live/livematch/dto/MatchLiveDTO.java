package com.example.edu.sports_predict_live.livematch.dto;

import java.util.List;

/*
 * GET /api/games/{matchId}/baseball-live 응답 구조.
 *
 * 지금은 MatchLiveDummyService가 이 DTO를 만들어서 반환한다.
 * 나중에 HTML fetch를 붙이면 프론트는 이 JSON을 읽어서 점수, 현재 이닝, 타자/투수, 댓글 preview 등을 화면에 꽂게 된다.
 */
public record MatchLiveDTO(
        Long matchId,
        String sport,
        String status,
        String currentInning,
        TeamScore homeTeam,
        TeamScore awayTeam,
        Score score,
        CurrentPlayer currentBatter,
        CurrentPlayer currentPitcher,
        Count count,
        Runners runners,
        List<InningScore> inningScores,
        List<LiveEvent> events,
        List<CommentPreview> comments,
        PredictionPreview prediction,
        ViewerState viewer
) {

    public record TeamScore(
            Long teamId,
            String name,
            String shortName,
            String logoText,
            String side
    ) {
    }

    public record Score(
            int home,
            int away
    ) {
    }

    public record CurrentPlayer(
            Long playerId,
            String name,
            String teamName,
            String description
    ) {
    }

    public record Count(
            int balls,
            int strikes,
            int outs
    ) {
    }

    public record Runners(
            boolean first,
            boolean second,
            boolean third
    ) {
    }

    public record InningScore(
            String teamName,
            List<String> innings,
            int runs,
            int hits,
            int errors,
            int basesOnBalls
    ) {
    }

    public record LiveEvent(
            String eventType,
            int eventTime,
            String eventPeriod,
            String title,
            String description
    ) {
    }

    public record CommentPreview(
            String userName,
            String text,
            String writtenAt
    ) {
    }

    public record PredictionPreview(
            String homeTeamName,
            String awayTeamName,
            int homePercent,
            int awayPercent,
            int homeVoteCount,
            int awayVoteCount,
            boolean closed
    ) {
    }

    public record ViewerState(
            boolean loggedIn,
            boolean favorite,
            boolean predictionJoined
    ) {
    }
}
