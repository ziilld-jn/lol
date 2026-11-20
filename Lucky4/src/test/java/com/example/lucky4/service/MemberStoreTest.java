package com.example.lucky4.service;

import com.example.lucky4.model.Member;
import com.example.lucky4.model.MemberUpdate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class MemberStoreTest {

    private static MemberUpdate update(String name, String funFact) {
        return new MemberUpdate(name, "Role", "BSIT", funFact, "Tea", "Quote",
                "50%", "2 days", "Naps");
    }

    @Test
    void startsWithFourDefaultMembers() {
        MemberStore store = MemberStore.inMemory();
        assertEquals(4, store.getAll().size());
        assertTrue(store.find(2).isPresent());
        assertTrue(store.find(99).isEmpty());
    }

    @Test
    void defaultMembersHaveTheFunStats() {
        for (Member m : MemberStore.inMemory().getAll()) {
            assertFalse(m.panicLevel().isBlank());
            assertFalse(m.replyTime().isBlank());
            assertFalse(m.superpower().isBlank());
        }
    }

    @Test
    void updateChangesTextButKeepsPhoto() {
        MemberStore store = MemberStore.inMemory();
        String oldImage = store.find(2).orElseThrow().image();

        Member updated = store.update(2, update("  Alex  ", "Likes tea"));

        assertEquals("Alex", updated.name());          // trimmed
        assertEquals("Role", updated.role());
        assertEquals("50%", updated.panicLevel());
        assertEquals("Naps", updated.superpower());
        assertEquals(oldImage, updated.image());
        assertEquals("Alex", store.find(2).orElseThrow().name());
    }

    @Test
    void emptyNameIsRejected() {
        MemberStore store = MemberStore.inMemory();
        assertThrows(IllegalArgumentException.class, () -> store.update(1, update("   ", "f")));
    }

    @Test
    void tooLongValuesAreRejected() {
        MemberStore store = MemberStore.inMemory();
        String longText = "x".repeat(500);
        assertThrows(IllegalArgumentException.class, () -> store.update(1, update("Name", longText)));
    }

    @Test
    void unknownMemberIsRejected() {
        MemberStore store = MemberStore.inMemory();
        assertThrows(IllegalArgumentException.class, () -> store.update(99, update("Name", "f")));
    }

    @Test
    void editsSurviveARestart(@TempDir Path dir) {
        MemberStore first = new MemberStore(dir);
        first.update(3, update("José 😀", "Fun fact"));
        first.setImage(3, "/uploads/member3-123.jpg");

        MemberStore second = new MemberStore(dir); // simulates restarting the app
        Member m = second.find(3).orElseThrow();
        assertEquals("José 😀", m.name());
        assertEquals("Naps", m.superpower());
        assertEquals("/uploads/member3-123.jpg", m.image());

        // Members that were not edited keep their defaults.
        assertEquals("/images/member1.svg", second.find(1).orElseThrow().image());
    }
}