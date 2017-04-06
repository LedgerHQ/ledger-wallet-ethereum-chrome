package co.ledger.wallet.web.ethereum.core.device

import co.ledger.wallet.core.utils.logs.Logger
import co.ledger.wallet.core.utils.{BytesReader, BytesWriter}

/**
  *
  * FidoU2fTransportHelper
  * ledger-wallet-ethereum-chrome
  *
  * Created by Pierre Pollastri on 05/04/2017.
  *
  * The MIT License (MIT)
  *
  * Copyright (c) 2016 Ledger
  *
  * Permission is hereby granted, free of charge, to any person obtaining a copy
  * of this software and associated documentation files (the "Software"), to deal
  * in the Software without restriction, including without limitation the rights
  * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
  * copies of the Software, and to permit persons to whom the Software is
  * furnished to do so, subject to the following conditions:
  *
  * The above copyright notice and this permission notice shall be included in all
  * copies or substantial portions of the Software.
  *
  * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
  * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
  * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
  * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
  * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
  * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
  * SOFTWARE.
  *
  */
class FidoU2fTransportHelper(val packetSize: Int) {

  /*

    Initialization packet
    =====================

    | Offset | Length  | Mnemonic |                    Description                     |
    |--------|---------|----------|----------------------------------------------------|
    |      0 | 4       | CID      | Channel identifier                                 |
    |      4 | 1       | CMD      | Command identifier (bit 7 always set)              |
    |      5 | 1       | BCNTH    | High part of payload length                        |
    |      6 | 1       | BCNTL    | Low part of payload length                         |
    |      7 | (s - 7) | DATA     | Payload data (s is equal to the fixed packet size) |


    Continuation packet
    ===================

    | Offset | Length  | Mnemonic |                    Description                     |
    |--------|---------|----------|----------------------------------------------------|
    |      0 | 4       | CID      | Channel identifier                                 |
    |      4 | 1       | SEQ      | Packet sequence 0x00..0x7f (bit 7 always cleared)  |
    |      5 | (s - 5) | DATA     | Payload data (s is equal to the fixed packet size) |

   */

  val BroadcastChannel = 0xffffffff

  sealed class Tag(val value: Byte)
  case class InitTag() extends Tag(0x86.toByte)
  case class MessageTag() extends Tag(0x83.toByte)

  private var _channel = BroadcastChannel
  def channel_= (channel: Int): Unit = _channel = channel
  def channel = _channel

  def wrapCommandAPDU(tag: Tag, command: Array[Byte]): Array[Byte] = {
    val writer = new BytesWriter()
    val apdu = new BytesReader(command)

    // Writes the initialization packet

    writer
      .writeInt(_channel)
      .writeByte(tag.value)
      .writeShort(command.length)
      .writeByteArray(apdu.safeReadNextBytes(packetSize - 7))

    var sequence = 0.toByte
    while (apdu.available != 0) {
      // Writes the continuation packet
      val data = apdu.safeReadNextBytes(packetSize - 5)
      writer
        .writeInt(_channel)
        .writeByte(sequence)
        .writeByteArray(data)
      sequence = (sequence + 1).toByte
    }
    Logger.d(s"Got ${packetSize} ${writer.size} ${writer.size % packetSize}")
    writer
      .writeByteArray(Array.fill[Byte](packetSize - (writer.size % packetSize))(0))
      .toByteArray
  }

  def unwrapResponseAPDU(tag: Tag, data: Array[Byte]): Array[Byte] = {
    val writer = new BytesWriter()
    if (data == null || data.length < 7) {
      null
    } else {
      val reader = new BytesReader(data)
      var readChannel = reader.readNextInt()
      if (readChannel != _channel) {
        if (_channel == BroadcastChannel) {
          _channel = readChannel
        } else {
          throw new Exception("Invalid channel")
        }
      }
      if (reader.readNextByte() != tag.value)
        throw new Exception("Invalid command")
      val responseLength = reader.readNextShort()
      if (reader.available < responseLength) {
        null
      } else {
        var blockSize = if (responseLength < packetSize - 7) responseLength else (packetSize - 7)
        writer.writeByteArray(reader.readNextBytes(blockSize))
        var sequence = 0.toByte
        while (writer.size < responseLength) {
          readChannel = reader.readNextInt()
          if (_channel != readChannel)
            throw new Exception("Invalid channel")
          if (reader.readNextByte() != sequence)
            throw new Exception("Invalid sequence")
          sequence = (sequence + 1).toByte
          blockSize = Math.min(responseLength - writer.size, packetSize - 5)
          if (reader.available < blockSize)
            return null
          writer.writeByteArray(reader.readNextBytes(blockSize))
        }
        writer.toByteArray
      }
    }
  }

}