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

package bisq.social.user.profile;

import bisq.common.data.Pair;
import bisq.common.encoding.Hex;
import bisq.common.util.CollectionUtil;
import bisq.common.util.CompletableFutureUtils;
import bisq.common.util.StringUtils;
import bisq.identity.IdentityService;
import bisq.network.NetworkService;
import bisq.network.http.common.BaseHttpClient;
import bisq.network.http.common.HttpException;
import bisq.network.p2p.node.transport.Transport;
import bisq.persistence.Persistence;
import bisq.persistence.PersistenceClient;
import bisq.persistence.PersistenceService;
import bisq.security.DigestUtil;
import bisq.security.KeyGeneration;
import bisq.security.KeyPairService;
import bisq.security.SignatureUtil;
import bisq.social.user.*;
import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.gson.JsonSyntaxException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.PublicKey;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static bisq.security.SignatureUtil.bitcoinSigToDer;
import static bisq.security.SignatureUtil.formatMessageForSigning;
import static com.google.common.base.Preconditions.checkArgument;
import static java.util.concurrent.CompletableFuture.supplyAsync;

@Slf4j
public class UserProfileService implements PersistenceClient<UserProfileStore> {
    // For dev testing we use hard coded txId and a pubkeyhash to get real data from Bisq explorer
    private static final boolean USE_DEV_TEST_POB_VALUES = true;

    public static record Config(List<String> btcMempoolProviders,
                                List<String> bsqMempoolProviders) {
        public static Config from(com.typesafe.config.Config typeSafeConfig) {
            List<String> btcMempoolProviders = typeSafeConfig.getStringList("btcMempoolProviders");
            List<String> bsqMempoolProviders = typeSafeConfig.getStringList("bsqMempoolProviders");
            return new UserProfileService.Config(btcMempoolProviders, bsqMempoolProviders);
        }
    }

    @Getter
    private final UserProfileStore persistableStore = new UserProfileStore();
    @Getter
    private final Persistence<UserProfileStore> persistence;
    private final KeyPairService keyPairService;
    private final IdentityService identityService;
    private final NetworkService networkService;
    private final Object lock = new Object();
    private final Config config;

    public UserProfileService(PersistenceService persistenceService,
                              Config config,
                              KeyPairService keyPairService,
                              IdentityService identityService,
                              NetworkService networkService) {
        persistence = persistenceService.getOrCreatePersistence(this, persistableStore);
        this.config = config;
        this.keyPairService = keyPairService;
        this.identityService = identityService;
        this.networkService = networkService;
    }

