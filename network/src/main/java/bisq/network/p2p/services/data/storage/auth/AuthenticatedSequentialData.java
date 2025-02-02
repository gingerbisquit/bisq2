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

package bisq.network.p2p.services.data.storage.auth;

import bisq.common.encoding.Hex;
import bisq.common.proto.Proto;
import com.google.protobuf.ByteString;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Data which ensures that the sequence of add or remove request is maintained correctly.
 * The data gets hashed and signed and need to be deterministic.
 */
@Slf4j
@Getter
@EqualsAndHashCode
public class AuthenticatedSequentialData implements Proto {
    public static AuthenticatedSequentialData from(AuthenticatedSequentialData data, int sequenceNumber) {
        return new AuthenticatedSequentialData(data.getAuthenticatedData(),
                sequenceNumber,
                data.getPubKeyHash(),
                data.getCreated());
    }

    protected final AuthenticatedData authenticatedData;
    protected final int sequenceNumber;
    protected final long created;
    protected final byte[] pubKeyHash;

    public AuthenticatedSequentialData(AuthenticatedData authenticatedData,
                                       int sequenceNumber,
                                       byte[] pubKeyHash,
                                       long created) {
        this.authenticatedData = authenticatedData;
        this.sequenceNumber = sequenceNumber;
        this.pubKeyHash = pubKeyHash;
        this.created = created;
    }

    public bisq.network.protobuf.AuthenticatedSequentialData toProto() {
        return bisq.network.protobuf.AuthenticatedSequentialData.newBuilder()
                .setAuthenticatedData(authenticatedData.toProto())
                .setSequenceNumber(sequenceNumber)
                .setPubKeyHash(ByteString.copyFrom(pubKeyHash))
                .setCreated(created)
                .build();
    }

    public static AuthenticatedSequentialData fromProto(bisq.network.protobuf.AuthenticatedSequentialData proto) {
        return new AuthenticatedSequentialData(AuthenticatedData.fromProto(proto.getAuthenticatedData()),
                proto.getSequenceNumber(),
                proto.getPubKeyHash().toByteArray(),
                proto.getCreated());
    }

    public boolean isExpired() {
        return (System.currentTimeMillis() - created) > authenticatedData.getMetaData().getTtl();
    }

    public boolean isSequenceNrInvalid(long seqNumberFromMap) {
        return sequenceNumber <= seqNumberFromMap;
    }

    @Override
    public String toString() {
        return "AuthenticatedSequentialData{" +
                "\r\n     authenticatedData=" + authenticatedData +
                ",\r\n     sequenceNumber=" + sequenceNumber +
                ",\r\n     created=" + created +
                ",\r\n     pubKeyHash=" + Hex.encode(pubKeyHash) +
                "\r\n}";
    }
}
