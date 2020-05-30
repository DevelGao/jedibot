package devgao.io.contractutil;

import devgao.io.contractneedsprovider.CircuitBreaker;
import devgao.io.contractneedsprovider.Wallet;
import devgao.io.contractneedsprovider.Web3jProvider;
import devgao.io.dai.DaiContract;
import devgao.io.gasprovider.GasProvider;
import devgao.io.numberutil.Wad18;
import devgao.io.util.JavaProperties;
import devgao.io.weth.WethContract;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;

import static org.junit.jupiter.api.Assertions.*;

public class ContractValidationUtilIT {
  private static final String TRAVIS_INFURA_PROJECT_ID = "TRAVIS_INFURA_PROJECT_ID";
  private static final String TRAVIS_WALLET = "TRAVIS_WALLET";
  private static final String TRAVIS_PASSWORD = "TRAVIS_PASSWORD";

  private static final String EXCEPTION = "Exception";

  private static final Wad18 minimumGasPrice = new Wad18(1_000000000);
  private static final Wad18 maximumGasPrice = new Wad18(200_000000000L);

  Web3j web3j;
  Credentials credentials;
  GasProvider gasProvider;

  @BeforeEach
  public void setUp() {
    String infuraProjectId;
    String password;
    String wallet;

    JavaProperties javaProperties = new JavaProperties(true);

    if ("true".equals(System.getenv().get("TRAVIS"))) {
      infuraProjectId = System.getenv().get(TRAVIS_INFURA_PROJECT_ID);
      wallet = System.getenv().get(TRAVIS_WALLET);
      password = System.getenv().get(TRAVIS_PASSWORD);
    } else {
      infuraProjectId = javaProperties.getValue("infuraProjectId");
      wallet = javaProperties.getValue("wallet");
      password = javaProperties.getValue("password");
    }

    web3j = new Web3jProvider(infuraProjectId).web3j;
    credentials = new Wallet(password, wallet).getCredentials();
    gasProvider = new GasProvider(web3j, minimumGasPrice, maximumGasPrice);
  }

  @Test
  public void isContractValid_isValid_continueRunning() {
    WethContract contract =
        WethContract.load(
            "0xc02aaa39b223fe8d0a0e5c4f27ead9083c756cc2", web3j, credentials, gasProvider);
    CircuitBreaker circuitBreaker = new CircuitBreaker();
    assertDoesNotThrow(() -> ContractValidationUtil.isContractValid(contract, circuitBreaker));
    assertTrue(circuitBreaker.getContinueRunning());
  }

  @Test
  public void isContractValid_isNotValid_stopRunning() {
    DaiContract contract =
        DaiContract.load(
            "0x0000000000000000000000000000000000000000", web3j, credentials, gasProvider);
    CircuitBreaker circuitBreaker = new CircuitBreaker();
    assertDoesNotThrow(() -> ContractValidationUtil.isContractValid(contract, circuitBreaker));
    assertFalse(circuitBreaker.getContinueRunning());
  }
}
