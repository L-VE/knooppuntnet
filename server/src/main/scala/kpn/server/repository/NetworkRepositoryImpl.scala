package kpn.server.repository

import akka.util.Timeout
import kpn.core.db._
import kpn.core.db.couch.Couch
import kpn.core.db.couch.Database
import kpn.core.db.json.JsonFormats.gpxDocFormat
import kpn.core.db.json.JsonFormats.networkDocFormat
import kpn.core.db.views.AnalyzerDesign
import kpn.core.db.views.NetworkMapView
import kpn.core.db.views.NetworkView
import kpn.core.gpx.GpxFile
import kpn.core.util.Log
import kpn.shared.Subset
import kpn.shared.network.NetworkAttributes
import kpn.shared.network.NetworkInfo
import kpn.shared.network.NetworkMapInfo
import org.springframework.stereotype.Component

@Component
class NetworkRepositoryImpl(analysisDatabase: Database) extends NetworkRepository {

  private val log = Log(classOf[NetworkRepository])

  override def network(networkId: Long, timeout: Timeout): Option[NetworkInfo] = {
    analysisDatabase.optionGet(networkKey(networkId), timeout).map(networkDocFormat.read).map(_.network)
  }

  override def save(network: NetworkInfo): Boolean = {

    val key = networkKey(network.id)

    analysisDatabase.optionGet(key, Couch.batchTimeout) match {
      case Some(jsDoc) =>
        val doc = networkDocFormat.read(jsDoc)
        if (network == doc.network) {
          log.info(s"""Network "${network.id}" not saved (no change)""")
          false
        }
        else {
          log.infoElapsed(s"""Network "${network.id}" update""") {
            analysisDatabase.save(key, networkDocFormat.write(NetworkDoc(key, network, doc._rev)))
            true
          }
        }

      case None =>
        log.infoElapsed(s"""Network "${network.id}" saved""") {
          analysisDatabase.save(key, networkDocFormat.write(NetworkDoc(key, network)))
          true
        }
    }
  }

  override def delete(networkId: Long): Unit = {
    analysisDatabase.delete(networkKey(networkId))
  }

  private def networkKey(networkId: Long): String = s"${KeyPrefix.Network}:$networkId"

  override def gpx(networkId: Long, timeout: Timeout): Option[GpxFile] = {
    analysisDatabase.optionGet(gpxKey(networkId), timeout).map(gpxDocFormat.read).map(_.file)
  }

  override def saveGpxFile(gpxFile: GpxFile): Boolean = {
    val key = gpxKey(gpxFile.networkId)

    def doSave(): Unit = {
      log.info(s"""Save gpx file "${gpxFile.networkId}"""")
      analysisDatabase.save(key, gpxDocFormat.write(GpxDoc(key, gpxFile)))
    }

    analysisDatabase.optionGet(key, Couch.batchTimeout) match {
      case Some(jsDoc) =>
        val doc = gpxDocFormat.read(jsDoc)
        if (gpxFile == doc.file) {
          log.info(s"""Network "${gpxFile.networkId}" gpx not saved (no change)""")
          false
        }
        else {
          log.infoElapsed(s"""Network "${gpxFile.networkId}" gpx update""") {
            analysisDatabase.delete(key)
            doSave()
            true
          }
        }

      case None =>
        log.infoElapsed(s"""Network "${gpxFile.networkId}" gpx saved""") {
          doSave()
          true
        }
    }
  }

  private def gpxKey(networkId: Long): String = s"${KeyPrefix.NetworkGpx}:$networkId"

  override def networks(subset: Subset, timeout: Timeout, stale: Boolean): Seq[NetworkAttributes] = {
    analysisDatabase.query(AnalyzerDesign, NetworkView, timeout, stale)(subset.country.domain, subset.networkType.name).map(NetworkView.convert)
  }

  override def networksMap(country: String, networkType: String, timeout: Timeout, stale: Boolean): Seq[NetworkMapInfo] = {
    analysisDatabase.query(AnalyzerDesign, NetworkMapView, timeout, stale)(country, networkType).map(NetworkMapView.convert)
  }
}
