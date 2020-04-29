package devgao.io.compoundeth;

import devgao.io.contractneedsprovider.ContractNeedsProvider;
import devgao.io.contractneedsprovider.Permissions;
import devgao.io.util.Balances;
import devgao.io.util.BigNumberUtil;
import org.jetbrains.annotations.NotNull;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.math.BigInteger;

import static devgao.io.util.BigNumberUtil.multiply;

public class CompoundEth {
  public static final String ADDRESS = "0x4Ddc2D193948926D02f9B1fE9e1daa0718270ED5";
  public static final BigInteger gasLimit =
      BigInteger.valueOf(100000); // https://compound.finance/developers#gas-costs
  private static final org.slf4j.Logger logger =
      LoggerFactory.getLogger(MethodHandles.lookup().lookupClass().getSimpleName());
  private static final BigInteger secondsPerYear = BigInteger.valueOf(31557600);
  private static final BigInteger timeBetweenBlocks = BigInteger.valueOf(15);
  private static final BigInteger supplyRatePerYearMultiplicand =
      secondsPerYear.divide(timeBetweenBlocks);
  private static CompoundEthContract contract;
  private final Permissions permissions;

  public CompoundEth(@NotNull ContractNeedsProvider contractNeedsProvider) {
    this.permissions = contractNeedsProvider.getPermissions();
  }

  static BigInteger getExchangeRate() {
    try {
      return contract.exchangeRateStored().send();
    } catch (Exception e) {
      logger.error("Exception", e);
    }
    return BigInteger.ZERO;
  }

  void borrow(BigInteger borrowAmount) {
    if (permissions.check(
        "COMPOUND ETH BORROW "
            + BigNumberUtil.makeBigNumberHumanReadableFullPrecision(borrowAmount))) {
      try {
        contract.borrow(borrowAmount).send();
        logger.warn(
            "BORROW ETH {}", BigNumberUtil.makeBigNumberHumanReadableFullPrecision(borrowAmount));
      } catch (Exception e) {
        logger.error("Exception", e);
      }
    }
  }

  void repayBorrow(BigInteger repayAmount) {
    if (permissions.check(
        "COMPOUND ETH REPAY "
            + BigNumberUtil.makeBigNumberHumanReadableFullPrecision(repayAmount))) {
      try {
        contract.repayBorrow(repayAmount).send();
        logger.warn(
            "REPAY ETH {}", BigNumberUtil.makeBigNumberHumanReadableFullPrecision(repayAmount));
      } catch (Exception e) {
        logger.error("Exception", e);
      }
    }
  }

  BigInteger getCTokenBalance(String ethereumAddress) {
    try {
      return contract.balanceOf(ethereumAddress).send();
    } catch (Exception e) {
      logger.error("Exception", e);
    }
    return BigInteger.ZERO;
  }

  BigInteger getBalance(String ethereumAddress) {
    return multiply(getExchangeRate(), getCTokenBalance(ethereumAddress));
  }

  // borrow dai against weth and sell it, if dai price is high
  void checkBorrowDaiOpportunity(@NotNull Balances balances) {
    if (balances.checkTooFewEthOrWeth()) {
      logger.warn("NOT ENOUGH ETH OR WETH TO BORROW DAI ON COMPOUND");
    }
  }
}
