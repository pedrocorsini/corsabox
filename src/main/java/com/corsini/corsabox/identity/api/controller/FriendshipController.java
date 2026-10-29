package com.corsini.corsabox.identity.api.controller;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import com.corsini.corsabox.identity.api.response.PersonResponse;
import com.corsini.corsabox.identity.application.service.PeopleService;
import com.corsini.corsabox.identity.domain.Friendship.Relationship;
import com.corsini.corsabox.identity.domain.Friendship.RequestDirection;

@RestController
@RequestMapping("/api/friends")
public class FriendshipController {
    private final PeopleService people;
    public FriendshipController(PeopleService people) { this.people = people; }
    public record RelationshipResponse(Relationship relationship) {}
    private static UUID viewer(Jwt jwt) { return UUID.fromString(jwt.getSubject()); }

    @GetMapping
    public Page<PersonResponse> friends(@AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return people.friends(viewer(jwt), page, size).map(PersonResponse::from);
    }

    @GetMapping("/requests")
    public Page<PersonResponse> requests(@AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "RECEIVED") RequestDirection direction,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return people.requests(viewer(jwt), direction, page, size).map(PersonResponse::from);
    }

    @PostMapping("/{userId}")
    public RelationshipResponse request(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID userId) {
        return new RelationshipResponse(people.request(viewer(jwt), userId));
    }

    @PostMapping("/{userId}/accept")
    public RelationshipResponse accept(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID userId) {
        return new RelationshipResponse(people.accept(viewer(jwt), userId));
    }

    @DeleteMapping("/{userId}")
    public RelationshipResponse remove(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID userId) {
        return new RelationshipResponse(people.remove(viewer(jwt), userId));
    }
}