    public CompletableFuture<Boolean> initialize() {
        log.info("initialize");
        return CompletableFuture.completedFuture(true);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // API
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public CompletableFuture<UserProfile> createNewInitializedUserProfile(String domainId,
                                                                          String keyId,
                                                                          KeyPair keyPair,
                                                                          Set<Entitlement> entitlements) {
        return identityService.createNewInitializedIdentity(domainId, keyId, keyPair)
                .thenApply(identity -> {
                    UserProfile userProfile = new UserProfile(identity, entitlements);
                    synchronized (lock) {
                        persistableStore.getUserProfiles().add(userProfile);
                        persistableStore.getSelectedUserProfile().set(userProfile);
                    }
                    persist();
                    return userProfile;
                });
    }

    public void selectUserProfile(UserProfile value) {
        persistableStore.getSelectedUserProfile().set(value);
        persist();
    }

    public boolean isDefaultUserProfileMissing() {
        return persistableStore.getUserProfiles().isEmpty();
    }

    public CompletableFuture<Optional<ChatUser.BurnInfo>> findBurnInfoAsync(byte[] pubKeyHash, Set<Entitlement> entitlements) {
        if (entitlements.stream().noneMatch(e -> e.entitlementType() == Entitlement.Type.LIQUIDITY_PROVIDER)) {
            return CompletableFuture.completedFuture(Optional.empty());
        }
        return CompletableFutureUtils.allOf(entitlements.stream()
                        .filter(e -> e.proof() instanceof Entitlement.ProofOfBurnProof)
                        .map(e -> (Entitlement.ProofOfBurnProof) e.proof())
                        .map(proof -> verifyProofOfBurn(Entitlement.Type.LIQUIDITY_PROVIDER, proof.txId(), pubKeyHash))
                )
                .thenApply(list -> {
                    List<Entitlement.ProofOfBurnProof> proofs = list.stream()
                            .filter(Optional::isPresent)
                            .map(Optional::get)
                            .sorted(Comparator.comparingLong(Entitlement.ProofOfBurnProof::date))
                            .collect(Collectors.toList());
                    if (proofs.isEmpty()) {
                        return Optional.empty();
                    } else {
                        long totalBsqBurned = proofs.stream()
                                .mapToLong(Entitlement.ProofOfBurnProof::burntAmount)
                                .sum();
                        long firstBurnDate = proofs.get(0).date();
                        return Optional.of(new ChatUser.BurnInfo(totalBsqBurned, firstBurnDate));
                    }
                });
    }

    public CompletableFuture<Optional<Entitlement.ProofOfBurnProof>> verifyProofOfBurn(Entitlement.Type type, String proofOfBurnTxId, String pubKeyHash) {
        return verifyProofOfBurn(type, proofOfBurnTxId, Hex.decode(pubKeyHash));
    }

    public CompletableFuture<Optional<Entitlement.ProofOfBurnProof>> verifyProofOfBurn(Entitlement.Type type,
                                                                                       String _txId,
                                                                                       byte[] pubKeyHash) {
        String txId;
        // We use as preImage in the DAO String.getBytes(Charsets.UTF_8) to get bytes from the input string (not hex 
        // as hex would be more restrictive for arbitrary inputs)
        byte[] preImage;
        String pubKeyHashAsHex;
        if (USE_DEV_TEST_POB_VALUES) {
            // BSQ proof of burn tx (mainnet): ac57b3d6bdda9976391217e6d0ecbea9b050177fd284c2b199ede383189123c7
            // pubkeyhash (preimage for POB tx) 6a4e52f31a24300fd2a03766b5ea6e4abf289609
            // op return hash 9593f12a86fcb6ca72ed621c208b9370ff8f5112 
            txId = "ac57b3d6bdda9976391217e6d0ecbea9b050177fd284c2b199ede383189123c7";
            pubKeyHashAsHex = "6a4e52f31a24300fd2a03766b5ea6e4abf289609";
        } else {
            txId = _txId;
            pubKeyHashAsHex = Hex.encode(pubKeyHash);
        }
        preImage = pubKeyHashAsHex.getBytes(Charsets.UTF_8);

        Map<String, Entitlement.ProofOfBurnProof> verifiedProofOfBurnProofs = persistableStore.getVerifiedProofOfBurnProofs();
        if (verifiedProofOfBurnProofs.containsKey(pubKeyHashAsHex)) {
            return CompletableFuture.completedFuture(Optional.of(verifiedProofOfBurnProofs.get(pubKeyHashAsHex)));
        } else {
            return supplyAsync(() -> {
                try {
                    BaseHttpClient httpClient = getApiHttpClient(config.bsqMempoolProviders());
                    String jsonBsqTx = httpClient.get("tx/" + txId, Optional.of(new Pair<>("User-Agent", httpClient.userAgent)));
                    Preconditions.checkArgument(BsqTxValidator.initialSanityChecks(txId, jsonBsqTx), txId + "Bsq tx sanity check failed");
                    checkArgument(BsqTxValidator.isBsqTx(httpClient.getBaseUrl()), txId + " is Nnt a valid Bsq tx");
                    checkArgument(BsqTxValidator.isProofOfBurn(jsonBsqTx), txId + " is not a proof of burn transaction");
                    long burntAmount = BsqTxValidator.getBurntAmount(jsonBsqTx);
                    checkArgument(burntAmount >= getMinBurnAmount(type), "Insufficient BSQ burn. burntAmount=" + burntAmount);
                    String hashOfPreImage = Hex.encode(DigestUtil.hash(preImage));
                    BsqTxValidator.getOpReturnData(jsonBsqTx).ifPresentOrElse(
                            opReturnData -> {
                                // First 2 bytes in opReturn are type and version
                                byte[] hashFromOpReturnDataAsBytes = Arrays.copyOfRange(Hex.decode(opReturnData), 2, 22);
                                String hashFromOpReturnData = Hex.encode(hashFromOpReturnDataAsBytes);
                                checkArgument(hashOfPreImage.equalsIgnoreCase(hashFromOpReturnData),
                                        "pubKeyHash does not match opReturn data");
                            },
                            () -> {
                                throw new IllegalArgumentException("no opReturn element found");
                            });
                    long date = BsqTxValidator.getValidatedTxDate(jsonBsqTx);
                    Entitlement.ProofOfBurnProof verifiedProof = new Entitlement.ProofOfBurnProof(txId, burntAmount, date);
                    verifiedProofOfBurnProofs.put(pubKeyHashAsHex, verifiedProof);
                    persist();
                    return Optional.of(verifiedProof);
                } catch (IllegalArgumentException e) {
                    log.warn("check failed: {}", e.getMessage(), e);
                } catch (IOException e) {
                    if (e.getCause() instanceof HttpException &&
                            e.getCause().getMessage() != null &&
                            e.getCause().getMessage().equalsIgnoreCase("Bisq transaction not found")) {
                        log.error("Tx might be not confirmed yet", e);
                    } else {
                        log.warn("Mem pool query failed:", e);
                    }
                } catch (Throwable t) {
                    t.printStackTrace();
                }
                return Optional.empty();
            });
        }
    }

    public CompletableFuture<Optional<Entitlement.Proof>> verifyBondedRole(String txId, String signature, String pubKeyHash) {
        // inputs: txid, signature from Bisq1
        // process: get txinfo, grab pubkey from 1st output
        // verify provided signature matches with pubkey from 1st output and hash of provided identity pubkey
        return supplyAsync(() -> {
            try {
                BaseHttpClient httpClientBsq = getApiHttpClient(config.bsqMempoolProviders());
                BaseHttpClient httpClientBtc = getApiHttpClient(config.btcMempoolProviders());
                String jsonBsqTx = httpClientBsq.get("tx/" + txId, Optional.of(new Pair<>("User-Agent", httpClientBsq.userAgent)));
                String jsonBtcTx = httpClientBtc.get("tx/" + txId, Optional.of(new Pair<>("User-Agent", httpClientBtc.userAgent)));
                checkArgument(BsqTxValidator.initialSanityChecks(txId, jsonBsqTx), "bsq tx sanity checks");
                Preconditions.checkArgument(BtcTxValidator.initialSanityChecks(txId, jsonBtcTx), "btc tx sanity checks");
                checkArgument(BsqTxValidator.isBsqTx(httpClientBsq.getBaseUrl()), "isBsqTx");
                checkArgument(BsqTxValidator.isLockup(jsonBsqTx), "is lockup transaction");
                String signingPubkeyAsHex = BtcTxValidator.getFirstInputPubKey(jsonBtcTx);
                PublicKey signingPubKey = KeyGeneration.generatePublicFromCompressed(Hex.decode(signingPubkeyAsHex));
                boolean signatureMatches = SignatureUtil.verify(formatMessageForSigning(pubKeyHash), bitcoinSigToDer(signature), signingPubKey);
                checkArgument(signatureMatches, "signature");
                return Optional.of(new Entitlement.BondedRoleProof(txId, signature));
            } catch (IllegalArgumentException e) {
                log.warn("check failed: {}", e.getMessage(), e);
            } catch (IOException e) {
                log.warn("mempool query failed:", e);
            } catch (GeneralSecurityException e) {
                log.warn("signature validation failed:", e);
            } catch (JsonSyntaxException e) {
                log.warn("json decoding failed:", e);
            } catch (NullPointerException e) {
                log.error("unexpected failure:", e);
            }
            return Optional.empty();
        });
    }

    public CompletableFuture<Optional<Entitlement.Proof>> verifyModerator(String invitationCode, PublicKey publicKey) {
        //todo
        return CompletableFuture.completedFuture(Optional.of(new Entitlement.InvitationProof(invitationCode)));
    }

    private CompletableFuture<Boolean> createDefaultUserProfile() {
        String keyId = StringUtils.createUid();
        KeyPair keyPair = keyPairService.generateKeyPair();
        byte[] pubKeyBytes = keyPair.getPublic().getEncoded();
        byte[] pubKeyHash = DigestUtil.hash(pubKeyBytes);
        String useName = UserNameGenerator.fromHash(pubKeyHash);
        return createNewInitializedUserProfile(useName, keyId, keyPair, new HashSet<>())
                .thenApply(userProfile -> true);
    }

    //todo work out concept how to adjust those values
    public long getMinBurnAmount(Entitlement.Type type) {
        return switch (type) {
            //todo for dev testing reduced to 6 BSQ
            case LIQUIDITY_PROVIDER -> USE_DEV_TEST_POB_VALUES ? 600 : 5000;
            case CHANNEL_MODERATOR -> 10000;
            default -> 0;
        };
    }

    private BaseHttpClient getApiHttpClient(List<String> providerUrls) {
        String userAgent = "Bisq 2";
        String url = CollectionUtil.getRandomElement(providerUrls);
        Set<Transport.Type> supportedTransportTypes = networkService.getSupportedTransportTypes();
        Transport.Type transportType;
        if (supportedTransportTypes.contains(Transport.Type.CLEAR)) {
            transportType = Transport.Type.CLEAR;
        } else if (supportedTransportTypes.contains(Transport.Type.TOR)) {
            transportType = Transport.Type.TOR;
        } else {
            throw new RuntimeException("I2P is not supported yet");
        }
        return networkService.getHttpClient(url, userAgent, transportType);
    }
}
