package com.corsini.corsabox.identity.application.port;

import com.corsini.corsabox.identity.domain.User;

public interface TokenIssuer {
    String issue(User user);
}
