package com.dglab.cia.persistence;

import com.dglab.cia.ConnectionState;
import com.dglab.cia.RankedMode;
import com.dglab.cia.database.*;
import com.dglab.cia.json.RankAndStars;
import com.dglab.cia.json.RankUpdateDetails;
import com.dglab.cia.json.RankedPlayer;
import com.dglab.cia.json.Streak;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * @author doc
 */
@Service
public class RankServiceImpl implements RankService {
	private static Logger logger = Logger.getLogger(RankService.class.getName());
	private static final ZonedDateTime FIRST_SEASON = ZonedDateTime.of(2016, 7, 1, 0, 0, 0, 0, ZoneOffset.UTC);

	@Autowired
	private RankDao rankDao;

	@Autowired
	private MatchDao matchDao;

	@Override
	public Map<RankedMode, RankAndStars> getPlayerRanks(long steamId64) {
		Collection<PlayerRank> playerRanks = rankDao.findPlayerRanks(steamId64, getCurrentSeason());

		return playerRanks
				.stream()
				.collect(
						Collectors.toMap(
								rank -> rank.getPk().getMode(),
								rank -> {
									RankAndStars rankAndStars = new RankAndStars(rank.getRank(), rank.getStars());

									EliteStreak streak = rank.getStreak();

									if (streak != null) {
										rankAndStars.setStreak(new Streak(streak.getCurrentStreak(), streak.getMaxStreak()));
									}

									return rankAndStars;
								}
						)
				);
	}

	@Override
	public Map<Byte, Map<RankedMode, RankAndStars>> getPlayerRankHistory(long steamId64) {
		Map<Byte, Map<RankedMode, RankAndStars>> result = new HashMap<>();
		Collection<PlayerRank> ranks = rankDao.findPlayerRanks(steamId64);

		for (PlayerRank rank : ranks) {
			byte season = rank.getPk().getSeason();
			Map<RankedMode, RankAndStars> seasonRanks = result.get(season);

			if (seasonRanks == null) {
				seasonRanks = new HashMap<>();
				result.put(season, seasonRanks);
			}

			seasonRanks.put(rank.getPk().getMode(), new RankAndStars(rank.getRank(), rank.getStars()));
		}

		return result;
	}

	@Override
	public RankedMode getMatchRankedMode(Match match) {
		String mode = match.getMode();
		byte players = match.getPlayers();

		if ("2v2".equals(mode) && players == 4) {
			return RankedMode.TWO_TEAMS;
		}

		if ("3v3".equals(mode) && players == 6) {
			return RankedMode.TWO_TEAMS;
		}

		if ("ffa".equals(mode) && players == 2) {
			return RankedMode.DUEL;
		}

		return null;
	}

	@Override
	public Map<Long, RankAndStars> getMatchRanks(long matchId) {
		Match match = matchDao.getMatch(matchId);

		logger.info("GetMatchRanks " + match + " " + matchId);

		if (match == null) {
			return null;
		}

		RankedMode matchRankedMode = getMatchRankedMode(match);
		byte season = getCurrentSeason();

		logger.info("Mode and season: " + matchRankedMode + " " + season);

		if (matchRankedMode == null) {
			return null;
		}

		Map<Long, RankAndStars> result = new HashMap<>();

		for (PlayerMatchData player : match.getMatchData()) {
			long steamId64 = player.getPk().getSteamId64();

			PlayerRank rank = rankDao.findPlayerRank(player.getPk().getSteamId64(), season, matchRankedMode);

			result.put(steamId64, new RankAndStars(rank.getRank(), rank.getStars()));
		}

		return result;
	}

	@Override
	public byte getCurrentSeason() {
		long between = ChronoUnit.MONTHS.between(FIRST_SEASON, ZonedDateTime.now(ZoneOffset.UTC));
		return (byte) between;
	}

