package devgao.io.gasprovider;

import devgao.io.compounddai.CompoundDaiContract;
import devgao.io.oasisdex.OasisDexContract;
import devgao.io.uniswap.UniswapContract;
import devgao.io.util.BigNumberUtil;
import devgao.io.weth.WethContract;
import org.jetbrains.annotations.NotNull;
import org.slf4j.LoggerFactory;
import org.web3j.protocol.Web3j;
import org.web3j.tx.gas.ContractGasProvider;
import org.web3j.utils.Convert;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import static devgao.io.util.BigNumberUtil.divide;
import static devgao.io.util.BigNumberUtil.multiply;

public class GasProvider implements ContractGasProvider {
  static final String GWEI = " GWEI";
  private static final org.slf4j.Logger logger =
      LoggerFactory.getLogger(MethodHandles.lookup().lookupClass().getSimpleName());
  final BigInteger minimumGasPrice;
  final BigInteger maximumGasPrice;
  final Web3j web3j;
  BigInteger gasPrice;
  private List<Long> failedTransactionsWithinTheLastTwelveHoursForGasPriceArrayList =
      new ArrayList<>();

  public GasProvider(Web3j web3j, BigInteger minimumGasPrice, BigInteger maximumGasPrice) {
    this.web3j = web3j;
    this.minimumGasPrice = minimumGasPrice;
    this.maximumGasPrice = maximumGasPrice;
    this.gasPrice = BigInteger.valueOf(1_000000000);
  }

  public void updateFailedTransactions(List<Long> list) {
    failedTransactionsWithinTheLastTwelveHoursForGasPriceArrayList.addAll(list);
    setFailedTransactionsWithinTheLastTwelveHoursForGasPriceArrayList(
        ArrayListUtil.removeDuplicates(
            failedTransactionsWithinTheLastTwelveHoursForGasPriceArrayList));
    if (System.currentTimeMillis()
        >= failedTransactionsWithinTheLastTwelveHoursForGasPriceArrayList.get(0)
            + 12 * 60 * 60 * 1000) { // 12 hours
      logger.trace("FAILED TRANSACTION REMOVED 12 HOURS");
      failedTransactionsWithinTheLastTwelveHoursForGasPriceArrayList.remove(0);
    }
  }

  @Override
  public BigInteger getGasLimit(@NotNull String contractFunc) {
    switch (contractFunc) {
      case CompoundDaiContract.FUNC_MINT:
      case CompoundDaiContract.FUNC_REDEEM:
        return BigInteger.valueOf(200_000);
      case UniswapContract.FUNC_TOKENTOETHSWAPINPUT:
      case UniswapContract.FUNC_ETHTOTOKENSWAPINPUT:
        return BigInteger.valueOf(65_000);
      case OasisDexContract.FUNC_BUY:
        return BigInteger.valueOf(300_000);
      case WethContract.FUNC_WITHDRAW:
      case WethContract.FUNC_DEPOSIT:
        return BigInteger.valueOf(100_000);
      default:
        logger.trace("{} FUNCTION IS NOT IMPLEMENTED", contractFunc);
        return BigInteger.valueOf(5_300_000);
    }
  }

  @Override
  public BigInteger getGasPrice(String contractFunc) {
    return gasPrice;
  }

  public BigInteger updateFastGasPrice(BigDecimal medianEthereumPrice, BigDecimal potentialProfit) {
    BigInteger fastGasPrice = minimumGasPrice;
    try {
      BigInteger etherchainResult = Etherchain.getFastestGasPrice();
      fastGasPrice = fastGasPrice.max(etherchainResult);
    } catch (GasPriceException e) {
      logger.error("GasPriceException", e);
    }
    try {
      BigInteger ethGasStationResult = ETHGasStation.getAverageGasPrice();
      fastGasPrice = fastGasPrice.max(ethGasStationResult);
    } catch (GasPriceException e) {
      logger.error("GasPriceException", e);
    }
    try {
      double percentageOfProfitAsFee =
          getPercentageOfProfitAsFee(
              failedTransactionsWithinTheLastTwelveHoursForGasPriceArrayList.size());
      BigInteger gasPriceBasedOnProfit =
          calculateGasPriceAsAPercentageOfProfit(
              medianEthereumPrice, potentialProfit, 300000.0, percentageOfProfitAsFee);
      fastGasPrice = fastGasPrice.max(gasPriceBasedOnProfit);
    } catch (GasPriceException e) {
      logger.error("GasPriceException", e);
    }
    gasPrice = fastGasPrice.min(maximumGasPrice);
    logger.trace("GAS PRICE {}{}", Convert.fromWei(gasPrice.toString(), Convert.Unit.GWEI), GWEI);
    return gasPrice;
  }

