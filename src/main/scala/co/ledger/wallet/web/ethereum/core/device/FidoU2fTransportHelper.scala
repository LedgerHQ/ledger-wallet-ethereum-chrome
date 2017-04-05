package co.ledger.wallet.web.ethereum.core.device

import co.ledger.wallet.core.utils.BytesWriter

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
object FidoU2fTransportHelper {

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

  def wrapCommandAPDU(tag: Byte, command: Array[Byte], packetSize: Int): Array[Byte] = {
    val writer = new BytesWriter()



    writer.toByteArray
  }

}
