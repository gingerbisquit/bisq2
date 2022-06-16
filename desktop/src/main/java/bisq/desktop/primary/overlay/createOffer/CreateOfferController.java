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

package bisq.desktop.primary.overlay.createOffer;

import bisq.application.DefaultApplicationService;
import bisq.desktop.common.utils.Transitions;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.Navigation;
import bisq.desktop.common.view.NavigationController;
import bisq.desktop.common.view.NavigationTarget;
import bisq.desktop.primary.overlay.OverlayController;
import bisq.desktop.primary.overlay.createOffer.market.MarketController;
import bisq.desktop.primary.overlay.createOffer.amount.AmountController;
import bisq.desktop.primary.overlay.createOffer.complete.OfferCompletedController;
import bisq.desktop.primary.overlay.createOffer.direction.DirectionController;
import bisq.desktop.primary.overlay.createOffer.method.PaymentMethodController;
import bisq.i18n.Res;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.util.List;
import java.util.Optional;

@Slf4j
public class CreateOfferController extends NavigationController {
    private final DefaultApplicationService applicationService;
    @Getter
    private final CreateOfferModel model;
    @Getter
    private final CreateOfferView view;
    private final DirectionController directionController;
    private final MarketController marketController;
    private final AmountController amountController;
    private final PaymentMethodController paymentMethodController;
    private final OfferCompletedController offerCompletedController;
    private final ListChangeListener<String> paymentMethodsListener;
    private Subscription directionSubscription, marketSubscription, baseSideAmountSubscription,
            quoteSideAmountSubscription;

    public CreateOfferController(DefaultApplicationService applicationService) {
        super(NavigationTarget.CREATE_OFFER);

        this.applicationService = applicationService;
        model = new CreateOfferModel();
        view = new CreateOfferView(model, this);

        model.getChildTargets().addAll(List.of(
                NavigationTarget.CREATE_OFFER_DIRECTION,
                NavigationTarget.CREATE_OFFER_MARKET,
                NavigationTarget.CREATE_OFFER_AMOUNT,
                NavigationTarget.CREATE_OFFER_PAYMENT_METHOD,
                NavigationTarget.CREATE_OFFER_OFFER_COMPLETED
        ));

        directionController = new DirectionController(applicationService, this::onNext, this::setButtonsVisible);
        marketController = new MarketController(applicationService);
        amountController = new AmountController(applicationService);
        paymentMethodController = new PaymentMethodController(applicationService);
        offerCompletedController = new OfferCompletedController(applicationService, this::setButtonsVisible, this::reset);

        model.getSkipButtonText().set(Res.get("onboarding.navProgress.skip"));
        paymentMethodsListener = c -> {
            c.next();
            handlePaymentMethodsUpdate();
        };
    }


    @Override
    public void onActivate() {
        model.getNextButtonDisabled().set(false);
        OverlayController.setTransitionsType(Transitions.Type.VERY_DARK);

        directionSubscription = EasyBind.subscribe(directionController.getDirection(), direction -> {
            offerCompletedController.setDirection(direction);
            amountController.setDirection(direction);
        });
        marketSubscription = EasyBind.subscribe(marketController.getMarket(), market -> {
            offerCompletedController.setMarket(market);
            paymentMethodController.setMarket(market);
            amountController.setMarket(market);
        });
        baseSideAmountSubscription = EasyBind.subscribe(amountController.getBaseSideAmount(), offerCompletedController::setBaseSideAmount);
        quoteSideAmountSubscription = EasyBind.subscribe(amountController.getQuoteSideAmount(), offerCompletedController::setQuoteSideAmount);

        paymentMethodController.getPaymentMethods().addListener(paymentMethodsListener);
        offerCompletedController.setPaymentMethods(paymentMethodController.getPaymentMethods());
        handlePaymentMethodsUpdate();
    }

    @Override
    public void onDeactivate() {
        directionSubscription.unsubscribe();
        marketSubscription.unsubscribe();
        baseSideAmountSubscription.unsubscribe();
        quoteSideAmountSubscription.unsubscribe();

        paymentMethodController.getPaymentMethods().removeListener(paymentMethodsListener);
    }

