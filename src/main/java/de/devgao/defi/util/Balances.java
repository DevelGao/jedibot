package devgao.io.util;

import devgao.io.compounddai.CompoundDai;
import devgao.io.dai.Dai;
import devgao.io.medianizer.MedianException;
import devgao.io.medianizer.Medianizer;
import devgao.io.weth.Weth;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.math.BigInteger;

import static devgao.io.util.BigNumberUtil.*;

public class Balances {
  // gas stuff, https://etherconverter.online/
  public static final BigDecimal MINIMUM_ETHEREUM_RESERVE_UPPER_LIMIT =
      makeDoubleMachineReadable(0.10);
  public static final BigDecimal MINIMUM_ETHEREUM_RESERVE_LOWER_LIMIT =
      makeDoubleMachineReadable(0.025);
  private static final BigDecimal MINIMUM_DAI_NECESSARY_FOR_SALE = makeDoubleMachineReadable(250.0);
  private static final BigDecimal MINIMUM_ETHEREUM_NECESSARY_FOR_SALE =
      makeDoubleMachineReadable(1.0);
  private static final org.slf4j.Logger logger =
      LoggerFactory.getLogger(MethodHandles.lookup().lookupClass().getSimpleName());
  // 1.043115231071306958
  public final BigDecimal minimumTradeProfit;
  private final Dai dai;
  private final Weth weth;
  private final CompoundDai compoundDai;
  private final Ethereum ethereum;
  BigDecimal usd;
  // environment variables
  private BigDecimal ethBalance;
  private BigDecimal daiBalance;
  private BigDecimal wethBalance;
  private BigInteger cdaiBalance;
  private BigDecimal daiInCompound;
  private BigDecimal sumEstimatedProfits;
  private BigDecimal sumEstimatedMissedProfits;
  private long pastTimeRatios;
  private BigDecimal minimumTradeProfitBuyDai;
  private BigDecimal minimumTradeProfitSellDai;
  private long currentTotalEthOwnershipRatio;
  private long currentTotalDaiOwnershipRatio;
  private BigDecimal initialTotalUSD;
  private int initialTotalUSDCounter;
  private long pastTimeBalances;
  private long lastSuccessfulTransaction;

  public Balances(Dai dai, Weth weth, CompoundDai compoundDai, Ethereum ethereum) {
    ethBalance = BigDecimal.ZERO;
    daiBalance = BigDecimal.ZERO;
    wethBalance = BigDecimal.ZERO;
    usd = BigDecimal.ZERO;
    cdaiBalance = BigInteger.ZERO;
    daiInCompound = BigDecimal.ZERO;
    sumEstimatedProfits = BigDecimal.ZERO;
    sumEstimatedMissedProfits = BigDecimal.ZERO;
    pastTimeRatios = System.currentTimeMillis();

    minimumTradeProfit = makeDoubleMachineReadable(1.0);
    minimumTradeProfitBuyDai = makeDoubleMachineReadable(1.0);
    minimumTradeProfitSellDai = makeDoubleMachineReadable(1.0);

    currentTotalEthOwnershipRatio = 0L;
    currentTotalDaiOwnershipRatio = 0L;
    initialTotalUSD = BigDecimal.ZERO;
    initialTotalUSDCounter = 0;
    lastSuccessfulTransaction = System.currentTimeMillis();

    this.dai = dai;
    this.weth = weth;
    this.compoundDai = compoundDai;
    this.ethereum = ethereum;
  }

  public Balances(
      BigDecimal ethBalance,
      BigDecimal daiBalance,
      BigDecimal wethBalance,
      Dai dai,
      Weth weth,
      CompoundDai compoundDai,
      Ethereum ethereum) {
    this.ethBalance = ethBalance;
    this.daiBalance = daiBalance;
    this.wethBalance = wethBalance;

    this.dai = dai;
    this.weth = weth;
    this.compoundDai = compoundDai;
    this.ethereum = ethereum;

    minimumTradeProfit = makeDoubleMachineReadable(1.0);
    minimumTradeProfitBuyDai = makeDoubleMachineReadable(1.0);
    minimumTradeProfitSellDai = makeDoubleMachineReadable(1.0);
  }

  public void checkEnoughEthereumForGas() {
    // TODO: test this method (might unwrap without updating the gas fee to eth balance from
    // previous transaction)
    if (ethBalance.compareTo(MINIMUM_ETHEREUM_RESERVE_LOWER_LIMIT) < 0
        && wethBalance.compareTo(new BigDecimal("10000000000000000")) > 0) {

      BigInteger toUnwrap =
          (MINIMUM_ETHEREUM_RESERVE_UPPER_LIMIT.subtract(ethBalance).toBigInteger())
              .min(wethBalance.toBigInteger());

      logger.info("UNWRAP {}", makeBigNumberHumanReadableFullPrecision(toUnwrap));
      try {
        BigDecimal medianEthereumPrice = Medianizer.getPrice();
        weth.weth2Eth(this, BigDecimal.ZERO, medianEthereumPrice, toUnwrap);
      } catch (MedianException e) {
        logger.error("MedianIsZeroException", e);
      }
    }
  }

