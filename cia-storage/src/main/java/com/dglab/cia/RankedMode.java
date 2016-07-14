package com.dglab.cia;

/**
 * @author doc
 */
public enum RankedMode {
	TWO_TEAMS((byte) 1, "teams"),
	DUEL((byte) 1, "duel");

	private byte stars;
	private String url;

	RankedMode(byte stars, String url) {
		this.stars = stars;
		this.url = url;
	}

	public byte getStars() {
		return stars;
	}

	public String getUrl() {
		return url;
	}
}
