package co.ledger.wallet.web.ethereum.services

import java.util.Date

import biz.enef.angulate.Module.RichModule
import biz.enef.angulate.Service
import co.ledger.wallet.web.ethereum.components.SnackBar.SnackBarInstance
import co.ledger.wallet.web.ethereum.controllers.WindowController

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.scalajs.js.timers

/**
  *
  * WindowService
  * ledger-wallet-ethereum-chrome
  *
  * Created by Pierre Pollastri on 02/05/2016.
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
/***
  * Configures the window
  */
class WindowService extends Service {

  def enableUserInterface(): Unit = _userInterfaceEnableListener.foreach(_(true))
  def disableUserInterface(): Unit = _userInterfaceEnableListener.foreach(_(false))

  def onUserInterfaceEnableChanged(handler: (Boolean) => Unit): Unit = {
    _userInterfaceEnableListener = Option(handler)
  }

  private var _userInterfaceEnableListener: Option[(Boolean) => Unit] = None

  // Navigation bar features
  def showNavigationBar(): Unit = {
    if (!_navigationIsVisible) {
      _navigationIsVisible = true
      _navigationBarVisibilityListener.foreach(_(_navigationIsVisible))
    }
  }

  def hideNavigationBar(): Unit = {
    if (_navigationIsVisible) {
      _navigationIsVisible = false
      _navigationBarVisibilityListener.foreach(_(_navigationIsVisible))
    }
  }

  def onNavigationBarVisibilityChanged(handler: (Boolean) => Unit) = {
    _navigationBarVisibilityListener = Option(handler)
  }

  def notifyRefresh(): Unit = {
    if (_refreshHandler.isDefined && !isRefreshing) {
      val start = new Date().getTime
      _refreshing = true
      _refreshHandler.get() onComplete {
        case all =>
          import timers._
          val now = new Date().getTime
          setTimeout((now - start) % 1000L + 1000L) {
            _refreshing = false
          }
      }
    }
  }

  def isRefreshing = _refreshing
  private var _refreshing = false

  def bind(windowController: WindowController): Unit = {
    _windowController = windowController
  }

  def onRefreshClicked(handler: () => Future[Unit]): Unit = {
    _refreshHandler = Option(handler)
  }

  private var _navigationBarVisibilityListener: Option[(Boolean) => Unit] = None
  private var _navigationIsVisible = false

  private var _refreshHandler: Option[() => Future[Unit]] = None

  private var _windowController: WindowController = null

  // SnackBar features
  var configureSnackBar: (Int, String, String) => SnackBarInstance = (_, _, _) => null

  case class StartRefresh()
  case class StopRefresh()
}

object WindowService {
  def init(module: RichModule) = module.serviceOf[WindowService]("windowService")
}