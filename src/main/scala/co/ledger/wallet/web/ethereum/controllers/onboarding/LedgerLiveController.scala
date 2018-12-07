package co.ledger.wallet.web.ethereum.controllers.onboarding

import biz.enef.angulate.Module.RichModule
import biz.enef.angulate.{Controller, Scope}
import biz.enef.angulate.core.Location
import co.ledger.wallet.web.ethereum.services.WindowService
import scala.concurrent.ExecutionContext.Implicits.global
import scala.scalajs.js
import scala.util.Success

/**
  * Describe your class here.
  *
  * User: Pierre Pollastri
  * Date: 30-07-2018
  * Time: 12:10
  *
  */
class LedgerLiveController(val windowService: WindowService,
                           val $scope: Scope,
                           $route: js.Dynamic,
                           $location: Location,
                           $routeParams: js.Dictionary[String])
  extends Controller with OnBoardingController {


  def download(): Unit = js.Dynamic.global.open("http://ledger.com/live")

  def continue(): Unit = {

  }

  def openHelpCenter(): Unit = js.Dynamic.global.open("http://support.ledgerwallet.com/")

}


object LedgerLiveController {
  def init(module: RichModule) = module.controllerOf[LedgerLiveController]("LedgerLiveController")
}