package co.ledger.wallet.web.ethereum.controllers.onboarding

import biz.enef.angulate.Module.RichModule
import biz.enef.angulate.{Controller, Scope}
import biz.enef.angulate.core.Location
import co.ledger.wallet.web.ethereum.core.utils.ChromeGlobalPreferences
import co.ledger.wallet.web.ethereum.services.{DeviceService, SessionService, WindowService}
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
                           deviceService: DeviceService,
                           sessionService: SessionService,
                           val $scope: Scope,
                           $route: js.Dynamic,
                           $location: Location,
                           $routeParams: js.Dictionary[String])
  extends Controller with OnBoardingController {

  val CounterName = "counter_1"
  val CounterGoal = 2

  private val _chain = $routeParams.get("chain")

  def download(): Unit = js.Dynamic.global.open("http://ledger.com/live")

  def continue(): Unit = {
    _chain match {
      case Some(chain) =>
        $location.url(s"/onboarding/alert/$chain")
        //$location.url(s"/onboarding/opening/$chain/")
        $route.reload()
      case others =>
        $location.url("/onboarding/alert")
        //$location.url("/onboarding/chain/select")
        $route.reload()
    }
  }

  def openHelpCenter(): Unit = js.Dynamic.global.open("http://support.ledgerwallet.com/")

  private val prefs = new ChromeGlobalPreferences("ledger_live")

  prefs.edit().putInt(CounterName, 1 + prefs.int(CounterName).getOrElse(0)).commit()

}


object LedgerLiveController {
  def init(module: RichModule) = module.controllerOf[LedgerLiveController]("LedgerLiveController")
}