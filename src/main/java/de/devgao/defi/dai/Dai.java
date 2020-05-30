package devgao.io.dai;

import devgao.io.contractneedsprovider.ContractNeedsProvider;
import devgao.io.contractutil.Account;
import devgao.io.contractutil.Approval;
import devgao.io.gasprovider.GasProvider;
import org.jetbrains.annotations.NotNull;
import org.slf4j.LoggerFactory;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.TransactionReceipt;

import java.lang.invoke.MethodHandles;
import java.math.BigInteger;

import static devgao.io.numberutil.NumberUtil.UINT_MAX;
import static devgao.io.numberutil.NumberUtil.getMachineReadable;

public class Dai {
  public static final String ADDRESS = "0x6b175474e89094c44da98b954eedeac495271d0f";
  private static final org.slf4j.Logger logger =
      LoggerFactory.getLogger(MethodHandles.lookup().lookupClass().getSimpleName());
  private static final String CDP_ADDRESS = "0x0000000000000000000000000000000000000000";
  private static final String EXCEPTION = "Exception";
  public final BigInteger minimumDaiNecessaryForSaleAndLending;
  private final DaiContract daiContract;
  private final Account account;
  private final Approval approval;

  public Dai(
      @NotNull ContractNeedsProvider contractNeedsProvider,
      double minimumDaiNecessaryForSaleAndLending) {
    Web3j web3j = contractNeedsProvider.getWeb3j();
    Credentials credentials = contractNeedsProvider.getCredentials();
    GasProvider gasProvider = contractNeedsProvider.getGasProvider();
    this.minimumDaiNecessaryForSaleAndLending =
        getMachineReadable(minimumDaiNecessaryForSaleAndLending);
    daiContract = DaiContract.load(ADDRESS, web3j, credentials, gasProvider);
    account = new Account(daiContract, credentials, "DAI");
    approval = new Approval(daiContract, contractNeedsProvider);
  }

  /** @deprecated DO NOT USE CDP ADDRESS THIS IS WRONG ADDRESS */
  @Deprecated(since = "0.0.1", forRemoval = false)
  private void cdpUnlockDai() {
    try {
      TransactionReceipt transferReceipt = daiContract.approve(CDP_ADDRESS, UINT_MAX).send();

      logger.debug(
          "Transaction complete, view it at https://etherscan.io/tx/{}",
          transferReceipt.getTransactionHash());

    } catch (Exception e) {
      logger.error(EXCEPTION, e);
    }
  }

  public Account getAccount() {
    return account;
  }

  public boolean isThereEnoughDaiForLending() {
    return account.getBalance().compareTo(minimumDaiNecessaryForSaleAndLending) > 0;
  }

  public Approval getApproval() {
    return approval;
  }
}