  public void updateBalanceInformation(BigDecimal medianEthereumPrice) {
    // Changes minimum trade profit depending on the time of the last successful transaction.
    /*
    if (System.currentTimeMillis() < lastSuccessfulTransaction + 5 * 60 * 1000) { // < 5 min
        minimumTradeProfit = makeDoubleMachineReadable(5.0);
    } else if (System.currentTimeMillis() < lastSuccessfulTransaction + 10 * 60 * 1000) { // < 10 min
        minimumTradeProfit = makeDoubleMachineReadable(2.5);
    } else if (System.currentTimeMillis() < lastSuccessfulTransaction + 30 * 60 * 1000) { // < 30 min
        minimumTradeProfit = makeDoubleMachineReadable(1.0);
    } else if (System.currentTimeMillis() < lastSuccessfulTransaction + 3 * 60 * 60 * 1000) { // < 3 h
        minimumTradeProfit = makeDoubleMachineReadable(0.5);
    } else { // > 3 h
        minimumTradeProfit = makeDoubleMachineReadable(0.01);
    }
    */
    // usingBufferedWriter("MINIMUM TRADE PROFIT          " +
    // makeBigNumberHumanReadable(minimumTradeProfit));

    logger.trace("DAI TO ETH RATIO {}{}", currentOwnershipRatio(medianEthereumPrice), " %");

    try {
      // TODO: think about putting this method into each class such as ethereum, dai and weth
      ethBalance = ethereum.getBalance();
      daiBalance = dai.getBalance(ethereum.getAddress());
      wethBalance = weth.getBalance(ethereum.getAddress());
      cdaiBalance = compoundDai.getCDaiBalance(ethereum.getAddress());
      daiInCompound = compoundDai.getBalanceInDai(cdaiBalance);
      usd =
          multiply(ethBalance, medianEthereumPrice)
              .add(multiply(wethBalance, medianEthereumPrice))
              .add(daiBalance)
              .add(daiInCompound); // US-Dollar

      // Gets executed just once at the beginning. Initializes initialTotalUSD.
      if (initialTotalUSDCounter == 0) {
        initialTotalUSD = usd;
        initialTotalUSDCounter++;
      }

      logger.trace("BALANCES");
      if (ethBalance.compareTo(BigDecimal.ZERO) != 0)
        logger.trace(
            "ETH BALANCE {}{}", makeBigNumberHumanReadableFullPrecision(ethBalance), " ETH");
      if (wethBalance.compareTo(BigDecimal.ZERO) != 0)
        logger.trace(
            "WETH BALANCE {}{}", makeBigNumberHumanReadableFullPrecision(wethBalance), " WETH");
      if (daiBalance.compareTo(BigDecimal.ZERO) != 0)
        logger.trace(
            "DAI BALANCE {}{}", makeBigNumberHumanReadableFullPrecision(daiBalance), " DAI");
      if (cdaiBalance.compareTo(BigInteger.ZERO) != 0)
        logger.trace(
            "CDAI BALANCE {}{}", makeBigNumberHumanReadableFullPrecision(cdaiBalance, 8), " CDAI");
      if (daiInCompound.compareTo(BigDecimal.ZERO) != 0)
        logger.trace(
            "DAI SUPPLIED BALANCE {}{}",
            makeBigNumberHumanReadableFullPrecision(daiInCompound),
            " DAI");
      if (sumEstimatedProfits.compareTo(BigDecimal.ZERO) != 0)
        logger.trace(
            "TOTAL ARBITRAGE P&L {}{}",
            makeBigNumberCurrencyHumanReadable(sumEstimatedProfits),
            " USD");
      if (sumEstimatedMissedProfits.compareTo(BigDecimal.ZERO) != 0)
        logger.trace(
            "TOTAL MISSED PROFITS {}{}",
            makeBigNumberCurrencyHumanReadable(sumEstimatedMissedProfits),
            " USD");
      if (usd.subtract(initialTotalUSD).compareTo(BigDecimal.ZERO) != 0)
        logger.trace(
            "TOTAL P&L DURING EXECUTION {}{}",
            makeBigNumberCurrencyHumanReadable(usd.subtract(initialTotalUSD)),
            " USD");
      // TODO: add information about interest earned
      if (usd.compareTo(BigDecimal.ZERO) != 0)
        logger.trace("TOTAL IN USD {}{}", makeBigNumberCurrencyHumanReadable(usd), " USD");

      minimumTradeProfitBuyDai = multiply(usd, makeDoubleMachineReadable(0.00025));
      logger.trace(
          "MINIMUM TRADE PROFIT BUY DAI {}{}",
          makeBigNumberCurrencyHumanReadable(minimumTradeProfitBuyDai),
          " DAI");

      minimumTradeProfitSellDai = multiply(usd, makeDoubleMachineReadable(0.0025));
      logger.trace(
          "MINIMUM TRADE PROFIT SELL DAI {}{}",
          makeBigNumberCurrencyHumanReadable(minimumTradeProfitSellDai),
          " DAI");

      // updateCDPInformation();

      // Checks if the bot made a big loss.
      // For some reason the DAI balance can be wrongly zero instead of the actual value. This means
      // that this If condition can be wrongly true.
      // If the DAI balance gets wrongly updated, the bot will do nothing for 60 secs until the next
      // update.
      if (usd.compareTo(multiply(initialTotalUSD, makeDoubleMachineReadable(0.5))) < 1) {
        logger.warn("USD BALANCE MIGHT BE ZERO EXCEPTION");

        updateBalanceInformation(Medianizer.getPrice());
        // everythingIsFine = false;
      }
    } catch (Exception e) {
      logger.error("Exception", e);
      updateBalanceInformation(medianEthereumPrice);
    }
  }

