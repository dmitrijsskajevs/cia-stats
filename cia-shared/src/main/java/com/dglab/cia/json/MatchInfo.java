package com.dglab.cia.json;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.Collection;
import java.util.HashSet;

/**
 * @author doc
 */
public class MatchInfo {
	private long matchId;
	private String mode;
	private String version;
	private Collection<PlayerInfo> players = new HashSet<>();
	private Instant dateTime;
	private MatchMap map;
    private byte winnerTeam;

	@JsonCreator()
	public MatchInfo(
			@JsonProperty(value = "mode", required = true) String mode,
			@JsonProperty(value = "version", required = true) String version,
			@JsonProperty(value = "players", required = true) Collection<PlayerInfo> players,
			@JsonProperty(value = "map", required = true) MatchMap map) {
		this.mode = mode;
		this.version = version;
		this.players = players;
		this.map = map;
	}

	public MatchInfo() {
	}

	public long getMatchId() {
		return matchId;
	}

	public void setMatchId(long matchId) {
		this.matchId = matchId;
	}

	public String getMode() {
		return mode;
	}

	public void setMode(String mode) {
		this.mode = mode;
	}

	public byte getPlayerNumber() {
		return (byte) players.size();
	}

	public Collection<PlayerInfo> getPlayers() {
		return players;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public Instant getDateTime() {
		return dateTime;
	}

	public void setDateTime(Instant dateTime) {
		this.dateTime = dateTime;
	}

    public byte getWinnerTeam() {
        return winnerTeam;
    }

    public void setWinnerTeam(byte winnerTeam) {
        this.winnerTeam = winnerTeam;
    }

	public MatchMap getMap() {
		return map;
	}

	public void setMap(MatchMap map) {
		this.map = map;
	}
}
