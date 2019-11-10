package kpn.core.tiles

import kpn.api.custom.NetworkType

trait TileAnalyzer {

  def analysis(networkType: NetworkType): TileAnalysis

}
