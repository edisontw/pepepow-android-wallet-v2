import { secp256k1 } from "@noble/curves/secp256k1";
import { sha256 } from "@noble/hashes/sha256";
import { ripemd160 } from "@noble/hashes/ripemd160";
import bs58 from "bs58";

export interface UTXO {
  txid: string;
  vout: number;
  satoshis: number;
  scriptPubKey: string; // hex string
}

// -------------------------------------------------------------
// Core Byte & Hex Serialization Helpers
// -------------------------------------------------------------

export function hexToBytes(hex: string): Uint8Array {
  const cleanHex = hex.startsWith("0x") ? hex.slice(2) : hex;
  if (cleanHex.length % 2 !== 0) {
    throw new Error("Invalid hex string length");
  }
  const bytes = new Uint8Array(cleanHex.length / 2);
  for (let i = 0; i < bytes.length; i++) {
    bytes[i] = parseInt(cleanHex.slice(i * 2, i * 2 + 2), 16);
  }
  return bytes;
}

export function bytesToHex(bytes: Uint8Array): string {
  return Array.from(bytes)
    .map(b => b.toString(16).padStart(2, "0"))
    .join("");
}

export function concatBytes(...arrays: Uint8Array[]): Uint8Array {
  const totalLength = arrays.reduce((acc, arr) => acc + arr.length, 0);
  const result = new Uint8Array(totalLength);
  let offset = 0;
  for (const arr of arrays) {
    result.set(arr, offset);
    offset += arr.length;
  }
  return result;
}

export function writeUInt32(val: number): Uint8Array {
  const buf = new Uint8Array(4);
  const view = new DataView(buf.buffer);
  view.setUint32(0, val, true); // true for little endian
  return buf;
}

export function writeInt64(val: number): Uint8Array {
  const buf = new Uint8Array(8);
  const view = new DataView(buf.buffer);
  view.setBigUint64(0, BigInt(val), true); // true for little endian
  return buf;
}

export function writeVarInt(val: number): Uint8Array {
  if (val < 0xfd) {
    return new Uint8Array([val]);
  } else if (val <= 0xffff) {
    const buf = new Uint8Array(3);
    buf[0] = 0xfd;
    const view = new DataView(buf.buffer);
    view.setUint16(1, val, true);
    return buf;
  } else if (val <= 0xffffffff) {
    const buf = new Uint8Array(5);
    buf[0] = 0xfe;
    const view = new DataView(buf.buffer);
    view.setUint32(1, val, true);
    return buf;
  } else {
    const buf = new Uint8Array(9);
    buf[0] = 0xff;
    const view = new DataView(buf.buffer);
    view.setBigUint64(1, BigInt(val), true);
    return buf;
  }
}

function doubleSha256(bytes: Uint8Array): Uint8Array {
  return sha256(sha256(bytes));
}

// -------------------------------------------------------------
// Base58Check & Key Derivation Utilities
// -------------------------------------------------------------

export function base58CheckEncode(version: number, payload: Uint8Array): string {
  const buf = new Uint8Array(1 + payload.length + 4);
  buf[0] = version;
  buf.set(payload, 1);
  const hash = doubleSha256(buf.subarray(0, 1 + payload.length));
  buf.set(hash.subarray(0, 4), 1 + payload.length);
  return bs58.encode(buf);
}

export function addressToHash160(address: string): Uint8Array {
  const decoded = bs58.decode(address.trim());
  if (decoded.length < 5) {
    throw new Error("Address is too short");
  }
  const payload = decoded.subarray(0, decoded.length - 4);
  const checksum = decoded.subarray(decoded.length - 4);
  const hash = doubleSha256(payload);
  for (let i = 0; i < 4; i++) {
    if (hash[i] !== checksum[i]) {
      throw new Error("Invalid address checksum");
    }
  }
  return decoded.subarray(1, decoded.length - 4);
}

export function derivePrivateKeyFromMnemonic(mnemonic: string): Uint8Array {
  return sha256(new TextEncoder().encode(mnemonic.trim()));
}

export function getAddressFromPrivateKey(privKey: Uint8Array, version = 55): string {
  const pubKey = secp256k1.getPublicKey(privKey, true);
  const hash = ripemd160(sha256(pubKey));
  return base58CheckEncode(version, hash);
}

export function privateKeyToWIF(privKey: Uint8Array, version = 204): string {
  const payload = new Uint8Array(33);
  payload.set(privKey, 0);
  payload[32] = 0x01; // compressed public key flag
  return base58CheckEncode(version, payload);
}

