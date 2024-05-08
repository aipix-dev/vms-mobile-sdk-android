package com.mobile.vms.player.rtsp;

import androidx.annotation.Nullable;

import java.io.ByteArrayOutputStream;

class VideoRtpParserH265 {

    private static final String TAG = VideoRtpParserH265.class.getSimpleName();
    private final static int NAL_UNIT_TYPE_FU_H265 = 49;

    private ByteArrayOutputStream _nalStream = new ByteArrayOutputStream();

    @Nullable
    public byte[] processRtpPacketAndGetNalUnit(byte[] data, int length) {
        int fu_header_s = data[2] >> 7 & 0x01;  // Start marker 0x01 - mask for last item, ex. 11111111 -> 00000001, was first->become last
        int fu_header_e = data[2] >> 6 & 0x01;  // End marker, , ex. 11111111 -> 00000011, was second->become last
        int nalType = (data[0] & 0x7E) >> 1; // For H.265, the NAL type is determined by the first byte after the prefix
        if (nalType == NAL_UNIT_TYPE_FU_H265) {
//            String bits0 = String.format("%8s", Integer.toBinaryString(data[0] & 0xFF)).replace(' ', '0');
//            String bits1 = String.format("%8s", Integer.toBinaryString(data[1] & 0xFF)).replace(' ', '0');
//            String bits2 = String.format("%8s", Integer.toBinaryString(data[2] & 0xFF)).replace(' ', '0');
//            System.out.println("Биты: \n" + "bits0 = " + bits0 + "\n" + "bits1 = " + bits1 + "\n" + "bits2 = " + bits2);
            if (fu_header_s == 1) { // START of the fragmented NAL unit
                _nalStream.reset();
//                Log.d(TAG, "_nalStream 1 = " + Arrays.toString(data));
                _nalStream.write(new byte[]{0x00, 0x00, 0x00, 0x01}, 0, 4);
//                Log.d(TAG, "nul type 111 = " + (data[2] & 0xFC));

                byte newNalUnit = (byte) ((data[2] & 0x3F) << 1); // ex. 00111111 -> 01111110
                byte firstBit = (byte) (data[0] & 0x80); // get first bit
                byte lastBit = (byte) (data[0] & 0x01);  // get last bit
                byte reconstructedHeader = (byte) ((firstBit) | (newNalUnit) | lastBit);

                _nalStream.write(reconstructedHeader);
                _nalStream.write(data[1]);
                _nalStream.write(data, 3, length - 3);
//                Log.d(TAG, "_nalStream 2 = " + Arrays.toString(_nalStream.toByteArray()));
            } else if (fu_header_e == 1) { // END of the fragmented NAL unit
                _nalStream.write(data, 3, length - 3);
                return _nalStream.toByteArray(); // Return the complete NAL unit
            } else { // CONTINUATION packet of the fragmented NAL unit
                _nalStream.write(data, 3, length - 3);
            }
        } else { // Non-fragmented NAL unit case or unsupported type
            _nalStream.reset();
            _nalStream.write(new byte[]{0x00, 0x00, 0x00, 0x01}, 0, 4);
            _nalStream.write(data, 0, length);
            return _nalStream.toByteArray();
        }

        return null; // Return null if NAL unit is not completely reconstructed yet
    }

