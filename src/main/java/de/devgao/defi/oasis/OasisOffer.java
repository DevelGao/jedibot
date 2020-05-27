package devgao.io.oasis;

import devgao.io.numberutil.Wad18;

import java.math.BigInteger;
import java.util.Map;

public class OasisOffer {
  public final BigInteger offerId;
  public final Map<String, Wad18> offerValues;
  public final Wad18 bestOfferDaiPerEth;
  public final Wad18 profit;

  public OasisOffer(
      BigInteger offerId, Map<String, Wad18> offerValues, Wad18 bestOfferDaiPerEth, Wad18 profit) {
    this.offerId = offerId;
    this.offerValues = offerValues;
    this.bestOfferDaiPerEth = bestOfferDaiPerEth;
    this.profit = profit;
  }
}