export function getP2PKHScript(hash160: Uint8Array): Uint8Array {
  const script = new Uint8Array(25);
  script[0] = 0x76; // OP_DUP
  script[1] = 0xa9; // OP_HASH160
  script[2] = 0x14; // Push 20 bytes
  script.set(hash160, 3);
  script[23] = 0x88; // OP_EQUALVERIFY
  script[24] = 0xac; // OP_CHECKSIG
  return script;
}

// -------------------------------------------------------------
// Transaction Construction and Signing
// -------------------------------------------------------------

export function createAndSignTransaction(
  privateKey: Uint8Array,
  utxos: UTXO[],
  recipientAddress: string,
  amountPepew: number,
  feePepew: number,
  senderAddress: string,
  p2pkhVersion = 55
): string {
  const amountSat = Math.round(amountPepew * 1e8);
  const feeSat = Math.round(feePepew * 1e8);
  const totalNeededSat = amountSat + feeSat;

  // 1. Select UTXOs
  let selectedSatoshis = 0;
  const selectedUtxos: UTXO[] = [];
  for (const utxo of utxos) {
    selectedUtxos.push(utxo);
    selectedSatoshis += utxo.satoshis;
    if (selectedSatoshis >= totalNeededSat) {
      break;
    }
  }

  if (selectedSatoshis < totalNeededSat) {
    throw new Error(
      `Insufficient UTXO balance. Selected inputs total ${(selectedSatoshis / 1e8).toFixed(4)} PEPEW, but need ${(totalNeededSat / 1e8).toFixed(4)} PEPEW.`
    );
  }

  const changeSat = selectedSatoshis - totalNeededSat;

  // 2. Decode recipient address & create outputs
  const recipientHash = addressToHash160(recipientAddress);
  const recipientScript = getP2PKHScript(recipientHash);

  const outputs: { scriptPubKey: Uint8Array; amount: number }[] = [
    { scriptPubKey: recipientScript, amount: amountSat }
  ];

  if (changeSat > 0) {
    const senderHash = addressToHash160(senderAddress);
    const changeScript = getP2PKHScript(senderHash);
    outputs.push({ scriptPubKey: changeScript, amount: changeSat });
  }

  // 3. Sign each input
  const signedScriptSigs: Uint8Array[] = [];
  const pubKey = secp256k1.getPublicKey(privateKey, true);

  for (let i = 0; i < selectedUtxos.length; i++) {
    const signInputs = selectedUtxos.map((utxo, idx) => {
      // Set scriptPubKey for the input being signed, others empty
      const scriptSig = idx === i ? hexToBytes(utxo.scriptPubKey) : new Uint8Array(0);
      return {
        txid: utxo.txid,
        vout: utxo.vout,
        scriptSig
      };
    });

    // Serialize for double-sha256 hashing (with sighash type SIGHASH_ALL appended)
    const serializedForSig = concatBytes(
      writeUInt32(1), // version
      writeVarInt(signInputs.length),
      ...signInputs.map(input => concatBytes(
        hexToBytes(input.txid).reverse(),
        writeUInt32(input.vout),
        writeVarInt(input.scriptSig.length),
        input.scriptSig,
        writeUInt32(0xffffffff)
      )),
      writeVarInt(outputs.length),
      ...outputs.map(output => concatBytes(
        writeInt64(output.amount),
        writeVarInt(output.scriptPubKey.length),
        output.scriptPubKey
      )),
      writeUInt32(0), // locktime
      writeUInt32(1)  // sighash type SIGHASH_ALL
    );

    const sighash = doubleSha256(serializedForSig);
    const sig = secp256k1.sign(sighash, privateKey);
    const derSigBytes = hexToBytes(sig.toDERHex());
    const sigWithHashType = concatBytes(derSigBytes, new Uint8Array([1])); // SIGHASH_ALL byte

    const scriptSig = concatBytes(
      writeVarInt(sigWithHashType.length),
      sigWithHashType,
      writeVarInt(pubKey.length),
      pubKey
    );

    signedScriptSigs.push(scriptSig);
  }

  // 4. Build final serialized transaction
  const finalInputs = selectedUtxos.map((utxo, idx) => {
    return {
      txid: utxo.txid,
      vout: utxo.vout,
      scriptSig: signedScriptSigs[idx]
    };
  });

  const finalSerialized = concatBytes(
    writeUInt32(1), // version
    writeVarInt(finalInputs.length),
    ...finalInputs.map(input => concatBytes(
      hexToBytes(input.txid).reverse(),
      writeUInt32(input.vout),
      writeVarInt(input.scriptSig.length),
      input.scriptSig,
      writeUInt32(0xffffffff)
    )),
    writeVarInt(outputs.length),
    ...outputs.map(output => concatBytes(
      writeInt64(output.amount),
      writeVarInt(output.scriptPubKey.length),
      output.scriptPubKey
    )),
    writeUInt32(0) // locktime
  );

  return bytesToHex(finalSerialized);
}
