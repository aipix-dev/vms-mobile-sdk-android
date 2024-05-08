package com.mobile.vms.player.rtsp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mobile.vms.player.rtsp.utils.NetUtils;

import java.io.IOException;
import java.io.InputStream;

class HeaderRtpParser {

    private static final String TAG = HeaderRtpParser.class.getSimpleName();

    private final static int RTP_HEADER_SIZE = 12;

    @Nullable
    static RtpHeader readHeader(@NonNull InputStream inputStream) throws IOException {
        // 24 01 00 1c 80 c8 00 06  7f 1d d2 c4
        // 24 01 00 1c 80 c8 00 06  13 9b cf 60
        // 24 02 01 12 80 e1 01 d2  00 07 43 f0
        byte[] header = new byte[RTP_HEADER_SIZE];
        // Skip 4 bytes (TCP only). No those bytes in UDP.
        NetUtils.readData(inputStream, header, 0, 4);
//        if (DEBUG && header[0] == 0x24)
//            logSdk(TAG, header[1] == 0 ? "RTP packet" : "RTCP packet");

        int packetSize = RtpHeader.getPacketSize(header);
//        if (DEBUG && packetSize != 0)
//            logSdk(TAG, "Packet size: " + packetSize);

        if (NetUtils.readData(inputStream, header, 0, header.length) == header.length) {
            RtpHeader rtpHeader = RtpHeader.parseData(header, packetSize);
            if (rtpHeader == null) {
                // Header not found. Possible keep-alive response. Search for another RTP header.
                boolean foundHeader = RtpHeader.searchForNextRtpHeader(inputStream, header);
                if (foundHeader) {
                    packetSize = RtpHeader.getPacketSize(header);
                    if (NetUtils.readData(inputStream, header, 0, header.length) == header.length)
                        return RtpHeader.parseData(header, packetSize);
                }
            } else {
                return rtpHeader;
            }
        }
        return null;
    }

    public static class RtpHeader {
        public int version;
        public int padding;
        public int extension;
        public int marker;
        public int payloadType;
        public int sequenceNumber;
        public long timeStamp;
        public long ssrc;
        public int payloadSize;

        // If RTP header found, return 4 bytes of the header
        private static boolean searchForNextRtpHeader(@NonNull InputStream inputStream, @NonNull byte[] header /*out*/) throws IOException {
            if (header.length < 4)
                throw new IOException("Invalid allocated buffer size");

            int bytesRemaining = 100000; // 100 KB max to check
            boolean foundFirstByte = false;
            boolean foundSecondByte = false;
            byte[] oneByte = new byte[1];
            // Search for {0x24, 0x00}
            do {
                if (bytesRemaining-- < 0)
                    return false;
                // Read 1 byte
                NetUtils.readData(inputStream, oneByte, 0, 1);
                if (foundFirstByte) {
                    // Found 0x24. Checking for 0x00-0x02.
                    if (oneByte[0] == 0x00)
                        foundSecondByte = true;
                    else
                        foundFirstByte = false;
                }
                if (!foundFirstByte && oneByte[0] == 0x24) {
                    // Found 0x24
                    foundFirstByte = true;
                }
            } while (!foundSecondByte);
            header[0] = 0x24;
            header[1] = oneByte[0];
            // Read 2 bytes more (packet size)
            NetUtils.readData(inputStream, header, 2, 2);
            return true;
        }

        @Nullable
        private static RtpHeader parseData(@NonNull byte[] header, int packetSize) {
            RtpHeader rtpHeader = new RtpHeader();
            rtpHeader.version = (header[0] & 0xFF) >> 6;
            if (rtpHeader.version != 2) {
//                if (DEBUG)
//                    Log.e(TAG,"Not a RTP packet (" + rtpHeader.version + ")");
                return null;
            }

            // 80 60 40 91 fd ab d4 2a
            // 80 c8 00 06
            rtpHeader.padding = (header[0] & 0x20) >> 5; // 0b00100100
            rtpHeader.extension = (header[0] & 0x10) >> 4;
            rtpHeader.marker = (header[1] & 0x80) >> 7;
            rtpHeader.payloadType = header[1] & 0x7F;
            rtpHeader.sequenceNumber = (header[3] & 0xFF) + ((header[2] & 0xFF) << 8);
            rtpHeader.timeStamp = (header[7] & 0xFF) + ((header[6] & 0xFF) << 8) + ((header[5] & 0xFF) << 16) + ((long) (header[4] & 0xFF) << 24) & 0xffffffffL;
            rtpHeader.ssrc = (header[7] & 0xFF) + ((header[6] & 0xFF) << 8) + ((header[5] & 0xFF) << 16) + ((long) (header[4] & 0xFF) << 24) & 0xffffffffL;
            rtpHeader.payloadSize = packetSize - RTP_HEADER_SIZE;
            return rtpHeader;
        }

