package kpn.server.api.analysis.pages

import kpn.api.common.common.TrackPath
import kpn.api.common.common.TrackPathKey
import kpn.api.common.planner.RouteLeg
import kpn.api.common.planner.RouteLegFragment
import kpn.api.common.planner.RouteLegNode
import kpn.api.common.planner.RouteLegRoute
import kpn.api.common.planner.RouteLegSegment
import kpn.api.common.route.RouteMap
import kpn.api.common.route.RouteNetworkNodeInfo
import kpn.api.custom.NetworkType
import kpn.core.db.couch.Couch
import kpn.core.util.Log
import kpn.server.repository.GraphRepository
import kpn.server.repository.RouteRepository
import org.springframework.stereotype.Component

@Component
class LegBuilderImpl(
  graphRepository: GraphRepository,
  routeRepository: RouteRepository,
  graphLoad: Boolean
) extends LegBuilder {

  private val log = Log(classOf[LegBuilderImpl])

  override def build(networkType: NetworkType, legId: String, sourceNodeId: String, sinkNodeId: String): Option[RouteLeg] = {
    graphRepository.graph(networkType) match {
      case Some(graph) =>
        graph.findPath(sourceNodeId.toLong, sinkNodeId.toLong) match {
          case Some(path) =>

            val legRoutes: Seq[RouteLegRoute] = path.segments.flatMap { segment =>
              val routeId = segment.pathKey.routeId
              routeRepository.routeWithId(routeId, Couch.uiTimeout) match {
                case None =>
                  log.error(s"route $routeId not found")
                  None
                case Some(route) =>
                  val routeMap = route.analysis.get.map // TODO make more safe
                  trackPathFromRouteMap(routeMap, segment.pathKey) match {
                    case None => None
                    case Some(trackPath) =>

                      val segments: Seq[RouteLegSegment] = trackPath.segments.map { trackPathSegment =>
                        val fragments: Seq[RouteLegFragment] = trackPathSegment.fragments.map { fragment =>
                          RouteLegFragment(
                            fragment.trackPoint.lat,
                            fragment.trackPoint.lon,
                            fragment.meters,
                            fragment.orientation,
                            fragment.streetIndex
                          )
                        }
                        val meters = fragments.map(_.meters).sum
                        RouteLegSegment(
                          meters,
                          trackPathSegment.surface,
                          fragments
                        )
                      }

                      val meters = segments.map(_.meters).sum

                      nodeFromRouteMap(routeMap, trackPath.endNodeId) match {
                        case Some(sinkRouteNetworkNodeInfo) =>
                          val sinkNodeId = sinkRouteNetworkNodeInfo.id.toString
                          val sinkodeName = sinkRouteNetworkNodeInfo.name
                          val sink = RouteLegNode(sinkNodeId, sinkodeName, sinkRouteNetworkNodeInfo.lat, sinkRouteNetworkNodeInfo.lon)

                          nodeFromRouteMap(routeMap, trackPath.startNodeId) match {
                            case Some(sourceRouteNetworkNodeInfo) =>
                              val sourceNodeId = sourceRouteNetworkNodeInfo.id.toString
                              val sourceNodeName = sourceRouteNetworkNodeInfo.name
                              val source = RouteLegNode(sourceNodeId, sourceNodeName, sourceRouteNetworkNodeInfo.lat, sourceRouteNetworkNodeInfo.lon)
                              val meters = segments.map(_.meters).sum
                              Some(RouteLegRoute(source, sink, meters, segments, routeMap.streets))

                            case None =>
                              log.error(s"route $routeId source node ${trackPath.startNodeId} not found")
                              None
                          }

                        case None =>
                          log.error(s"route $routeId sink node ${trackPath.endNodeId} not found")
                          None
                      }
                  }
              }
            }

            val routeLeg = RouteLeg(legId, legRoutes)
            Some(routeLeg)

          case None =>
            log.error(s"Could not find $networkType path between $sourceNodeId and $sinkNodeId")
            None
        }

      case _ =>
        log.error("Could not find graph for network type " + networkType)
        None
    }
  }

  private def trackPathFromRouteMap(routeMap: RouteMap, trackPathKey: TrackPathKey): Option[TrackPath] = {
    trackPathKey.pathType match {
      case "forward" => routeMap.forwardPath
      case "backward" => routeMap.backwardPath
      case "start" => Some(routeMap.startTentaclePaths(trackPathKey.pathIndex.toInt - 1))
      case "end" => Some(routeMap.endTentaclePaths(trackPathKey.pathIndex.toInt - 1))
      case _ => None
    }
  }

  private def nodeFromRouteMap(routeMap: RouteMap, nodeId: Long): Option[RouteNetworkNodeInfo] = {
    val allNodes = routeMap.startNodes ++
      routeMap.endNodes ++
      routeMap.startTentacleNodes ++
      routeMap.endTentacleNodes
    allNodes.find(_.id == nodeId)
  }

}