  /*  @Nullable
    public byte[] processRtpPacketAndGetNalUnit(@NonNull byte[] data, int length) {
        int fu_header_s = data[2] >> 7 & 0x01;  // start marker
        int fu_header_e = data[2] >> 6 & 0x01;  // end marker
        int nalType = (data[0] & 0x7E) >> 1; // Для H265, тип NAL определяется первым байтом после префикса
//        logSdk(TAG, "Packet new: nalType:" + nalType + " fu_header_s: " + fu_header_s + " fu_header_e: " + fu_header_e);
        switch (nalType) {
            case NAL_UNIT_TYPE_AP_H265:
                int offset = 2; // Пропускаем заголовок AP - Aggregation Packets
//                logSdk(TAG, "Packet NAL_UNIT_TYPE_AP_H265 : nalType:" + nalType + " fu_header_s: " + fu_header_s + " fu_header_e: " + fu_header_e);
                while (offset < length) {
                    int nalSize = ((data[offset] & 0xFF) << 8) | (data[offset + 1] & 0xFF); // Считываем размер NAL единицы
                    offset += 2; // Перемещаемся к NAL единице
                    if (offset + nalSize > length) {
                        // Ошибка: размер NAL единицы превышает размер пакета
                        return null;
                    }
                    // Здесь вы можете обработать каждую NAL единицу в AP, например, добавить в очередь или немедленно обработать
                    // Пример добавления NAL единицы с start code в очередь:
                    byte[] nalUnit = new byte[4 + nalSize];
                    nalUnit[0] = 0x00;
                    nalUnit[1] = 0x00;
                    nalUnit[2] = 0x00;
                    nalUnit[3] = 0x01;
                    System.arraycopy(data, offset, nalUnit, 4, nalSize);
                    // Здесь добавьте nalUnit в вашу очередь или обработайте его

                    offset += nalSize; // Перемещаемся к следующей NAL единице в AP
                }
                break;

            case NAL_UNIT_TYPE_FU_H265:
//            оригинальный NALU тайп в старте фрагментированного пакета
//            uint8_t value = (uint8_t)(data[2] & 0x3F);

//            формируем пакет с учетом нового тайпа
//            uint8_t newType = (uint8_t)(((value << 1) | 0x80) | data[0] >> 7);

//            добавляем то что лежало в первом байте
//            [nalUnit appendData: [NSMutableData dataWithBytes: &data[1] length: 1]];

//            записываем все что лежало после хедеров (3 байта)
//            [nalUnit appendData: [NSMutableData dataWithBytes: data + 3 length: length - 3]];
                if (fu_header_s == 1) {
                    // Начало
                    _nalEndFlag = false;
                    _packetNum = 1;
                    _bufferLength = length - 1;
                    _buffer[1] = new byte[_bufferLength];
                    _buffer[1][0] = (byte) ((data[2] & 0x3F) << 1 | 0x80 | data[0] >> 7);
                    _buffer[1][1] = data[1];
                    System.arraycopy(data, 3, _buffer[1], 2, length - 3);
//                    logSdk(TAG, "Packet START NAL_UNIT_TYPE_FU_H265 nalType: " + nalType + " fu_header_s: " + fu_header_s + " fu_header_e: " + fu_header_e + " _bufferLength: " + _bufferLength);
                } else if (fu_header_s == 0 && fu_header_e == 0) { // Продолжение
                    _nalEndFlag = false;
                    _packetNum++;
                    if (_packetNum >= _buffer.length) {
                        // Расширяем буфер
                        byte[][] tempBuffer = new byte[_buffer.length * 2][];
                        System.arraycopy(_buffer, 0, tempBuffer, 0, _buffer.length);
                        _buffer = tempBuffer;
                    }
                    _bufferLength += length - 3;
                    _buffer[_packetNum] = new byte[length - 3];
                    System.arraycopy(data, 3, _buffer[_packetNum], 0, length - 3);
//                    logSdk(TAG, "Packet CONTINUE NAL_UNIT_TYPE_FU_H265 nalType: " + nalType + " fu_header_s: " + fu_header_s + " fu_header_e: " + fu_header_e + " _bufferLength: " + _bufferLength);
                } else if (fu_header_e == 1) { // Конец
                    _nalEndFlag = true;
                    _nalUnit = new byte[_bufferLength + length + 1]; // was "- 3 + 4
                    _nalUnit[0] = 0x00;
                    _nalUnit[1] = 0x00;
                    _nalUnit[2] = 0x00;
                    _nalUnit[3] = 0x01;
                    int tmpLen = 4;

                    try {
                        if (_buffer != null && _buffer.length != 0 && _buffer[1].length != 0) {
                            System.arraycopy(_buffer[1], 0, _nalUnit, tmpLen, _buffer[1].length); // java.lang.NullPointerException: Attempt to get length of null array
                            tmpLen += _buffer[1].length;
                            for (int i = 2; i < _packetNum + 1; ++i) {
                                System.arraycopy(_buffer[i], 0, _nalUnit, tmpLen, _buffer[i].length);
                                tmpLen += _buffer[i].length;
                            }
                        }
                        System.arraycopy(data, 3, _nalUnit, tmpLen, length - 3);
                    } catch (Exception e) {
                        e.printStackTrace();
                        break;
                    }
                    logSdk(TAG, "Packet END NAL_UNIT_TYPE_FU_H265 nalType: " + nalType + " fu_header_s: " + fu_header_s + " fu_header_e: " + fu_header_e + " _bufferLength: " + _bufferLength);
                }
                break;
            default:
//               if (nalType != 1)
//                   logSdk(TAG, "Packet DEFAULT nalType: " + nalType + " _bufferLength: " + _bufferLength + ", nalType: " + nalType);
                _nalUnit = new byte[4 + length];
                _nalUnit[0] = 0x00;
                _nalUnit[1] = 0x00;
                _nalUnit[2] = 0x00;
                _nalUnit[3] = 0x01;
                System.arraycopy(data, 0, _nalUnit, 4, length);
                _nalEndFlag = true;
                break;
        }

        if (_nalEndFlag) {
            return _nalUnit;
        } else {
            return null;
        }
    }*/

}
