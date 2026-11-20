package com.example.lucky4.controller;

import com.example.lucky4.model.Member;
import com.example.lucky4.model.Question;
import com.example.lucky4.model.SpinResult;
import com.example.lucky4.service.GameService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class GameController {

    private final GameService gameService;

    public GameController(GameService gameService) {
        this.gameService = gameService;
    }

    @GetMapping("/spin")
    public SpinResult spin() {
        return gameService.spin();
    }

    @GetMapping("/members")
    public List<Member> members() {
        return gameService.getMembers();
    }

    @GetMapping("/question")
    public Question question() {
        return gameService.randomQuestion();
    }
}
