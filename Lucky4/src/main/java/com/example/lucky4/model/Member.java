package com.example.lucky4.model;

public record Member(
        int id,
        String name,
        String role,
        String course,
        String funFact,
        String favorite,
        String quote,
        String image,
        String panicLevel,
        String replyTime,
        String superpower
) {}