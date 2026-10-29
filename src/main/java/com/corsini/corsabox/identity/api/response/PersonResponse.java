package com.corsini.corsabox.identity.api.response;

import com.corsini.corsabox.identity.application.service.PeopleService;
import com.corsini.corsabox.identity.domain.Friendship.Relationship;

public record PersonResponse(UserSummaryResponse user, Relationship relationship) {
    public static PersonResponse from(PeopleService.Person person) {
        return new PersonResponse(UserSummaryResponse.from(person.user()), person.relationship());
    }
}
