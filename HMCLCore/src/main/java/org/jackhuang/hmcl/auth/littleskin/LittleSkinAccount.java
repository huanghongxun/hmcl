/*
 * Hello Minecraft! Launcher
 * Copyright (C) 2024 huangyuhui <huanghongxun2008@126.com> and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package org.jackhuang.hmcl.auth.littleskin;

import org.jackhuang.hmcl.auth.AuthInfo;
import org.jackhuang.hmcl.auth.AuthenticationException;
import org.jackhuang.hmcl.auth.OAuthAccount;
import org.jackhuang.hmcl.auth.ServerResponseMalformedException;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * @author Glavo
 */
public final class LittleSkinAccount extends OAuthAccount {
    private final LittleSkinService service;
    private LittleSkinSession session;
    private UUID characterUUID;

    private boolean authenticated = false;

    public LittleSkinAccount(LittleSkinService service, LittleSkinSession session) {
        this.service = service;
        this.session = session;
    }

    @Override
    public AuthInfo logIn() throws AuthenticationException {
        if (!authenticated) {
            if (service.validate(session)) {
                authenticated = true;
            } else {
                LittleSkinSession acquiredSession = service.refresh(session);
                if (!Objects.equals(acquiredSession.getIdToken().getSelectedProfile().getId(), session.getIdToken().getSelectedProfile().getId())) {
                    throw new ServerResponseMalformedException("Selected profile changed");
                }

                session = acquiredSession;

                authenticated = true;
                invalidate();
            }
        }

        return session.toAuthInfo();
    }

    @Override
    public AuthInfo logInWhenCredentialsExpired() throws AuthenticationException {
        LittleSkinSession acquiredSession = service.authenticate();
        UUID id = acquiredSession.getIdToken().getSelectedProfile().getId();

        if (!Objects.equals(characterUUID, id)) {
            throw new WrongAccountException(characterUUID, id);
        }

        if (acquiredSession.getIdToken() == null) {
            session = service.refresh(acquiredSession);
        } else {
            session = acquiredSession;
        }

        authenticated = true;
        invalidate();
        return session.toAuthInfo();
    }

    @Override
    public String getUsername() {
        return "";
    }

    @Override
    public String getCharacter() {
        return "";
    }

    @Override
    public UUID getUUID() {
        return null;
    }

    @Override
    public AuthInfo playOffline() throws AuthenticationException {
        return null;
    }

    @Override
    public Map<Object, Object> toStorage() {
        return null;
    }

    @Override
    public String getIdentifier() {
        return "";
    }
}
