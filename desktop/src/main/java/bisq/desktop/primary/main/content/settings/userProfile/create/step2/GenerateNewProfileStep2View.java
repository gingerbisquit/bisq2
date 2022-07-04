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

import bisq.desktop.common.view.Model;
import bisq.desktop.common.view.Navigation;
import bisq.desktop.common.view.NavigationTarget;
import bisq.desktop.common.view.View;
import bisq.desktop.components.robohash.RoboHash;
import bisq.desktop.primary.main.content.settings.userProfile.EditUserProfile;
import bisq.i18n.Res;
import bisq.social.user.ChatUser;
import bisq.social.user.ChatUserIdentity;
import bisq.social.user.ChatUserService;
import javafx.beans.property.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;




@Slf4j
public class GenerateNewProfileStep2View extends View<VBox, GenerateNewProfileStep2Model, GenerateNewProfileStep2Controller> {

    private final TextArea tacField;

    protected final ImageView roboIconView;

    private final TextArea bioField;

    private final Label tacFieldLabel;

    private final Label bioFieldLabel;
    private final Button saveButton, cancelButton;
    private final Label nickName, nym;
    private Subscription selectedChatUserPin;



    private static class Model implements bisq.desktop.common.view.Model {
        private final ChatUserIdentity chatUserIdentity;
//        private final ObjectProperty<Image> roboHashNode = new SimpleObjectProperty<>();
//        private final StringProperty nym = new SimpleStringProperty();
//        private final StringProperty nickName = new SimpleStringProperty();
//        private final StringProperty id = new SimpleStringProperty();
//        private final StringProperty bio = new SimpleStringProperty();
//        private final StringProperty terms = new SimpleStringProperty();
//        private final StringProperty reputationScore = new SimpleStringProperty();
//        private final StringProperty profileAge = new SimpleStringProperty();
//        private final BooleanProperty isEditMode = new SimpleBooleanProperty();
//        private final BooleanProperty isPublishing = new SimpleBooleanProperty();
//        private final IntegerProperty progress = new SimpleIntegerProperty();

        private Model(ChatUserIdentity chatUserIdentity) {
            this.chatUserIdentity = chatUserIdentity;
        }
    }

    public GenerateNewProfileStep2View(GenerateNewProfileStep2Model model, GenerateNewProfileStep2Controller controller) {
        super(new VBox(), model, controller);

        root.setAlignment(Pos.TOP_CENTER);
        root.setSpacing(8);
        root.setPadding(new Insets(10, 0, 10, 0));

        Label headLineLabel = new Label(Res.get("editUserProfile.headline"));
        headLineLabel.getStyleClass().add("bisq-text-headline-2");

        Label subtitleLabel = new Label(Res.get("editUserProfile.subTitle"));
        subtitleLabel.setTextAlignment(TextAlignment.CENTER);
        subtitleLabel.setMaxWidth(400);
        subtitleLabel.getStyleClass().addAll("bisq-text-3", "wrap-text");

        nickName = new Label();
        nickName.getStyleClass().addAll("bisq-text-9", "font-semi-bold");
        nickName.setAlignment(Pos.CENTER);

        roboIconView = new ImageView();
        int size = 128;
        roboIconView.setFitWidth(size);
        roboIconView.setFitHeight(size);

        nym = new Label();
        nym.getStyleClass().addAll("bisq-text-7");
        nym.setAlignment(Pos.CENTER);

        VBox nameAndIconBox = new VBox(10, nickName, roboIconView, nym);
        nameAndIconBox.setAlignment(Pos.TOP_CENTER);


        tacField = new TextArea();
        tacField.setMaxWidth(400);
        tacField.setMaxHeight(50);
        tacFieldLabel = new Label("Chat Rules");


        bioField = new TextArea();
        bioField.setMaxWidth(400);
        bioField.setMaxHeight(50);
        bioFieldLabel = new Label("Credo");

        cancelButton = new Button("Cancel");
        saveButton = new Button("Save");
        HBox saveCancel = new HBox();
        saveCancel.getChildren().addAll(cancelButton, saveButton);
        saveCancel.setSpacing(20);
        VBox credoTacSave = new VBox(bioFieldLabel,  bioField, tacFieldLabel, tacField, saveCancel);
        credoTacSave.setSpacing(20);

        HBox iconAndEdits = new HBox(nameAndIconBox, credoTacSave);
        iconAndEdits.setAlignment(Pos.TOP_CENTER);



        VBox.setMargin(headLineLabel, new Insets(40, 0, 0, 0));
        VBox.setMargin(subtitleLabel, new Insets(0, 0, 50, 0));
        root.getChildren().addAll(
                headLineLabel,
                subtitleLabel,
                iconAndEdits
        );
    }

    protected void onViewAttached() {
        roboIconView.imageProperty().bind(model.getRoboHashImage());
//        roboIconView.managedProperty().bind(model.getRoboHashIconVisible());
//        roboIconView.visibleProperty().bind(model.getRoboHashIconVisible());


    }

    @Override
    protected void onViewDetached() {
        roboIconView.imageProperty().unbind();

    }

}