        private static int getPacketSize(@NonNull byte[] header) {
            int packetSize = ((header[2] & 0xFF) << 8) | (header[3] & 0xFF);
//            if (DEBUG)
//                Log.d(TAG, "Packet size: " + packetSize);
            return packetSize;
        }

        public void dumpHeader() {
//            Log.d("RTP","RTP header version: " + version
//                    + ", padding: " + padding
//                    + ", ext: " + extension
//                    + ", cc: " + cc
//                    + ", marker: " + marker
//                    + ", payload type: " + payloadType
//                    + ", seq num: " + sequenceNumber
//                    + ", ts: " + timeStamp
//                    + ", ssrc: " + ssrc
//                    + ", payload size: " + payloadSize);
        }
    }
}
/*

    Метод parseData предназначен для анализа заголовка RTP (Real-time Transport Protocol) пакета. RTP используется в системах потоковой передачи мультимедиа.

    private static RtpHeader parseData(@NonNull byte[] header, int packetSize) {
    Этот метод принимает массив байтов header, который представляет собой заголовок RTP-пакета, и packetSize - размер всего RTP-пакета.

    RtpHeader rtpHeader = new RtpHeader();
    Создается новый объект RtpHeader, который будет заполнен данными из заголовка.

    rtpHeader.version = (header[0] & 0xFF) >> 6;
    Извлекается версия RTP-пакета из первого байта заголовка. Первые два бита первого байта указывают на версию. Операция & 0xFF преобразует байт в беззнаковое целое, а сдвиг >> 6 перемещает нужные биты в позицию единиц и десятков.

    if (rtpHeader.version != 2) {
    return null;
    }
    Проверяется, что версия RTP равна 2. Если это не так, метод возвращает null, что означает, что это не RTP-пакет или он неверной версии.

    rtpHeader.padding = (header[0] & 0x20) >> 5;
    Извлекается флаг padding из первого байта. Этот бит указывает, есть ли в пакете дополнительные заполнители.

    rtpHeader.extension = (header[0] & 0x10) >> 4;
    Извлекается флаг extension. Этот бит указывает на наличие расширенного заголовка.

    rtpHeader.marker = (header[1] & 0x80) >> 7;
    Извлекается флаг marker из второго байта. Этот бит используется для специфических приложений, например, для обозначения ключевых кадров в видеопотоках.

    rtpHeader.payloadType = header[1] & 0x7F;
    Определяется тип полезной нагрузки (payloadType), который описывает формат содержимого пакета.

    rtpHeader.sequenceNumber = (header[3] & 0xFF) + ((header[2] & 0xFF) << 8);
    Вычисляется порядковый номер пакета (sequenceNumber). Это 16-битное число, составленное из третьего и четвертого байтов заголовка.

    rtpHeader.timeStamp = (header[7] & 0xFF) + ((header[6] & 0xFF) << 8) + ((header[5] & 0xFF) << 16) + ((long) (header[4] & 0xFF) << 24) & 0xffffffffL;
    Вычисляется временная метка (timeStamp). Это 32-битное число, показывающее время создания пакета.

    rtpHeader.ssrc = (header[7] & 0xFF) + ((header[6] & 0xFF) << 8) + ((header[5] & 0xFF) << 16) + ((long) (header[4] & 0xFF) << 24) & 0xffffffffL;
    Определяется идентификатор источника (ssrc), который уникален для каждого потока в сеансе.

    rtpHeader.payloadSize = packetSize - RTP_HEADER_SIZE;
    Вычисляется размер полезной нагрузки (payloadSize) путем вычитания размера заголовка RTP из общего размера пакета.

    return rtpHeader;
    Возвращается объект RtpHeader, заполненный данными из заголовка пакета.

*/
