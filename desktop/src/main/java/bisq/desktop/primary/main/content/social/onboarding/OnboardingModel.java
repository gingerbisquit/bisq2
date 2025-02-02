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

package bisq.desktop.primary.main.content.social.onboarding;

import bisq.desktop.NavigationTarget;
import bisq.desktop.common.view.NavigationModel;
import bisq.social.user.profile.UserProfileService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
public class OnboardingModel extends NavigationModel {
    private final UserProfileService userProfileService;

    public OnboardingModel(UserProfileService userProfileService) {
        this.userProfileService = userProfileService;
    }

    @Override
    public NavigationTarget getDefaultNavigationTarget() {
        return userProfileService.isDefaultUserProfileMissing() ?
                NavigationTarget.INIT_USER_PROFILE :
                NavigationTarget.ONBOARD_NEWBIE;
    }
}
