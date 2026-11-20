package com.example.lucky4.service;

import com.example.lucky4.model.Member;
import com.example.lucky4.model.Question;
import com.example.lucky4.model.SpinResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Service
public class GameService {

    private static final int MEME_COUNT = 4;

    private final MemberStore memberStore;
    private final Random random;
    private final double jackpotChance;

    private final List<Question> questions = List.of(
            new Question(1, "Who is most likely to be late to class?"),
            new Question(2, "Who is most likely to say 'I'll do it later' and forget?"),
            new Question(3, "Who would survive longest in a zombie apocalypse?"),
            new Question(4, "Who spends the most money on food?"),
            new Question(5, "Who is most likely to accidentally delete the project?"),
            new Question(6, "Who would become famous first?"),
            new Question(7, "Who is most likely to sleep during a meeting?"),
            new Question(8, "Who would panic first if the Wi-Fi stopped working?"),
            new Question(9, "Who is most likely to say 'easy lang' before making things complicated?"),
            new Question(10, "Who would you trust to present the entire project alone?"),
            new Question(11, "Who would send the group chat 'omw' while still in bed?"),
            new Question(12, "Who would blame the Wi-Fi for everything?"),
            new Question(13, "Who has 47 unread messages and 12 open tabs?"),
            new Question(14, "Who would win in a fight against a printer?"),
            new Question(15, "Who would start the group project the night before?")
    );

    /** Used by Spring. The chance is configurable in application.properties. */
    @Autowired
    public GameService(MemberStore memberStore,
                       @Value("${lucky4.jackpot-chance:0.25}") double jackpotChance) {
        this(memberStore, jackpotChance, new Random());
    }

    /** Used by tests, so results can be controlled. */
    GameService(MemberStore memberStore, double jackpotChance, Random random) {
        this.memberStore = memberStore;
        this.jackpotChance = Math.max(0.0, Math.min(1.0, jackpotChance));
        this.random = random;
    }

    public List<Member> getMembers() {
        return memberStore.getAll();
    }

    public SpinResult spin() {
        List<Member> members = memberStore.getAll();

        // Decide the outcome first, then build reels that match it.
        // (Pure random reels give a jackpot only 1 in 64 spins.)
        boolean jackpot = random.nextDouble() < jackpotChance;

        if (jackpot) {
            Member winner = randomMember(members);
            return new SpinResult(
                    List.of(winner, winner, winner, winner),
                    true,
                    winner,
                    randomJackpotMessage(),
                    "profile",
                    winner.image()
            );
        }

        int memeNumber = random.nextInt(MEME_COUNT) + 1;
        return new SpinResult(
                nonMatchingReels(members),
                false,
                null,
                randomFailMessage(),
                "image",
                "/media/meme" + memeNumber + ".svg"
        );
    }

    /** Four reels that are guaranteed NOT to all show the same member. */
    private List<Member> nonMatchingReels(List<Member> members) {
        List<Member> reels = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            reels.add(randomMember(members));
        }
        int firstId = reels.get(0).id();
        boolean allSame = reels.stream().allMatch(m -> m.id() == firstId);
        if (allSame && members.size() > 1) {
            // Swap the last reel for a different member.
            Member different;
            do {
                different = randomMember(members);
            } while (different.id() == firstId);
            reels.set(3, different);
        }
        return List.copyOf(reels);
    }

    public Question randomQuestion() {
        return questions.get(random.nextInt(questions.size()));
    }

    private Member randomMember(List<Member> members) {
        return members.get(random.nextInt(members.size()));
    }

    private String randomJackpotMessage() {
        String[] messages = {
                "JACKPOT! The machine has chosen you.",
                "JACKPOT! Please clap.",
                "The algorithm has chosen violence, and it's you.",
                "Congratulations! You've unlocked a groupmate.",
                "JACKPOT! Somewhere, a random number generator is smiling.",
                "Four of a kind! Frame this moment."
        };
        return messages[random.nextInt(messages.length)];
    }

    private String randomFailMessage() {
        String[] messages = {
                "The universe said NO.",
                "Bro really thought that was a jackpot.",
                "Your luck left the group chat.",
                "The machine has personal beef with you.",
                "Almost. But absolutely not.",
                "Maybe the real jackpot was the friends we made along the way.",
                "The machine looked at your luck and sighed.",
                "Error 404: Jackpot not found.",
                "Skill issue. Also luck issue.",
                "Have you tried blowing on the machine?",
                "Your jackpot is in another castle.",
                "That spin was sponsored by disappointment."
        };
        return messages[random.nextInt(messages.length)];
    }
}