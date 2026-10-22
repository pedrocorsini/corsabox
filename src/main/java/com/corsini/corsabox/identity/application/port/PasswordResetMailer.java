package com.corsini.corsabox.identity.application.port;

import com.corsini.corsabox.identity.domain.User;

public interface PasswordResetMailer {
    void send(User user, String rawToken);
}