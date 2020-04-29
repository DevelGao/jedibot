package devgao.io.uniswap;

import java.math.BigDecimal;
import java.math.BigInteger;

public class UniswapOffer {
  public final BigInteger buyableAmount;
  public final BigDecimal profit;

  public UniswapOffer(BigInteger buyableAmount, BigDecimal profit) {
    this.buyableAmount = buyableAmount;
    this.profit = profit;
  }
}
