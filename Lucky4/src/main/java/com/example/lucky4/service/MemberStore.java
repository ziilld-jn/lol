package com.example.lucky4.service;

import com.example.lucky4.model.Member;
import com.example.lucky4.model.MemberUpdate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

/**
 * Holds the four group members. Edits are saved to data/members.properties,
 * so they survive restarts. The built-in defaults below are used for any
 * value that has not been edited yet.
 */
@Component
public class MemberStore {

    private static final Logger log = LoggerFactory.getLogger(MemberStore.class);

    // Starting values. Once you edit someone on the edit page, the saved value wins.
    private static List<Member> defaults() {
        return List.of(
                new Member(1, "Reigne Gabrielle", "The Organizer", "BS Information Technology",
                        "Can turn a simple task into a 12-step checklist.", "Pink things",
                        "Let me just finish this first.", "/images/member1.svg",
                        "42%", "Within 5 business minutes", "Turns chaos into a checklist"),
                new Member(2, "Member 2", "The Debugger", "BS Information Technology",
                        "Says 'it works on my computer' with confidence.", "Coffee",
                        "Wait, don't touch it. It was working.", "/images/member2.svg",
                        "87%", "3 business days", "Appears 5 minutes before the deadline"),
                new Member(3, "Member 3", "The Comedian", "BS Information Technology",
                        "Can make a serious meeting unserious in 3 seconds.", "Memes",
                        "Guys, hear me out...", "/images/member3.svg",
                        "65%", "After the meme is delivered", "Makes serious meetings unserious"),
                new Member(4, "Member 4", "The Survivor", "BS Information Technology",
                        "Has survived group projects, deadlines, and questionable code.", "Sleep",
                        "We can still submit it.", "/images/member4.svg",
                        "99%", "Eventually. Probably.", "Survives on 3 hours of sleep")
        );
    }

    private final Path file; // null = keep everything in memory only (used by tests)
    private volatile List<Member> members;

    @Autowired
    public MemberStore(@Value("${lucky4.data-dir:data}") String dataDir) {
        this(Paths.get(dataDir).toAbsolutePath());
    }

    /** Stores data in the given folder. */
    MemberStore(Path dataDir) {
        this.file = dataDir.resolve("members.properties");
        this.members = load();
    }

    private MemberStore() {
        this.file = null;
        this.members = defaults();
    }

    /** A store that never touches the disk. */
    public static MemberStore inMemory() {
        return new MemberStore();
    }

    public List<Member> getAll() {
        return members;
    }

    public Optional<Member> find(int id) {
        return members.stream().filter(m -> m.id() == id).findFirst();
    }

    public synchronized Member update(int id, MemberUpdate u) {
        Member old = find(id).orElseThrow(() -> new IllegalArgumentException("No member with id " + id));

        Member updated = new Member(
                id,
                clean(u.name(), 60, "Name", true),
                clean(u.role(), 60, "Role", false),
                clean(u.course(), 80, "Course", false),
                clean(u.funFact(), 200, "Fun fact", false),
                clean(u.favorite(), 80, "Favorite", false),
                clean(u.quote(), 200, "Quote", false),
                old.image(),
                clean(u.panicLevel(), 40, "Panic level", false),
                clean(u.replyTime(), 60, "Reply time", false),
                clean(u.superpower(), 100, "Superpower", false)
        );
        replace(updated);
        return updated;
    }

    public synchronized Member setImage(int id, String imagePath) {
        Member old = find(id).orElseThrow(() -> new IllegalArgumentException("No member with id " + id));
        Member updated = new Member(id, old.name(), old.role(), old.course(),
                old.funFact(), old.favorite(), old.quote(), imagePath,
                old.panicLevel(), old.replyTime(), old.superpower());
        replace(updated);
        return updated;
    }

    private static String clean(String value, int max, String label, boolean required) {
        String v = value == null ? "" : value.trim();
        if (required && v.isEmpty()) {
            throw new IllegalArgumentException(label + " can't be empty.");
        }
        if (v.length() > max) {
            throw new IllegalArgumentException(label + " must be " + max + " characters or fewer.");
        }
        return v;
    }

    private void replace(Member updated) {
        List<Member> next = new ArrayList<>(members);
        for (int i = 0; i < next.size(); i++) {
            if (next.get(i).id() == updated.id()) {
                next.set(i, updated);
            }
        }
        save(next);              // if saving fails, memory stays unchanged
        members = List.copyOf(next);
    }

    private List<Member> load() {
        List<Member> base = defaults();
        if (file == null || !Files.exists(file)) {
            return base;
        }
        Properties p = new Properties();
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            p.load(reader);
        } catch (IOException | IllegalArgumentException e) {
            log.warn("Could not read {}; using default members. {}", file, e.toString());
            return base;
        }
        List<Member> result = new ArrayList<>();
        for (Member m : base) {
            String k = "m" + m.id() + ".";
            result.add(new Member(
                    m.id(),
                    p.getProperty(k + "name", m.name()),
                    p.getProperty(k + "role", m.role()),
                    p.getProperty(k + "course", m.course()),
                    p.getProperty(k + "funFact", m.funFact()),
                    p.getProperty(k + "favorite", m.favorite()),
                    p.getProperty(k + "quote", m.quote()),
                    p.getProperty(k + "image", m.image()),
                    p.getProperty(k + "panicLevel", m.panicLevel()),
                    p.getProperty(k + "replyTime", m.replyTime()),
                    p.getProperty(k + "superpower", m.superpower())
            ));
        }
        return List.copyOf(result);
    }

    private void save(List<Member> list) {
        if (file == null) {
            return;
        }
        Properties p = new Properties();
        for (Member m : list) {
            String k = "m" + m.id() + ".";
            p.setProperty(k + "name", m.name());
            p.setProperty(k + "role", m.role());
            p.setProperty(k + "course", m.course());
            p.setProperty(k + "funFact", m.funFact());
            p.setProperty(k + "favorite", m.favorite());
            p.setProperty(k + "quote", m.quote());
            p.setProperty(k + "image", m.image());
            p.setProperty(k + "panicLevel", m.panicLevel());
            p.setProperty(k + "replyTime", m.replyTime());
            p.setProperty(k + "superpower", m.superpower());
        }
        try {
            Files.createDirectories(file.getParent());
            try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                p.store(writer, "Lucky 4 member profiles");
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Could not save members to " + file, e);
        }
    }
}