	@Override
	public RankUpdateDetails processMatchResults(long matchId) {
		Match match = matchDao.getMatch(matchId);

		if (match == null) {
			return null;
		}

		RankedMode matchRankedMode = getMatchRankedMode(match);
		byte season = getCurrentSeason();

		if (matchRankedMode == null) {
			return null;
		}

		Map<Long, RankAndStars> previous = new HashMap<>();
		Map<Long, RankAndStars> updated = new HashMap<>();

		List<PlayerRank> toUpdate = new ArrayList<>();

		for (PlayerMatchData player : match.getMatchData()) {
			List<PlayerRoundData> playerData = match
					.getRounds()
					.stream()
					.flatMap(round -> round.getPlayerRoundData().stream())
					.filter(data -> data.getPk().getSteamId64() == player.getPk().getSteamId64())
					.collect(Collectors.toList());

			long notPlayed = playerData
					.stream()
					.filter(data -> data.getHero() == null)
					.count();

			boolean abandoned = playerData
					.stream()
					.anyMatch(data -> data.getConnectionState() == ConnectionState.ABANDONED.ordinal());

			long steamId64 = player.getPk().getSteamId64();
			PlayerRank playerRank = rankDao.findPlayerRank(steamId64, season, matchRankedMode);
			EliteStreak streak = playerRank.getStreak();

			int stars = playerRank.getStars();

			RankAndStars oldRank = new RankAndStars(playerRank.getRank(), playerRank.getStars());
			previous.put(steamId64, oldRank);

            boolean won = player.getTeam() == match.getWinnerTeam() && !(abandoned || notPlayed >= 2);

            if (playerRank.getRank() == 1) {
				oldRank.setStreak(new Streak(streak.getCurrentStreak(), streak.getMaxStreak()));

				updateEliteStreak(playerRank, won);
            } else {
                stars = stars + (won ? 1 : -1);
            }

			if (stars > matchRankedMode.getStars()) {
				stars = matchRankedMode.getStars();

				int newRank = Math.max(1, playerRank.getRank() - 1);
				playerRank.setRank((byte) newRank);
				playerRank.setStars((byte) stars);
			} else if (stars <= 0) {
				stars = matchRankedMode.getStars();

				int newRank = Math.min(30, playerRank.getRank() + 1);
				playerRank.setRank((byte) newRank);
				playerRank.setStars((byte) stars);
			} else {
				playerRank.setStars((byte) stars);
			}

			boolean rankUpdated = oldRank.getRank() == playerRank.getRank() && oldRank.getStars() == playerRank.getStars();
			boolean streakUpdated = false;

			if (oldRank.getStreak() != null) {
				streakUpdated = oldRank.getStreak().getCurrent() != streak.getCurrentStreak();
			}

			if (!rankUpdated && !streakUpdated) {
				previous.remove(steamId64);
				continue;
			}

			RankAndStars newRank = new RankAndStars(playerRank.getRank(), playerRank.getStars());

			if (streak != null) {
				newRank.setStreak(new Streak(streak.getCurrentStreak(), streak.getMaxStreak()));
			}

			updated.put(steamId64, newRank);

			toUpdate.add(playerRank);
		}

		rankDao.save(toUpdate);

		RankUpdateDetails details = new RankUpdateDetails();
		details.setPrevious(previous);
		details.setUpdated(updated);

		return details;
	}

	private EliteStreak updateEliteStreak(PlayerRank rank, boolean won) {
		EliteStreak streak = rank.getStreak();

		if (won) {
			streak.setCurrentStreak((short) (streak.getCurrentStreak() + 1));
			streak.setMaxStreak((short) Math.max(streak.getMaxStreak(), streak.getCurrentStreak()));
		} else {
			streak.setCurrentStreak((short) 0);
		}

		return streak;
	}

	@Override
	public Map<RankedMode, List<RankedPlayer>> getTopPlayers() {
        Map<RankedMode, List<PlayerRank>> topPlayers = rankDao.findTopPlayers(getCurrentSeason(), 5);
        return topPlayers.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, entry ->
            entry.getValue().stream().map(
                    rank -> new RankedPlayer(rank.getPk().getSteamId64(), rank.getRank())
            ).collect(Collectors.toList())
        ));
	}
}