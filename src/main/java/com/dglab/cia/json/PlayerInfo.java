package com.dglab.cia.json;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author doc
 */
public class PlayerInfo {
	private long steamId64;
	private byte team;
	private String name;

	public PlayerInfo(){}

	@JsonCreator()
	public PlayerInfo(
			@JsonProperty(value = "steamId64", required = true) long steamId64,
			@JsonProperty(value = "team", required = true) byte team) {
		this.steamId64 = steamId64;
		this.team = team;
	}

	public long getSteamId64() {
		return steamId64;
	}

	public void setSteamId64(long steamId64) {
		this.steamId64 = steamId64;
	}

	public byte getTeam() {
		return team;
	}

	public void setTeam(byte team) {
		this.team = team;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