    public void onNavigate(NavigationTarget navigationTarget, Optional<Object> data) {
        model.getNextButtonVisible().set(true);
        model.getBackButtonVisible().set(true);
        model.getSkipButtonVisible().set(true);
        model.getTopPaneBoxVisible().set(true);
        model.getNextButtonText().set(Res.get("next"));
        model.getBackButtonText().set(Res.get("back"));

        switch (navigationTarget) {
            case CREATE_OFFER_DIRECTION -> {
                model.getBackButtonVisible().set(false);
            }
            case CREATE_OFFER_MARKET -> {
            }
            case CREATE_OFFER_AMOUNT -> {
            }
            case CREATE_OFFER_PAYMENT_METHOD -> {
            }
            case CREATE_OFFER_OFFER_COMPLETED -> {
                model.getNextButtonVisible().set(false);
            }
            default -> {
            }
        }
    }

    @Override
    protected Optional<? extends Controller> createController(NavigationTarget navigationTarget) {
        switch (navigationTarget) {
            case CREATE_OFFER_DIRECTION -> {
                return Optional.of(directionController);
            }
            case CREATE_OFFER_MARKET -> {
                return Optional.of(marketController);
            }
            case CREATE_OFFER_AMOUNT -> {
                return Optional.of(amountController);
            }
            case CREATE_OFFER_PAYMENT_METHOD -> {
                return Optional.of(paymentMethodController);
            }
            case CREATE_OFFER_OFFER_COMPLETED -> {
                return Optional.of(offerCompletedController);
            }
            default -> {
                return Optional.empty();
            }
        }
    }

    public void onNext() {
        int nextIndex = model.getCurrentIndex().get() + 1;
        if (nextIndex < model.getChildTargets().size()) {
            model.setAnimateToRight(true);
            model.getCurrentIndex().set(nextIndex);
            NavigationTarget nextTarget = model.getChildTargets().get(nextIndex);
            model.getSelectedChildTarget().set(nextTarget);
            Navigation.navigateTo(nextTarget);
            updateNextButtonState();
        }
    }

    public void onBack() {
       /* if (model.getSelectedChildTarget().get() == CREATE_OFFER_OFFER_PUBLISHED) {
            OverlayController.hide();
            Navigation.navigateTo(NavigationTarget.DASHBOARD);
            reset();
        } else {*/
        int prevIndex = model.getCurrentIndex().get() - 1;
        if (prevIndex >= 0) {
            model.setAnimateToRight(false);
            model.getCurrentIndex().set(prevIndex);
            NavigationTarget nextTarget = model.getChildTargets().get(prevIndex);
            model.getSelectedChildTarget().set(nextTarget);
            Navigation.navigateTo(nextTarget);
            updateNextButtonState();
        }
        // }
    }

    public void onSkip() {
        Navigation.navigateTo(NavigationTarget.MAIN);
        OverlayController.hide();
    }

    public void onQuit() {
        applicationService.shutdown().thenAccept(__ -> Platform.exit());
    }

    private void reset() {
        model.getCurrentIndex().set(0);
        model.getSelectedChildTarget().set(model.getChildTargets().get(0));
        resetSelectedChildTarget();
    }

    private void handlePaymentMethodsUpdate() {
        offerCompletedController.setPaymentMethods(paymentMethodController.getPaymentMethods());
        updateNextButtonState();
    }

    private void updateNextButtonState() {
        if (NavigationTarget.CREATE_OFFER_PAYMENT_METHOD.equals(model.getSelectedChildTarget().get())) {
            model.getNextButtonDisabled().set(paymentMethodController.getPaymentMethods().isEmpty());
        } else {
            model.getNextButtonDisabled().set(false);
        }
    }

    private void setButtonsVisible(boolean value) {
        model.getBackButtonVisible().set(value && model.getSelectedChildTarget().get() != NavigationTarget.CREATE_OFFER_DIRECTION);
        model.getNextButtonVisible().set(value && model.getSelectedChildTarget().get() != NavigationTarget.CREATE_OFFER_OFFER_COMPLETED);
        model.getSkipButtonVisible().set(value);
    }
}