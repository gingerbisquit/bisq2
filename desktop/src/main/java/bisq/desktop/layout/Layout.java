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

package bisq.desktop.layout;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import javax.annotation.Nullable;

public class Layout {
    public static final Insets PADDING = new Insets(20);
    public static final double SPACING = 20;

    public static void pinToAnchorPane(Node node,
                                       @Nullable Integer top,
                                       @Nullable Integer right,
                                       @Nullable Integer bottom,
                                       @Nullable Integer left) {
        if (top != null) AnchorPane.setTopAnchor(node, (double) top);
        if (right != null) AnchorPane.setRightAnchor(node, (double) right);
        if (bottom != null) AnchorPane.setBottomAnchor(node, (double) bottom);
        if (left != null) AnchorPane.setLeftAnchor(node, (double) left);
    }

    public static HBox hBoxWith(Node... nodes) {
        HBox hBox = new HBox();
        hBox.setSpacing(10);
        hBox.getChildren().addAll(nodes);
        return hBox;
    }

    public static VBox vBoxWith(Node... nodes) {
        VBox vBox = new VBox();
        vBox.setSpacing(10);
        vBox.getChildren().addAll(nodes);
        return vBox;
    }
}