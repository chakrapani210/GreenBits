package com.greenaddress.greenapi;

import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.crypto.DeterministicKey;
import org.bitcoinj.crypto.TransactionSignature;

import java.util.List;

public abstract class ISigningWallet {
    protected static final int HARDENED = 0x80000000;
    protected static final int PASSWORD_PATH = 0x70617373 | HARDENED; // 'pass'
    protected static final byte[] PASSWORD_SALT = new byte[] {
        0x70, 0x61, 0x73, 0x73, 0x73, 0x61, 0x6c, 0x74 // 'passsalt'
    };

    public abstract boolean requiresPrevoutRawTxs(); // FIXME: Get rid of this

    public abstract DeterministicKey getSubAccountPublicKey(final int subAccount);
    public abstract List<byte[]> signTransaction(PreparedTransaction ptx);
    public abstract List<byte[]> signTransaction(final Transaction tx, final PreparedTransaction ptx, final List<Output> prevOuts);

    // FIXME: This is only needed until the challenge RPC is unified
    public abstract Object[] getChallengeArguments();
    public abstract String[] signChallenge(final String challengeString, final String[] challengePath);

    public static byte[] getTxSignature(final ECKey.ECDSASignature sig) {
        final TransactionSignature txSig = new TransactionSignature(sig, Transaction.SigHash.ALL, false);
        return txSig.encodeToBitcoin();
    }

    // Derive a local password for encryption of client side data.
    // This takes a hardened public key using PASSWORD_PATH and returns
    // the pbkdf2_hmac_sha512 of its serialized bytes with PASSWORD_SALT.
    public abstract byte[] getLocalEncryptionPassword();
}
