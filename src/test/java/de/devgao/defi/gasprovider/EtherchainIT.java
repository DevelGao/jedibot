package devgao.io.gasprovider;

import org.junit.jupiter.api.Test;

import java.math.BigInteger;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class EtherchainIT {
  private static final String TOO_HIGH = "Error, value is too high";
  private static final String TOO_LOW = "Error, value is too low";
  private static final BigInteger MINIMUM_GAS_PRICE = BigInteger.valueOf(1_000000000);
  private static final BigInteger MAXIMUM_GAS_PRICE = BigInteger.valueOf(100_000000000L);

  @Test
  public void getFastestGasPrice_currentGasPrice_GasPriceWithinBoundaries()
      throws GasPriceException {
    BigInteger result = Etherchain.getFastestGasPrice();
    assertTrue(MINIMUM_GAS_PRICE.compareTo(result) < 0, TOO_LOW);
    assertTrue(MAXIMUM_GAS_PRICE.compareTo(result) > 0, TOO_HIGH);
  }

  @Test
  public void getFastestGasPrice_currentGasPriceDifference_GasPriceDifferenceIsReasonable()
      throws GasPriceException {
    BigInteger etherchainResult = Etherchain.getFastestGasPrice();
    BigInteger ethGasStationResult = ETHGasStation.getFastestGasPrice();
    BigInteger difference = ethGasStationResult.subtract(etherchainResult);
    assertThat(difference, is(lessThan(BigInteger.valueOf(10_000000000L))));
  }
}
