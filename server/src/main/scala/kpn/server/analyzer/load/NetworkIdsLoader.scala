package kpn.server.analyzer.load

import kpn.api.common.ScopedNetworkType
import kpn.api.custom.Timestamp

trait NetworkIdsLoader {
  def load(timestamp: Timestamp, scopedNetworkType: ScopedNetworkType): Seq[Long]
}
