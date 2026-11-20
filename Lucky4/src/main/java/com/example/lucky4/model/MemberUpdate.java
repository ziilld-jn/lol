package com.example.lucky4.model;

/** The editable text fields of a member. (The photo is uploaded separately.) */
public record MemberUpdate(
        String name,
        String role,
        String course,
        String funFact,
        String favorite,
        String quote,
        String panicLevel,
        String replyTime,
        String superpower
) {}