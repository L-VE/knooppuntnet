package kpn.core.database.views.analyzer

import kpn.core.database.views.analyzer.FactView.FactViewKey
import kpn.core.db.TestDocBuilder
import kpn.core.test.TestSupport.withDatabase
import kpn.shared.Country
import kpn.shared.Fact
import kpn.shared.NetworkScope
import kpn.shared.NetworkType
import kpn.shared.ScopedNetworkType
import kpn.shared.Subset
import kpn.shared.data.Tags
import org.scalatest.FunSuite
import org.scalatest.Matchers

class FactViewTest extends FunSuite with Matchers {

  test("rows") {

    withDatabase { database =>

      val networkId = 5L

      new TestDocBuilder(database) {
        val detail = Some(
          networkInfoDetail(
            nodes = Seq(
              newNetworkNodeInfo2(
                1001L,
                "01",
                facts = Seq(
                  Fact.NodeMemberMissing
                )
              )
            ),
            routes = Seq(
              networkRouteInfo(
                10L,
                facts = Seq(
                  Fact.RouteBroken,
                  Fact.RouteNameMissing
                )
              )
            )
          )
        )
        network(
          networkId,
          Subset.nlHiking,
          "network-name",
          facts = Seq(
            Fact.NameMissing,
            Fact.NetworkExtraMemberNode
          ),
          detail = detail
        )
      }

      FactView.query(database, stale = false) should equal(
        Seq(
          FactViewKey("nl", "hiking", "NameMissing", "network-name", networkId),
          FactViewKey("nl", "hiking", "NetworkExtraMemberNode", "network-name", networkId),
          FactViewKey("nl", "hiking", "NodeMemberMissing", "network-name", networkId),
          FactViewKey("nl", "hiking", "RouteBroken", "network-name", networkId),
          FactViewKey("nl", "hiking", "RouteNameMissing", "network-name", networkId)
        )
      )
    }
  }

  test("orphan route") {

    withDatabase { database =>

      new TestDocBuilder(database) {
        route(
          11,
          Subset.nlHiking,
          orphan = true,
          facts = Seq(Fact.RouteBroken)
        )
      }

      FactView.query(database, stale = false) should equal(
        Seq(
          FactViewKey("nl", "hiking", "RouteBroken", "OrphanRoutes", 0)
        )
      )
    }
  }

  test("orphan node rcn") {
    orphanNodeTest(NetworkType.bicycle)
    orphanNodeTest(NetworkType.hiking)
    orphanNodeTest(NetworkType.horseRiding)
    orphanNodeTest(NetworkType.motorboat)
    orphanNodeTest(NetworkType.canoe)
    orphanNodeTest(NetworkType.inlineSkates)
  }

  private def orphanNodeTest(networkType: NetworkType): Unit = {

    withDatabase { database =>

      new TestDocBuilder(database) {
        val scopedNetworkType = ScopedNetworkType(NetworkScope.regional, networkType)
        node(
          11,
          Country.nl,
          tags = Tags.from(scopedNetworkType.nodeTagKey -> "01"),
          orphan = true,
          facts = Seq(Fact.IntegrityCheck)
        )
      }

      FactView.query(database, stale = false) should equal(
        Seq(
          FactViewKey("nl", networkType.name, "IntegrityCheck", "OrphanNodes", 0)
        )
      )
    }
  }
}
