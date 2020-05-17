package devgao.io.weth;

import devgao.io.contractneedsprovider.CircuitBreaker;
import devgao.io.contractneedsprovider.ContractNeedsProvider;
import devgao.io.contractneedsprovider.Permissions;
import devgao.io.gasprovider.GasProvider;
import devgao.io.util.Balances;
import devgao.io.util.ContractUser;
import devgao.io.util.IContract;
import org.jetbrains.annotations.NotNull;
import org.slf4j.LoggerFactory;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.TransactionReceipt;

import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.concurrent.TimeUnit;

import static devgao.io.util.BigNumberUtil.BIGGEST_NUMBER;
import static devgao.io.util.BigNumberUtil.makeBigNumberHumanReadableFullPrecision;

public class Weth extends ContractUser {
  public static final String ADDRESS = "0xc02aaa39b223fe8d0a0e5c4f27ead9083c756cc2";
  private static final String EXCEPTION = "Exception";
  private static final org.slf4j.Logger logger =
      LoggerFactory.getLogger(MethodHandles.lookup().lookupClass().getSimpleName());
  private final WethContract contract;
  private final GasProvider gasProvider;
  private final Permissions permissions;
  private final Credentials credentials;
  private final CircuitBreaker circuitBreaker;

  public Weth(@NotNull ContractNeedsProvider contractNeedsProvider) {
    Web3j web3j = contractNeedsProvider.getWeb3j();
    gasProvider = contractNeedsProvider.getGasProvider();
    permissions = contractNeedsProvider.getPermissions();
    credentials = contractNeedsProvider.getCredentials();
    circuitBreaker = contractNeedsProvider.getCircuitBreaker();
    contract = WethContract.load(ADDRESS, web3j, credentials, gasProvider);
    isContractValid(contract, circuitBreaker);
  }

  public void weth2Eth(
      Balances balances,
      BigDecimal potentialProfit,
      BigDecimal medianEthereumPrice,
      BigInteger amountOfWethToUnwrap) {
    if (permissions.check("WETH2ETH")) {
      try {
        gasProvider.updateFastGasPrice(medianEthereumPrice, potentialProfit);

        if (amountOfWethToUnwrap.compareTo(balances.getWethBalance().toBigInteger()) > 0) {
          amountOfWethToUnwrap = balances.getWethBalance().toBigInteger();
          logger.warn(
              "WETH AMOUNT TO UNWRAP WAS TOO BIG {}",
              makeBigNumberHumanReadableFullPrecision(amountOfWethToUnwrap));
        }

        logger.warn(
            "ETH 2 WETH  {}", makeBigNumberHumanReadableFullPrecision(amountOfWethToUnwrap));
        TransactionReceipt transferReceipt = contract.withdraw(amountOfWethToUnwrap).send();
        TimeUnit.SECONDS.sleep(
            1); // for balances to update, otherwise same (buy/sell) type of transaction happens,
        // although not enough balance weth/dai
        logger.trace(
            "Transaction complete, view it at https://etherscan.io/tx/{}",
            transferReceipt.getTransactionHash());

        balances.updateBalanceInformation(medianEthereumPrice); // not really necessary?
      } catch (Exception e) {
        logger.error(EXCEPTION, e);
        circuitBreaker.add(System.currentTimeMillis());
      }
    }
  }

  public void eth2Weth(
      BigInteger amountOfEthToWrap,
      BigDecimal potentialProfit,
      BigDecimal medianEthereumPrice,
      Balances balances) {
    if (permissions.check("ETH2WETH")) {
      try {
        gasProvider.updateFastGasPrice(medianEthereumPrice, potentialProfit);
        if (amountOfEthToWrap.compareTo(balances.getEthBalance().toBigInteger()) > 0) {
          amountOfEthToWrap =
              balances
                  .getEthBalance()
                  .toBigInteger()
                  .subtract(balances.ethereum.minimumEthereumReserveUpperLimit.toBigInteger());
          logger.warn(
              "ETH AMOUNT TO WRAP WAS TOO BIG {}",
              makeBigNumberHumanReadableFullPrecision(amountOfEthToWrap));
        }

        logger.warn("ETH 2 WETH  {}", makeBigNumberHumanReadableFullPrecision(amountOfEthToWrap));
        TransactionReceipt transferReceipt = contract.deposit(amountOfEthToWrap).send();
        TimeUnit.SECONDS.sleep(1);
        logger.trace(
            "Transaction complete, view it at https://etherscan.io/tx/{}",
            transferReceipt.getTransactionHash());
        balances.updateBalanceInformation(medianEthereumPrice); // not really necessary?
      } catch (Exception e) {
        logger.error(EXCEPTION, e);
        circuitBreaker.add(System.currentTimeMillis());
      }
    }
  }

  private void approve(String address, String name) {
    if (permissions.check("WETH UNLOCK " + name)) {
      try {
        gasProvider.updateSlowGasPrice();
        contract.approve(address, BIGGEST_NUMBER).send();
        logger.debug("{} UNLOCK WETH", name);

      } catch (Exception e) {
        logger.error(EXCEPTION, e);
        circuitBreaker.add(System.currentTimeMillis());
      }
    }
  }

  public void checkApproval(IContract toAllowContract) {
    try {
      String address = toAllowContract.getAddress();
      BigInteger allowance = contract.allowance(credentials.getAddress(), address).send();
      if (allowance.compareTo(BIGGEST_NUMBER) < 0) {
        approve(address, toAllowContract.getClass().getName());
      }
    } catch (Exception e) {
      logger.error(EXCEPTION, e);
    }
  }

  public BigDecimal getBalance(String ethereumAddress) {
    try {
      return new BigDecimal(contract.balanceOf(ethereumAddress).send());
    } catch (Exception e) {
      logger.error(EXCEPTION, e);
    }
    return BigDecimal.ZERO;
  }
}
