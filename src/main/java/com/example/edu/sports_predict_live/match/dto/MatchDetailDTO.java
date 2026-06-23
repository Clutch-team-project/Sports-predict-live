package com.example.edu.sports_predict_live.match.dto;

import com.example.edu.sports_predict_live.livematch.dto.MatchEventDTO;
import com.example.edu.sports_predict_live.livematch.dto.MatchLineupDTO;
import com.example.edu.sports_predict_live.livematch.dto.MatchPlayerStatDTO;
import com.example.edu.sports_predict_live.livematch.dto.MatchTeamStatDTO;
import com.example.edu.sports_predict_live.player.dto.response.PlayerResponseDTO;

import java.util.List;
import java.util.Map;

public record MatchDetailDTO(
        MatchDTO game,
        List<MatchEventDTO> events,
        List<MatchLineupDTO> lineup,
        Map<String, List<MatchTeamStatDTO>> teamStats,
        List<MatchPlayerStatDTO> playerStats,
        List<PlayerResponseDTO> homePlayers,
        List<PlayerResponseDTO> awayPlayers
) {
}
