package devgao.io.oasis;

import devgao.io.numberutil.Wad18;

import java.util.Map;

public class OasisOffer {
  public final Wad18 offerId;
  public final Map<String, Wad18> offerValues;
  public final Wad18 bestOfferDaiPerEth;
  public final Wad18 profit;

  public OasisOffer(
          Wad18 offerId, Map<String, Wad18> offerValues, Wad18 bestOfferDaiPerEth, Wad18 profit) {
    this.offerId = offerId;
    this.offerValues = offerValues;
    this.bestOfferDaiPerEth = bestOfferDaiPerEth;
    this.profit = profit;
  }
}
