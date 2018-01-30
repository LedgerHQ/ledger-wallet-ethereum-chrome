package co.ledger.wallet.web.ethereum.controllers.onboarding

import biz.enef.angulate.Module.RichModule
import biz.enef.angulate.core.{JQLite, Location}
import biz.enef.angulate.{Controller, Scope}
import org.scalajs.jquery.jQuery
import co.ledger.wallet.web.ethereum.services.{WindowService}
import co.ledger.wallet.web.ethereum.core.utils.{JQueryHelper}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.scalajs.js
import scala.scalajs.js.timers._

/**
  *
  * OpeningController
  * ledger-wallet-ethereum-chrome
  *
  * Created by Pierre Pollastri on 12/05/2016.
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
class AlertController(override val windowService: WindowService,
                        $location: Location,
                        $route: js.Dynamic,
                        $scope: Scope,
                        $element: JQLite,
                        $routeParams: js.Dictionary[String])
  extends Controller with OnBoardingController {

  private def animate() = {
    // Initialize default state
    JQueryHelper.injectCustomEasings()
    val header = jQuery($element.find("> header").asInstanceOf[JQLite](0))
    header.height(98)
    val section = jQuery($element.find("> section").asInstanceOf[JQLite](0))
    val document = jQuery(".onboarding-container")
    val plugFooter = jQuery($element.find("#plugFooter"))
    plugFooter.fadeOut(0)
    section.css("opacity", 0)
    val offset = header.offset().asInstanceOf[js.Dictionary[Double]]
    val translate = (document.outerHeight(true) / 2 - header.outerHeight(true) / 2) - document.css("padding-top").replace("px", "").toInt
    header.css("top", translate + "px")
    val duration = 750
    val easing = "default"
      // Perform animation
    setTimeout(1500){
      header.animate(js.Dictionary("top" -> 0), duration, easing)
      section.animate(js.Dictionary("opacity" -> 1), duration, easing)
      plugFooter.fadeIn(duration * 0.60)
    }



  }


  def openHelpCenter(): Unit = {
    js.Dynamic.global.open("https://support.ledgerwallet.com/hc/en-us/articles/115005200009")
  }

  def openMew(): Unit = {
    js.Dynamic.global.open("https://www.myetherwallet.com/")
  }

  def accept(): Unit = {
    js.Dynamic.global.mew_alert = true
    $location.url(s"/onboarding/launch/")
    $route.reload()
  }

  animate()
}

object AlertController {
  def init(module: RichModule) = module.controllerOf[AlertController]("AlertController")
}