package de.androidcrypto.android_hce_beginner_app;/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.nfc.cardemulation.HostApduService;
import android.os.Bundle;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class MyHostApduServiceSimple extends HostApduService {
    private static final String TAG = "HceBeginnerApp";
    // ISO-DEP command HEADER for selecting an AID.
    // Format: [Class | Instruction | Parameter 1 | Parameter 2]
//    private static final byte[] SELECT_APPLICATION_APDU = hexStringToByteArray("00a4040006f2233445566700");
//    private static final String GET_DATA_APDU_HEADER = "00CA0000";
//    private static final String PUT_DATA_APDU_HEADER = "00DA0000";
//    // "OK" status word sent in response to SELECT AID command (0x9000)
//    private static final byte[] SELECT_OK_SW = hexStringToByteArray("9000");
//    // "UNKNOWN" status word sent in response to invalid APDU command (0x0000)
//    private static final byte[] UNKNOWN_CMD_SW = hexStringToByteArray("0000");


    // --- APDU COMMANDS AND HEADERS ---
    private static final byte[] SELECT_APPLICATION_APDU = hexStringToByteArray("00a4040006f2233445566700");
    private static final byte[] GET_DATA_APDU_HEADER = hexStringToByteArray("00CA0000");
    private static final byte[] PUT_DATA_APDU_HEADER = hexStringToByteArray("00DA0000");

    // --- APDU STATUS WORDS (SW) ---
    private static final byte[] SW_OK = hexStringToByteArray("9000"); // Success
    private static final byte[] SW_UNKNOWN_INSTRUCTION = hexStringToByteArray("6D00"); // Instruction not supported
    private static final byte[] SW_WRONG_LENGTH = hexStringToByteArray("6700"); // Wrong length


    private byte[] fileContent01 = "Terminal partners".getBytes(StandardCharsets.UTF_8);
    private byte[] fileContent02 = "Let's make a deal!".getBytes(StandardCharsets.UTF_8);
    private byte[] fileContentUnknown = "HCE Beginner App Unknown".getBytes(StandardCharsets.UTF_8);

    /**
     * Called if the connection to the NFC card is lost, in order to let the application know the
     * cause for the disconnection (either a lost link, or another AID being selected by the
     * reader).
     *
     * @param reason Either DEACTIVATION_LINK_LOSS or DEACTIVATION_DESELECTED
     */
    @Override
    public void onDeactivated(int reason) {
        Log.d(TAG, "onDeactivated() reason: " + reason);
    }

    /**
     * This method will be called when a command APDU has been received from a remote device. A
     * response APDU can be provided directly by returning a byte-array in this method. In general
     * response APDUs must be sent as quickly as possible, given the fact that the user is likely
     * holding his device over an NFC reader when this method is called.
     *
     * <p class="note">If there are multiple services that have registered for the same AIDs in
     * their meta-data entry, you will only get called if the user has explicitly selected your
     * service, either as a default or just for the next tap.
     *
     * <p class="note">This method is running on the main thread of your application. If you
     * cannot return a response APDU immediately, return null and use the {@link
     * #sendResponseApdu(byte[])} method later.
     *
     * @param commandApdu The APDU that received from the remote device
     * @param extras      A bundle containing extra data. May be null.
     * @return a byte-array containing the response APDU, or null if no response APDU can be sent
     * at this point.
     */

    @Override
    public byte[] processCommandApdu(byte[] commandApdu, Bundle extras) {
        Log.i(TAG, "Received APDU: " + byteArrayToHexString(commandApdu));

        // 1. Handle SELECT Application APDU
        if (Arrays.equals(SELECT_APPLICATION_APDU, commandApdu)) {
            Log.i(TAG, "APDU matches SELECT_APPLICATION_APDU. Sending OK.");
            return SW_OK;
        }

        // 2. Handle GET DATA APDU
        if (arraysStartWith(commandApdu, GET_DATA_APDU_HEADER)) {
            Log.i(TAG, "APDU is a GET_DATA command.");
            // Your ReadFragment sends: 00 CA 00 00 01 01 00 (8 bytes total)
            // Lc (at index 4) = 0x01. This is the length of the data field.
            // The data field itself is just the file number (e.g., 0x01).

            // Check if Lc is exactly 1 (for the file number)
            if (commandApdu[4] != (byte) 0x01) {
                Log.e(TAG, "GET_DATA error: Lc is not 1.");
                return SW_WRONG_LENGTH;
            }

            int fileNumber = commandApdu[5];
            byte[] fileContent;

            if (fileNumber == 1) {
                fileContent = fileContent01.clone();
                Log.i(TAG, "File 1 requested.");
            } else if (fileNumber == 2) {
                fileContent = fileContent02.clone();
                Log.i(TAG, "File 2 requested.");
            } else {
                fileContent = fileContentUnknown.clone();
                Log.w(TAG, "Unknown file requested: " + fileNumber);
            }

            // Respond with [file content] + [status word 9000]
            byte[] response = new byte[fileContent.length + SW_OK.length];
            System.arraycopy(fileContent, 0, response, 0, fileContent.length);
            System.arraycopy(SW_OK, 0, response, fileContent.length, SW_OK.length);
            Log.i(TAG, "GET_DATA Response: " + byteArrayToHexString(response));
            return response;
        }

        // 3. Handle PUT DATA APDU
        if (arraysStartWith(commandApdu, PUT_DATA_APDU_HEADER)) {
            Log.i(TAG, "APDU is a PUT_DATA command.");
            // Your ReadFragment sends: 00 DA 00 00 Lc [file nr] [data...] 00
            // Lc (at index 4) = length of [file nr] + [data...]. So data length is Lc - 1.

            int lc = commandApdu[4] & 0xFF; // Length of data field (file nr + content)
            int fileNumber = commandApdu[5];
            int contentLength = lc - 1;

            // Basic sanity check
            if (contentLength < 0 || (commandApdu.length < 6 + contentLength)) {
                Log.e(TAG, "PUT_DATA error: Malformed APDU. Lc: " + lc + ", APDU length: " + commandApdu.length);
                return SW_WRONG_LENGTH;
            }

            byte[] newContent = new byte[contentLength];
            System.arraycopy(commandApdu, 6, newContent, 0, contentLength);

            Log.i(TAG, "PUT_DATA: fileNr: " + fileNumber + ", content: " + new String(newContent, StandardCharsets.UTF_8));

            if (fileNumber == 1) {
                fileContent01 = newContent;
            } else if (fileNumber == 2) {
                fileContent02 = newContent;
            } else {
                fileContentUnknown = newContent;
            }
            // A successful write operation returns just the status word
            return SW_OK;
        }

        // 4. Handle any other command
        Log.wtf(TAG, "Unknown APDU command received.");
        return SW_UNKNOWN_INSTRUCTION;
    }

    // This helper function is correct and does not need changes.
    boolean arraysStartWith(byte[] completeArray, byte[] compareArray) {
        if (completeArray == null || compareArray == null || completeArray.length < compareArray.length) {
            return false;
        }
        int n = compareArray.length;
        return ByteBuffer.wrap(completeArray, 0, n).equals(ByteBuffer.wrap(compareArray, 0, n));
    }

    // Utility methods are correct and do not need changes.
    public static String byteArrayToHexString(byte[] bytes) {
        if (bytes == null) return "null";
        final char[] hexArray = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
        char[] hexChars = new char[bytes.length * 2];
        int v;
        for (int j = 0; j < bytes.length; j++) {
            v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    public static byte[] hexStringToByteArray(String s) throws IllegalArgumentException {
        int len = s.length();
        if (len % 2 == 1) {
            throw new IllegalArgumentException("Hex string must have even number of characters");
        }
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }
}