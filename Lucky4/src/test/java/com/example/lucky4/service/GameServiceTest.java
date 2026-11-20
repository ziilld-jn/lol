package com.example.lucky4.service;

import com.example.lucky4.model.SpinResult;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class GameServiceTest {

    @Test
    void hasFourMembers() {
        GameService service = new GameService(MemberStore.inMemory(), 0.25, new Random(1));
        assertEquals(4, service.getMembers().size());
    }

    @Test
    void alwaysJackpotWhenChanceIsOne() {
        GameService service = new GameService(MemberStore.inMemory(), 1.0, new Random(1));
        for (int i = 0; i < 200; i++) {
            SpinResult result = service.spin();
            assertTrue(result.match());
            assertNotNull(result.winner());
            assertEquals(4, result.reels().size());
            assertTrue(result.reels().stream().allMatch(m -> m.id() == result.winner().id()));
            assertEquals("profile", result.mediaType());
        }
    }

    @Test
    void neverJackpotWhenChanceIsZero_andReelsNeverAllMatch() {
        GameService service = new GameService(MemberStore.inMemory(), 0.0, new Random(1));
        for (int i = 0; i < 2000; i++) {
            SpinResult result = service.spin();
            assertFalse(result.match());
            assertNull(result.winner());
            assertEquals(4, result.reels().size());
            int firstId = result.reels().get(0).id();
            boolean allSame = result.reels().stream().allMatch(m -> m.id() == firstId);
            assertFalse(allSame, "A losing spin must not show four identical reels");
            assertTrue(result.media().startsWith("/media/meme"));
        }
    }

    @Test
    void jackpotRateRoughlyMatchesConfiguredChance() {
        GameService service = new GameService(MemberStore.inMemory(), 0.25, new Random(42));
        int jackpots = 0;
        int spins = 10_000;
        for (int i = 0; i < spins; i++) {
            if (service.spin().match()) jackpots++;
        }
        double rate = (double) jackpots / spins;
        assertTrue(rate > 0.21 && rate < 0.29, "Jackpot rate was " + rate);
    }

    @Test
    void chanceIsClampedToValidRange() {
        GameService high = new GameService(MemberStore.inMemory(), 5.0, new Random(1));
        assertTrue(high.spin().match());
        GameService low = new GameService(MemberStore.inMemory(), -3.0, new Random(1));
        assertFalse(low.spin().match());
    }

    @Test
    void randomQuestionIsNeverNull() {
        GameService service = new GameService(MemberStore.inMemory(), 0.25, new Random(1));
        for (int i = 0; i < 50; i++) {
            assertNotNull(service.randomQuestion());
        }
    }
}