  /** @return 0 if no dai was owned during the execution of the program, 100 if no eth was owned */
  private long currentOwnershipRatio(BigDecimal medianEthereumPrice) {
    // TODO: add tests for this method
    currentTotalDaiOwnershipRatio +=
        (long)
            ((System.currentTimeMillis() - pastTimeRatios)
                * makeBigNumberHumanReadable(daiBalance)
                / (makeBigNumberHumanReadable(multiply(ethBalance, medianEthereumPrice))
                    + makeBigNumberHumanReadable(daiBalance)));
    if (currentTotalDaiOwnershipRatio == 0) return 0L;

    currentTotalEthOwnershipRatio +=
        (long)
            ((System.currentTimeMillis() - pastTimeRatios)
                * makeBigNumberHumanReadable(multiply(ethBalance, medianEthereumPrice))
                / (makeBigNumberHumanReadable(multiply(ethBalance, medianEthereumPrice))
                    + makeBigNumberHumanReadable(daiBalance)));
    long currentRatio =
        (currentTotalDaiOwnershipRatio * 10000)
            / ((currentTotalEthOwnershipRatio + currentTotalDaiOwnershipRatio) * 100);
    pastTimeRatios = System.currentTimeMillis();
    return currentRatio;
  }

  public void updateBalance(int duration) {
    long currentTime = System.currentTimeMillis();
    if (currentTime
        >= (pastTimeBalances
            + duration * 1000)) { // multiply by 1000 to getMedianEthereumPrice milliseconds
      try {
        BigDecimal medianEthereumPrice = Medianizer.getPrice();
        updateBalanceInformation(medianEthereumPrice);
      } catch (MedianException e) {
        logger.error("MedianIsZeroException{]", e);
      }

      pastTimeBalances = currentTime;
    }
  }

  public boolean checkTooFewDaiAndDaiInCompound() {
    return daiBalance.add(daiInCompound).compareTo(MINIMUM_DAI_NECESSARY_FOR_SALE) <= 0;
  }

  public BigDecimal getMaxDaiToSell() {
    BigDecimal maxDaiToSell = daiBalance.add(daiInCompound);
    logger.trace("MAX DAI TO SELL {}", makeBigNumberHumanReadableFullPrecision(maxDaiToSell));
    return maxDaiToSell;
  }

  public boolean checkEnoughDai() {
    return daiBalance.compareTo(MINIMUM_DAI_NECESSARY_FOR_SALE) > 0;
  }

  public boolean checkTooFewEthOrWeth() {
    return wethBalance
            .add(BigDecimal.ZERO.max(ethBalance.subtract(MINIMUM_ETHEREUM_RESERVE_UPPER_LIMIT)))
            .compareTo(MINIMUM_ETHEREUM_NECESSARY_FOR_SALE)
        <= 0;
  }

  public void addToSumEstimatedProfits(BigDecimal potentialProfit) {
    sumEstimatedProfits = sumEstimatedProfits.add(potentialProfit);
  }

  public void addToSumEstimatedMissedProfits(BigDecimal potentialProfit) {
    sumEstimatedMissedProfits = sumEstimatedMissedProfits.add(potentialProfit);
  }

  public BigDecimal getEthBalance() {
    return ethBalance;
  }

  public BigDecimal getDaiBalance() {
    return daiBalance;
  }

  public BigDecimal getWethBalance() {
    return wethBalance;
  }

  public BigInteger getCdaiBalance() {
    return cdaiBalance;
  }

  public BigDecimal getDaiInCompound() {
    return daiInCompound;
  }

  public BigDecimal getMinimumTradeProfitBuyDai() {
    return minimumTradeProfitBuyDai;
  }

  public BigDecimal getMinimumTradeProfitSellDai() {
    return minimumTradeProfitSellDai;
  }

  public long getLastSuccessfulTransaction() {
    return lastSuccessfulTransaction;
  }

  public void refreshLastSuccessfulTransaction() {
    lastSuccessfulTransaction = System.currentTimeMillis();
  }
}