  public BigInteger updateSlowGasPrice() {
    BigInteger slowGasPrice = minimumGasPrice;
    try {
      slowGasPrice = slowGasPrice.max(web3j.ethGasPrice().send().getGasPrice());
    } catch (IOException e) {
      logger.error("IOException", e);
    }
    gasPrice = slowGasPrice;
    logger.trace("GAS PRICE {}{}", Convert.fromWei(gasPrice.toString(), Convert.Unit.GWEI), GWEI);
    return gasPrice;
  }

  public double getPercentageOfProfitAsFee(
      int failedTransactionsWithinTheLastTwelveHoursForGasPriceArrayListSize) {
    double percentageOfProfitAsFee =
        Math.min(
            0.35,
            ((double) failedTransactionsWithinTheLastTwelveHoursForGasPriceArrayListSize * 5.0
                    + 10.0)
                / 100.0);
    logger.trace("GP PERCENTAGE OF PROFIT {}", percentageOfProfitAsFee);

    return percentageOfProfitAsFee;
  }

  BigInteger calculateGasPriceAsAPercentageOfProfit(
      @NotNull BigDecimal medianEthereumPrice,
      BigDecimal potentialProfit,
      double gasLimit,
      double percentageOfProfitAsFee)
      throws GasPriceException {
    if (medianEthereumPrice.compareTo(BigDecimal.ZERO) == 0
        || potentialProfit.compareTo(BigDecimal.ZERO) == 0
        || gasLimit == 0.0)
      throw new GasPriceException("calculateGasPriceAsAPercentageOfProfit Exception");

    BigDecimal feeInEth =
        divide(
            potentialProfit.multiply(BigDecimal.valueOf(percentageOfProfitAsFee)),
            medianEthereumPrice); // 0.049307620043223397 0.04930762004
    logger.trace(
        "EST. TRANSACTION FEE {}{}",
        BigNumberUtil.makeBigNumberHumanReadable(multiply(feeInEth, medianEthereumPrice)),
        " DAI");

    BigInteger gasPriceBasedOnProfit =
        divide(feeInEth, BigNumberUtil.makeDoubleMachineReadable(gasLimit)).toBigInteger();
    logger.trace(
        "PROFIT SUGGESTS GP {}{}",
        Convert.fromWei(gasPriceBasedOnProfit.toString(), Convert.Unit.GWEI),
        GWEI);

    return gasPriceBasedOnProfit;
  }

  /** @deprecated in the interface       */
  @Override
  @Deprecated
  public BigInteger getGasPrice() {
    return null;
  }

  /** @deprecated in the interface       */
  @Override
  @Deprecated
  public BigInteger getGasLimit() {
    return null;
  }

  public int getFailedTransactionsWithinTheLastTwelveHoursForGasPriceArrayList() {
    return failedTransactionsWithinTheLastTwelveHoursForGasPriceArrayList.size();
  }

  public void setFailedTransactionsWithinTheLastTwelveHoursForGasPriceArrayList(
      List<Long> failedTransactionsWithinTheLastTwelveHoursForGasPriceArrayList) {
    this.failedTransactionsWithinTheLastTwelveHoursForGasPriceArrayList =
        failedTransactionsWithinTheLastTwelveHoursForGasPriceArrayList;
  }
}
