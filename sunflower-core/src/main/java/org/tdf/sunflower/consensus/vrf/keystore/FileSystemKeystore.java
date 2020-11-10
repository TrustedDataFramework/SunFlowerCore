/*
 * Copyright (c) [2019] [ <silk chain> ]
 * This file is part of the silk chain library.
 *
 * The silk chain library is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The silk chain library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with the silk chain library. If not, see <http://www.gnu.org/licenses/>.
 */
package org.tdf.sunflower.consensus.vrf.keystore;


import org.spongycastle.util.encoders.Hex;
import org.springframework.util.StringUtils;
import org.tdf.crypto.PrivateKey;
import org.tdf.sunflower.consensus.vrf.util.VrfUtil;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * VRF key store manager working in user file system.
 * <p>
 * Comply to go-ethereum key store format.
 * https://github.com/ethereum/wiki/wiki/Web3-Secret-Storage-Definition
 *
 * @author James Hu
 * @since 2019/6/15
 */
public class FileSystemKeystore implements Keystore {
    private static final KeystoreFormat keystoreFormat = new KeystoreFormat();
    private final String keystoreDir;

    public FileSystemKeystore(String keystoreDir) {
        this.keystoreDir = keystoreDir;
    }

    @Override
    public void removeKey(String pubkey) {
        getFiles().stream()
                .filter(f -> hasPubkeyInName(pubkey, f))
                .findFirst()
                .ifPresent(f -> f.delete());
    }

    @Override
    public void storeKey(PrivateKey key, String password) throws RuntimeException {
        final String pubkey = Hex.toHexString(key.generatePublicKey().getEncoded());
        if (hasStoredKey(pubkey)) {
            throw new RuntimeException("Keystore is already exist for pubkey: " + pubkey +
                    " Please remove old one first if you want to add with new password.");
        }

        final File keysFolder = getKeyStoreLocation().toFile();
        keysFolder.mkdirs();

        String content = keystoreFormat.toKeystore(key, password);
        storeRawKeystore(content, pubkey);
    }

    @Override
    public void storeRawKeystore(String content, String pubkey) throws RuntimeException {
        String fileName = "UTC--" + getISODate(VrfUtil.curTime()) + "--" + pubkey;
        try {
            Files.write(getKeyStoreLocation().resolve(fileName), Arrays.asList(content));
        } catch (IOException e) {
            throw new RuntimeException("Problem storing key for pubkey");
        }
    }

    /**
     * @return array of pubkey in format "0x123abc..."
     */
    @Override
    public String[] listStoredKeys() {
        final List<File> files = getFiles();
        return files.stream()
                .filter(f -> !f.isDirectory())
                .map(f -> f.getName().split("--"))
                .filter(n -> n != null && n.length == 3)
                .map(a -> "0x" + a[2])
                .toArray(size -> new String[size]);
    }

    /**
     * @return some loaded key or null
     */
    @Override
    public PrivateKey loadStoredKey(String pubkey, String password) throws RuntimeException {
        return getFiles().stream()
                .filter(f -> hasPubkeyInName(pubkey, f))
                .map(f -> {
                    try {
                        return Files.readAllLines(f.toPath())
                                .stream()
                                .collect(Collectors.joining(""));
                    } catch (IOException e) {
                        throw new RuntimeException("Problem reading keystore file for pubkey:" + pubkey);
                    }
                })
                .map(content -> keystoreFormat.fromKeystore(content, password))
                .findFirst()
                .orElse(null);
    }

    private boolean hasPubkeyInName(String pubkey, File file) {
        String trim;
        if (pubkey.startsWith("0x")) {
            trim = pubkey.substring(2, pubkey.length());
        } else {
            trim = pubkey;
        }

        return !file.isDirectory() && file.getName().toLowerCase().endsWith("--" + trim.toLowerCase());
    }

    @Override
    public boolean hasStoredKey(String pubkey) {
        return getFiles().stream()
                .filter(f -> hasPubkeyInName(pubkey, f))
                .findFirst()
                .isPresent();
    }

    private List<File> getFiles() {
        final File dir = getKeyStoreLocation().toFile();
        final File[] files = dir.listFiles();
        return files != null ? Arrays.asList(files) : Collections.emptyList();
    }

    private String getISODate(long milliseconds) {
        TimeZone tz = TimeZone.getTimeZone("UTC");
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH-mm'Z'");
        df.setTimeZone(tz);
        return df.format(new Date(milliseconds));
    }

    /**
     * @return platform dependent path to Ethereum folder
     */
    @Override
    public Path getKeyStoreLocation() {
        final String osName = System.getProperty("os.name").toLowerCase();
        String keystoreDir = "vrf/keystore";

        if (!StringUtils.isEmpty(this.keystoreDir)) {
            keystoreDir = this.keystoreDir;
        }

        if (osName.indexOf("win") >= 0) {
            return Paths.get(System.getenv("APPDATA") + "/sunflower/" + keystoreDir);
        } else if (osName.indexOf("mac") >= 0) {
            return Paths.get(System.getProperty("user.home") + "/Library/sunflower/" + keystoreDir);
        } else {    // must be linux/unix
            return Paths.get(System.getProperty("user.home") + "/.sunflower/" + keystoreDir);
        }
    }
}
