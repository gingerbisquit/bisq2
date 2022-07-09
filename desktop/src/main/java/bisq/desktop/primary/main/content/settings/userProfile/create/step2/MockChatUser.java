/*
 * This file is part of Bisq.
 *
 * Bisq is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Bisq is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Bisq. If not, see <http://www.gnu.org/licenses/>.
 */

package bisq.desktop.primary.main.content.settings.userProfile.create.step2;

import lombok.Getter;

@Getter
public class MockChatUser {
    private final String nickName;
    private final String tac;
    private final String credo;

    public MockChatUser(String nickName, String tac, String credo) {
        this.nickName = nickName;
        this.tac = tac;
        this.credo = credo;
    }

    @Override
    public String toString() {
        return nickName;
    }
}