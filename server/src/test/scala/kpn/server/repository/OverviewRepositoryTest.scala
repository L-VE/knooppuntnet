package kpn.server.repository

import kpn.api.custom.Country
import kpn.api.custom.NetworkType
import kpn.api.custom.Subset
import kpn.core.app.stats.Figure
import kpn.core.db.couch.Couch
import kpn.core.test.TestSupport.withDatabase
import kpn.api.common.SharedTestObjects
import org.scalatest.FunSuite
import org.scalatest.Matchers

class OverviewRepositoryTest extends FunSuite with Matchers with SharedTestObjects {

  test("figures") {

    withDatabase { database =>

      val networkRepository: NetworkRepository = new NetworkRepositoryImpl(database)
      networkRepository.save(
        newNetwork(
          1,
          Some(Country.be),
          NetworkType.hiking,
          nodes = Seq(
            newNetworkNodeInfo2(101, "01"),
            newNetworkNodeInfo2(102, "02")
          )
        )
      )

      val repository: OverviewRepository = new OverviewRepositoryImpl(database)

      val figures = repository.figures(Couch.uiTimeout, stale = false)
      figures("NetworkCount") should equal(Figure("NetworkCount", 1, Map(Subset.beHiking -> 1)))
      figures("NodeCount") should equal(Figure("NodeCount", 2, Map(Subset.beHiking -> 2)))
    }
  }
}
