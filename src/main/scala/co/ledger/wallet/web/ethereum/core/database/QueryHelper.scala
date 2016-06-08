package co.ledger.wallet.web.ethereum.core.database

import org.scalajs.dom.{ErrorEvent, Event, idb}

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.{Future, Promise}
import scala.reflect.ClassTag
import scala.concurrent.ExecutionContext.Implicits.global
import scala.scalajs.js

/**
  *
  * QueryHelper
  * ledger-wallet-ethereum-chrome
  *
  * Created by Pierre Pollastri on 07/06/2016.
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
trait QueryHelper[M <: Model] {
  private val modelDeclaration = newInstance()
  def database: DatabaseDeclaration
  def creator: ModelCreator[M]
  def newInstance(): M

  def readonly(): ReadOnlyQueryBuilder = new ReadOnlyQueryBuilder
  def readwrite(): ReadWriteQueryBuilder = new ReadWriteQueryBuilder

  trait QueryBuilder {
    protected def mode: String
    def commit(): Future[QueryResult] = {
      _steps.commit(mode)
    }

    protected def :+(perform: PerformStep) = _steps = new QueryStep(_steps, perform)
    private var _steps: QueryStep = new QueryStep(null, (_, _) => Future.successful())
  }

  class ReadOnlyQueryBuilder extends QueryBuilder {
    override protected def mode: String = "readonly"
  }

  class ReadWriteQueryBuilder extends ReadOnlyQueryBuilder {
    override protected def mode: String = "readwrite"
    def add(item: M): this.type = {
      this :+ {(transaction, result) =>
        println("ADDING")
        val promise = Promise[Unit]()
        try {
          val request = transaction.objectStore(modelDeclaration.entityName).add(item.toDictionary)
          request.onerror = { (event: ErrorEvent) =>
            println("ADDING FAILED")
            promise.failure(new Exception(event.message))
          }
          request.onsuccess = { (event: Event) =>
            println("ADDING SUCCEED")
            promise.success()
          }
        } catch {
          case er: Throwable => promise.failure(er)
        }
        promise.future
      }
      this
    }

    def add(items: Array[M]): this.type  = {
      for (item <- items)
        add(item)
      this
    }
  }

  private class QueryStep(val parent: QueryStep, val perform: PerformStep) {
    var child: QueryStep = null

    if (parent != null) {
      parent.child = this
    }

    def root: QueryStep = {
      var n = this
      while (n.parent != null)
        n = n.parent
      n
    }

    def commit(mode: String): Future[QueryResult] = {
      val r = root
      val result = new MutableQueryResult
      database.obtainConnection() flatMap {(connection) =>
        val transaction = connection.transaction(js.Array(modelDeclaration.entityName), mode)
        def iterate(step: QueryStep): Future[Unit] = {
          if (step == null)
            Future.successful()
          else
            step.perform(transaction, result) flatMap {(_) =>
              iterate(step.child)
            }
        }
        iterate(r)
      } map((_) => result.asInstanceOf[QueryResult])
    }
  }

  trait QueryResult {
    def cursor: Cursor[M]
    def items: Array[M]
  }

  private class MutableQueryResult extends QueryResult {
    override def cursor: Cursor[M] = ???

    override def items: Array[M] = ???
  }

  private type PerformStep = (idb.Transaction, MutableQueryResult) => Future[Unit]
}