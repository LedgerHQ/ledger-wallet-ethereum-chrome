package co.ledger.wallet.web.ethereum.core.device.usb

import co.ledger.wallet.core.device.Device.CommunicationException
import co.ledger.wallet.core.utils.{BytesReader, HexUtils}
import co.ledger.wallet.core.utils.logs.{Loggable, Logger}
import co.ledger.wallet.web.ethereum.core.device.{FidoU2fTransportHelper, LedgerTransportHelper}
import co.ledger.wallet.web.ethereum.core.device.usb.UsbDeviceImpl.UsbExchangePerformer

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Future, Promise}
import scala.scalajs.js
import scala.util.{Failure, Random, Success}

/**
  *
  * UsbHidDeviceImpl
  * ledger-wallet-ethereum-chrome
  *
  * Created by Pierre Pollastri on 27/05/2016.
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
class UsbHidExchangePerformer(connection: UsbDeviceImpl.Connection,
                              var debug: Boolean,
                              val transport: UsbHidExchangePerformer.Transport
                             ) extends UsbExchangePerformer with Loggable {
  import UsbHidExchangePerformer._
  override implicit val LogTag: String = "APDU"
  val HidBufferSize = 64
  val LedgerDefaultChannel = 2
  val Sw1DataAvailable = 0x61


  private var _initializeFuture: Option[Future[Unit]] = None
  private val fidoHelper = new FidoU2fTransportHelper(HidBufferSize)
  private val chrome = js.Dynamic.global.chrome

  override def close(): Unit = {
    chrome.hid.disconnect(connection.connectionId)
  }


  override def performExchange(command: Array[Byte]): Future[Array[Byte]] =
    init().flatMap(_ => performExchange(command, fidoHelper.MessageTag()))

  private def transportName = transport match {
    case LedgerTransport() => "LEDGER"
    case FidoU2FTransport() => "FIDO"
    case LegacyTransport() => "LEGACY"
  }

  def performExchange(cmd: Array[Byte], tag: fidoHelper.Tag): Future[Array[Byte]] = {
    Logger.v(s"($transportName) => ${HexUtils.bytesToHex(cmd)}")("APDU")
    var command = cmd
    transport match {
      case LedgerTransport() =>
        command = LedgerTransportHelper.wrapCommandAPDU(LedgerDefaultChannel, cmd, HidBufferSize)
      case FidoU2FTransport() =>
        command = fidoHelper.wrapCommandAPDU(tag, cmd)
      case LegacyTransport() =>
        // Ignore
    }
    Logger.v(s"wrapped ${HexUtils.bytesToHex(command)}")
    def sendBlocks(offset: Int = 0): Future[Unit] = {
      val blockSize = if (command.length - offset > HidBufferSize) HidBufferSize else command.length - offset
      System.arraycopy(command, offset, _transferBuffer, 0, blockSize)
      send(_transferBuffer) flatMap {(_) =>
        if (offset + blockSize < command.length)
          sendBlocks(offset + blockSize)
        else
          Future.successful()
      }
    }
    def receiveLegacyBlock(buffer: ArrayBuffer[Byte]): Future[Array[Byte]] = {
      null
    }
    def receiveLedgerBlock(buffer: ArrayBuffer[Byte]): Future[Array[Byte]] = {
      receive().flatMap {(response) =>
        buffer ++= response
        val responseData = LedgerTransportHelper.unwrapResponseAPDU(LedgerDefaultChannel, buffer.toArray, HidBufferSize)
        if (responseData == null) {
          receiveLedgerBlock(buffer)
        } else {
          Future.successful(responseData)
        }
      }
    }
    def receiveFidoU2fBlock(buffer: ArrayBuffer[Byte]): Future[Array[Byte]] = {
      receive().flatMap {(response) =>
        buffer ++= response
        val responseData = fidoHelper.unwrapResponseAPDU(tag, buffer.toArray)
        if (responseData == null) {
          receiveFidoU2fBlock(buffer)
        } else {
          Logger.v(s"unwrapping ${HexUtils.bytesToHex(buffer.toArray)}")
          Future.successful(responseData)
        }
      }
    }
    def receiveBlocks(buffer: ArrayBuffer[Byte] = ArrayBuffer.empty[Byte]): Future[Array[Byte]] = {
      transport match {
        case LedgerTransport() =>
          receiveLedgerBlock(buffer)
        case FidoU2FTransport() =>
          receiveFidoU2fBlock(buffer)
        case LegacyTransport() =>
          receiveLegacyBlock(buffer)
      }
    }
    sendBlocks().flatMap((_) => receiveBlocks()) andThen {
      case Success(result) =>
        Logger.v(s"($transportName) <= ${HexUtils.bytesToHex(result)}")("APDU")
      case Failure(ex) =>
        ex.printStackTrace()
    }
  }

  private def init(): Future[Unit] = {
    _initializeFuture.getOrElse {
      _initializeFuture = Some(transport match {
        case FidoU2FTransport() =>
          val nonce = Array.fill[Byte](8)((Random.nextInt(256) - 128).toByte)
          performExchange(nonce, fidoHelper.InitTag()) map {(answer) =>
            val reader = new BytesReader(answer)
            if (!(reader.readNextBytes(8) sameElements nonce)) {
              throw new Exception("Invalid channel initialization")
            } else {
              fidoHelper.channel = reader.readNextInt()
            }
            ()
          }
        case other =>
          Future.successful[Unit]()
      })
      _initializeFuture.get
    }
  }

  private def send(bytes: Array[Byte]): Future[Unit] = {
    import scala.scalajs.js.typedarray._
    val promise = Promise[Unit]()
    chrome.hid.send(connection.connectionId, 0, byteArray2Int8Array(bytes).buffer, { () =>
      if (js.isUndefined(chrome.runtime.lastError))
        promise.success()
      else
        promise.failure(CommunicationException(chrome.runtime.lastError.message.toString))
    })
    promise.future
  }

  private def receive(): Future[Array[Byte]] = {
    import scala.scalajs.js.typedarray._
    val promise = Promise[Array[Byte]]()
    chrome.hid.receive(connection.connectionId, {(reportId: Int, data: TypedArray[_, _]) =>
      if (js.isUndefined(chrome.runtime.lastError))
        promise.success(int8Array2ByteArray(new Int8Array(data)))
      else
        promise.failure(CommunicationException(chrome.runtime.lastError.message.toString))
    })
    promise.future
  }

  private val _transferBuffer = new Array[Byte](HidBufferSize)
}

object UsbHidExchangePerformer {

  sealed trait Transport
  case class LegacyTransport() extends Transport
  case class LedgerTransport() extends Transport
  case class FidoU2FTransport() extends Transport

}