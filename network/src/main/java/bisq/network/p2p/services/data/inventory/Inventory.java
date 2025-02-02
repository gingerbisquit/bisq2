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

package bisq.network.p2p.services.data.inventory;

import bisq.common.proto.Proto;
import bisq.network.p2p.services.data.DataRequest;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
public record Inventory(Set<? extends DataRequest> entries, int numDropped) implements Proto {
    public bisq.network.protobuf.Inventory toProto() {
        return bisq.network.protobuf.Inventory.newBuilder()
                .addAllEntries(entries.stream().map(e -> e.toProto().getDataRequest()).collect(Collectors.toList()))
                .setNumDropped(numDropped)
                .build();
    }

    public static Inventory fromProto(bisq.network.protobuf.Inventory proto) {
        List<bisq.network.protobuf.DataRequest> entriesList = proto.getEntriesList();
        Set<DataRequest> entries = entriesList.stream()
                .map(DataRequest::fromProto)
                .collect(Collectors.toSet());
        return new Inventory(entries, proto.getNumDropped());
    }
}
