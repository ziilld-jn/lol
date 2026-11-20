package com.example.lucky4.model;

import java.util.List;

public record SpinResult(
        List<Member> reels,
        boolean match,
        Member winner,
        String message,
        String mediaType,
        String media
) {}
