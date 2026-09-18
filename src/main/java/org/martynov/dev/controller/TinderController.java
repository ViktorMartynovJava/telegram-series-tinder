package org.martynov.dev.controller;

import org.martynov.dev.entity.Series;
import org.martynov.dev.service.MatchService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tinder")
public class TinderController {

    private final MatchService matchService;

    public TinderController(MatchService matchService) {
        this.matchService = matchService;
    }

    public record SwipeRequest(Long telegramId, Long seriesId, boolean isLiked) {}

    @GetMapping("/next")
    public Series getNext(@RequestParam Long telegramId) {
        return matchService.getNextSeries(telegramId);
    }

    @PostMapping("/swipe")
    public String swipe(@RequestBody SwipeRequest request) {
        boolean isMatch = matchService.processSwipe(
                request.telegramId(),
                request.seriesId(),
                request.isLiked()
        );
        return isMatch ? "MATCH" : "OK";
    }
}
