package devgao.io.contractutil;

import devgao.io.contractneedsprovider.ContractNeedsProvider;
import devgao.io.contractuserutil.AddressMethod;
import devgao.io.numberutil.Wad18;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;

import static devgao.io.numberutil.NumberUtil.MINIMUM_APPROVAL_ALLOWANCE;
import static devgao.io.numberutil.NumberUtil.UINT_MAX;

public class Approval {
  private static final org.slf4j.Logger logger =
      LoggerFactory.getLogger(MethodHandles.lookup().lookupClass().getSimpleName());
  private static final String EXCEPTION = "Exception";
  private final ApprovalMethod contract;
  private final ContractNeedsProvider contractNeedsProvider;

  public Approval(ApprovalMethod contract, ContractNeedsProvider contractNeedsProvider) {
    this.contract = contract;
    this.contractNeedsProvider = contractNeedsProvider;
  }

  private void approve(String address, String name) {
    if (contractNeedsProvider.getPermissions().check("DAI UNLOCK " + name)) {
      try {
        contractNeedsProvider.getGasProvider().updateSlowGasPrice();
        contract.approve(address, UINT_MAX).send();
        logger.debug("{} UNLOCK DAI", name);
      } catch (Exception e) {
        logger.error(EXCEPTION, e);
        contractNeedsProvider.getCircuitBreaker().addTransactionFailedNow();
      }
    }
  }

  public void check(AddressMethod toAllowContract) {
    try {
      String address = toAllowContract.getAddress();
      Wad18 allowance = new Wad18(
              contract.allowance(contractNeedsProvider.getCredentials().getAddress(), address).send());
      if (allowance.compareTo(MINIMUM_APPROVAL_ALLOWANCE) < 0) {
        logger.warn("DAI ALLOWANCE IS TOO LOW {}", allowance);
        approve(address, toAllowContract.getClass().getName());
      }
    } catch (Exception e) {
      logger.error(EXCEPTION, e);
    }
  }
}
