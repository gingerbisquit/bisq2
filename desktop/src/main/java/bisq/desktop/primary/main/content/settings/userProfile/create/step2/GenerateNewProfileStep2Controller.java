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

import bisq.application.DefaultApplicationService;
import bisq.desktop.common.view.Controller;
import bisq.desktop.components.robohash.RoboHash;
import bisq.desktop.primary.overlay.OverlayController;
import bisq.desktop.primary.overlay.onboarding.profile.GenerateProfileModel;
import bisq.desktop.primary.overlay.onboarding.profile.GenerateProfileView;
import bisq.desktop.primary.overlay.onboarding.profile.TempIdentity;
import bisq.security.pow.ProofOfWorkService;
import bisq.social.user.ChatUserService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

@Slf4j
public class GenerateNewProfileStep2Controller implements Controller {
    protected final GenerateNewProfileStep2Model model;
    @Getter
    protected final GenerateNewProfileStep2View view;
    protected final ChatUserService chatUserService;
    protected final ProofOfWorkService proofOfWorkService;
//    private GenerateNewProfileStep2Model getGenerateNewProfileStep2Model;
//    private GenerateNewProfileStep2Model getGenerateNewProfileStep2View;

    protected Subscription nickNameSubscription;

    public GenerateNewProfileStep2Controller(DefaultApplicationService applicationService) {

        chatUserService = applicationService.getChatUserService();
        proofOfWorkService = applicationService.getSecurityService().getProofOfWorkService();

        model = getGenerateNewProfileStep2Model();
        view = getGenerateNewProfileStep2View();
    }
    protected GenerateNewProfileStep2View getGenerateNewProfileStep2View() {
        return new GenerateNewProfileStep2View(model, this);
    }

    protected GenerateNewProfileStep2Model getGenerateNewProfileStep2Model() {
        return new GenerateNewProfileStep2Model();
    }

    @Override
    public void onActivate() {
        nickNameSubscription = EasyBind.subscribe(model.getNickName(),
                nickName -> {
                    TempIdentity tempIdentity = model.getTempIdentity().get();
                    if (tempIdentity != null) {
                        model.getNymId().set(tempIdentity.getProfileId());
                    }

                    model.getCreateProfileButtonDisabled().set(model.getCreateProfileProgress().get() == -1 ||
                            nickName == null || nickName.isEmpty());
                });
        model.getRoboHashImage().set(RoboHash.getImage(chatUserService.getSelectedChatUserIdentity().get()
                .getChatUser().getProofOfWork().getPayload()));
    }

    @Override
    public void onDeactivate() {
        if (nickNameSubscription != null) {
            nickNameSubscription.unsubscribe();
        }
    }

    private void onSave() {
        OverlayController.hide();
    }
}
