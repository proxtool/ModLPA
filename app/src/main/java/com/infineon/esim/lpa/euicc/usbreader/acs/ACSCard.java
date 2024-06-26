/*
 * THE SOURCE CODE AND ITS RELATED DOCUMENTATION IS PROVIDED "AS IS". INFINEON
 * TECHNOLOGIES MAKES NO OTHER WARRANTY OF ANY KIND,WHETHER EXPRESS,IMPLIED OR,
 * STATUTORY AND DISCLAIMS ANY AND ALL IMPLIED WARRANTIES OF MERCHANTABILITY,
 * SATISFACTORY QUALITY, NON INFRINGEMENT AND FITNESS FOR A PARTICULAR PURPOSE.
 *
 * THE SOURCE CODE AND DOCUMENTATION MAY INCLUDE ERRORS. INFINEON TECHNOLOGIES
 * RESERVES THE RIGHT TO INCORPORATE MODIFICATIONS TO THE SOURCE CODE IN LATER
 * REVISIONS OF IT, AND TO MAKE IMPROVEMENTS OR CHANGES IN THE DOCUMENTATION OR
 * THE PRODUCTS OR TECHNOLOGIES DESCRIBED THEREIN AT ANY TIME.
 *
 * INFINEON TECHNOLOGIES SHALL NOT BE LIABLE FOR ANY DIRECT, INDIRECT OR
 * CONSEQUENTIAL DAMAGE OR LIABILITY ARISING FROM YOUR USE OF THE SOURCE CODE OR
 * ANY DOCUMENTATION, INCLUDING BUT NOT LIMITED TO, LOST REVENUES, DATA OR
 * PROFITS, DAMAGES OF ANY SPECIAL, INCIDENTAL OR CONSEQUENTIAL NATURE, PUNITIVE
 * DAMAGES, LOSS OF PROPERTY OR LOSS OF PROFITS ARISING OUT OF OR IN CONNECTION
 * WITH THIS AGREEMENT, OR BEING UNUSABLE, EVEN IF ADVISED OF THE POSSIBILITY OR
 * PROBABILITY OF SUCH DAMAGES AND WHETHER A CLAIM FOR SUCH DAMAGE IS BASED UPON
 * WARRANTY, CONTRACT, TORT, NEGLIGENCE OR OTHERWISE.
 *
 * (C)Copyright INFINEON TECHNOLOGIES All rights reserved
 */

package com.infineon.esim.lpa.euicc.usbreader.acs;

import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;

import com.acs.smartcard.Reader;
import com.infineon.esim.lpa.euicc.base.generic.Atr;
import com.infineon.esim.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ACSCard {
    private static final String TAG = ACSCard.class.getName();
    private Context context;
    private UsbManager mManager;
    private Reader mReader;
    private String currentCardName;

    public ACSCard(Context context) {
        this.context = context;
        mManager = (UsbManager)context.getSystemService(Context.USB_SERVICE);
        mReader = new Reader(mManager);
        currentCardName = null;
    }

    List<String> getReaderNames() {
        List<String> euiccNames = new ArrayList<>();

        for (UsbDevice device : mManager.getDeviceList().values()) {
            if (mReader.isSupported(device)) {
                euiccNames.add(device.getDeviceName());
            }
        }

        return euiccNames;
    }

    boolean isConnected() {
        return (currentCardName != null);
    }

    void establishContext() throws Exception {
    }

    void releaseContext() throws Exception {
    }

    void connectCard(String cardName) throws Exception {
        byte[] atr;

        try {
            mReader.open(mManager.getDeviceList().get(cardName));
            mReader.getNumSlots();
            atr = mReader.power(0, Reader.CARD_COLD_RESET);
            mReader.setProtocol(0, Reader.PROTOCOL_UNDEFINED | Reader.PROTOCOL_T0);
        } catch (Exception e) {
            throw new Exception(TAG + e.getClass().getName());
        }

        if (!Atr.isAtrValid(atr)) {
            disconnectCard();
            Log.error(TAG, "eUICC not allowed!");
            throw new Exception("eUICC not allowed!");
        }

        currentCardName = cardName;
    }

    void disconnectCard() throws Exception {
        mReader.close();
        currentCardName = null;
    }

    void resetCard() throws Exception {
        String cardName;

        if (currentCardName == null) {
            return;
        }

        cardName = currentCardName;
        disconnectCard();
        connectCard(cardName);
    }

    byte[] transmitToCard(byte[] command) throws Exception {
        byte[] rxbuf = new byte[264];
        byte[] response = null;
        int responseLen = 0;

        responseLen = mReader.transmit(0, command, command.length, rxbuf, rxbuf.length);
        response = Arrays.copyOf(rxbuf, responseLen);

        return response;
    }
}
